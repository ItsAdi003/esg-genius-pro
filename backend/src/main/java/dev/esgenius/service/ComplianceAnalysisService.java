package dev.esgenius.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.esgenius.dto.*;
import dev.esgenius.entity.*;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.entity.DocumentPage;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentPageRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.FrameworkRequirementRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.config.GeminiProperties;
import dev.esgenius.service.compliance.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ComplianceAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceAnalysisService.class);
    static final String GENERIC_FAILURE_REASON = ComplianceAnalysisPersistenceService.GENERIC_FAILURE_REASON;

    private final ComplianceAnalysisRepository analysisRepository;
    private final RequirementAssessmentRepository assessmentRepository;
    private final DocumentRepository documentRepository;
    private final DocumentPageRepository documentPageRepository;
    private final FrameworkRepository frameworkRepository;
    private final FrameworkRequirementRepository requirementRepository;
    private final TextChunkingService chunkingService;
    private final LexicalEvidenceRetrievalService retrievalService;
    private final ComplianceClassificationProvider classificationProvider;
    private final DeterministicNoEvidenceClassifier noEvidenceClassifier;
    private final ClassificationFailureHandler failureHandler;
    private final ClassificationEvidenceBuilder classificationEvidenceBuilder;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final ComplianceAnalysisCreationService creationService;
    private final ComplianceAnalysisPersistenceService persistenceService;
    private final ComplianceAnalysisProcessor complianceAnalysisProcessor;

    public ComplianceAnalysisService(
            ComplianceAnalysisRepository analysisRepository,
            RequirementAssessmentRepository assessmentRepository,
            DocumentRepository documentRepository,
            DocumentPageRepository documentPageRepository,
            FrameworkRepository frameworkRepository,
            FrameworkRequirementRepository requirementRepository,
            TextChunkingService chunkingService,
            LexicalEvidenceRetrievalService retrievalService,
            ComplianceClassificationProvider classificationProvider,
            DeterministicNoEvidenceClassifier noEvidenceClassifier,
            ClassificationFailureHandler failureHandler,
            ClassificationEvidenceBuilder classificationEvidenceBuilder,
            GeminiProperties geminiProperties,
            ObjectMapper objectMapper,
            ComplianceAnalysisCreationService creationService,
            ComplianceAnalysisPersistenceService persistenceService,
            @Lazy ComplianceAnalysisProcessor complianceAnalysisProcessor) {
        this.analysisRepository = analysisRepository;
        this.assessmentRepository = assessmentRepository;
        this.documentRepository = documentRepository;
        this.documentPageRepository = documentPageRepository;
        this.frameworkRepository = frameworkRepository;
        this.requirementRepository = requirementRepository;
        this.chunkingService = chunkingService;
        this.retrievalService = retrievalService;
        this.classificationProvider = classificationProvider;
        this.noEvidenceClassifier = noEvidenceClassifier;
        this.failureHandler = failureHandler;
        this.classificationEvidenceBuilder = classificationEvidenceBuilder;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
        this.creationService = creationService;
        this.persistenceService = persistenceService;
        this.complianceAnalysisProcessor = complianceAnalysisProcessor;
    }

    public ComplianceAnalysisResponse startAnalysis(Long documentId, StartAnalysisRequest request) {
        Long analysisId = creationService.createInProgressAnalysis(documentId, request);
        ComplianceAnalysisResponse response = getAnalysis(analysisId);
        complianceAnalysisProcessor.processAnalysis(analysisId);
        return response;
    }

    public void executeAnalysis(Long analysisId) {
        ComplianceAnalysis analysis = analysisRepository.findByIdWithFrameworkAndDocument(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found: " + analysisId));

        if (analysis.getStatus() != AnalysisStatus.IN_PROGRESS) {
            log.warn("Skipping analysis {} because status is {}", analysisId, analysis.getStatus());
            return;
        }

        Long documentId = analysis.getDocument().getId();
        Long frameworkId = analysis.getFramework().getId();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        Framework framework = frameworkRepository.findById(frameworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Framework not found: " + frameworkId));

        List<FrameworkRequirement> requirements = requirementRepository.findByFramework(framework);
        List<TextChunk> chunks = buildChunksForDocument(document);

        for (FrameworkRequirement requirement : requirements) {
            List<RetrievedChunk> retrieved = retrievalService.retrieve(requirement, chunks);
            boolean hadEvidence = !retrieved.isEmpty();

            if (hadEvidence) {
                double topScore = retrieved.get(0).score();
                ComplianceClassificationResult classification =
                        classifyWithEvidence(requirement, retrieved, chunks);
                persistenceService.persistRequirementAssessment(
                        analysisId,
                        requirement.getId(),
                        topScore,
                        buildEvidenceText(retrieved),
                        serializeEvidenceChunks(retrieved),
                        classification);
                paceBeforeNextClassification();
            } else {
                persistenceService.persistRequirementAssessment(
                        analysisId,
                        requirement.getId(),
                        0.0,
                        null,
                        null,
                        noEvidenceClassifier.classify(requirement));
            }
        }

        persistenceService.finalizeCompleted(analysisId);
    }

    @Transactional(readOnly = true)
    public ComplianceAnalysisResponse getAnalysis(Long analysisId) {
        ComplianceAnalysis analysis = analysisRepository.findByIdWithFrameworkAndDocument(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found: " + analysisId));
        return toResponse(analysis);
    }

    @Transactional(readOnly = true)
    public List<ComplianceAnalysisSummaryResponse> listAnalysesForDocument(Long documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Document not found: " + documentId);
        }

        List<ComplianceAnalysis> analyses =
                analysisRepository.findByDocumentIdWithFrameworkOrderByStartedAtDesc(documentId);
        if (analyses.isEmpty()) {
            return List.of();
        }

        List<Long> analysisIds = analyses.stream().map(ComplianceAnalysis::getId).toList();
        Map<Long, AssessmentStatusCounts> statusCountsByAnalysis = loadAssessmentStatusCounts(analysisIds);

        return analyses.stream()
                .map(analysis -> toSummaryResponse(
                        documentId,
                        analysis,
                        statusCountsByAnalysis.getOrDefault(analysis.getId(), AssessmentStatusCounts.empty())))
                .toList();
    }

    private ComplianceClassificationResult classifyWithEvidence(
            FrameworkRequirement requirement, List<RetrievedChunk> retrieved, List<TextChunk> sourceChunks) {
        try {
            List<String> expandedPassages = classificationEvidenceBuilder.buildExpandedPassages(retrieved, sourceChunks);
            ComplianceClassificationRequest request = new ComplianceClassificationRequest(
                    requirement.getRequirementCode(),
                    requirement.getTitle(),
                    requirement.getDescription(),
                    requirement.getFrameworkText(),
                    expandedPassages);
            return classificationProvider.classify(request);
        } catch (ComplianceClassificationException ex) {
            log.warn(
                    "Classification fallback for requirementCode={} category={} httpStatus={} attempt={} detail={}",
                    requirement.getRequirementCode(),
                    ex.getCategory(),
                    ex.getHttpStatus(),
                    ex.getAttempt(),
                    ex.getSafeDetail());
            return failureHandler.handleFailure();
        }
    }

    private void paceBeforeNextClassification() {
        long delayMs = geminiProperties.getInterRequestDelay().toMillis();
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Classification pacing interrupted", interrupted);
        }
    }

    private List<TextChunk> buildChunksForDocument(Document document) {
        List<DocumentPage> pages = documentPageRepository.findByDocumentOrderByPageNumberAsc(document);
        if (!pages.isEmpty()) {
            List<ExtractedPdfPage> pageSources = pages.stream()
                    .map(page -> new ExtractedPdfPage(page.getPageNumber(), page.getExtractedText()))
                    .toList();
            return chunkingService.chunkPages(pageSources);
        }
        return chunkingService.chunk(document.getExtractedText());
    }

    private String buildEvidenceText(List<RetrievedChunk> chunks) {
        return chunks.stream()
                .map(RetrievedChunk::text)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String serializeEvidenceChunks(List<RetrievedChunk> chunks) {
        List<EvidenceChunkResponse> payload = chunks.stream()
                .map(chunk -> new EvidenceChunkResponse(
                        chunk.chunkIndex(), chunk.pageNumber(), chunk.text(), chunk.score()))
                .toList();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Failed to serialize evidence chunks");
        }
    }

    private Map<Long, AssessmentStatusCounts> loadAssessmentStatusCounts(List<Long> analysisIds) {
        Map<Long, AssessmentStatusCounts> countsByAnalysis = new HashMap<>();
        for (Object[] row : assessmentRepository.countByStatusForAnalysisIds(analysisIds)) {
            Long analysisId = (Long) row[0];
            AssessmentStatus status = (AssessmentStatus) row[1];
            int count = ((Number) row[2]).intValue();
            countsByAnalysis
                    .computeIfAbsent(analysisId, ignored -> new AssessmentStatusCounts())
                    .add(status, count);
        }
        return countsByAnalysis;
    }

    private ComplianceAnalysisSummaryResponse toSummaryResponse(
            Long documentId, ComplianceAnalysis analysis, AssessmentStatusCounts statusCounts) {
        Framework framework = analysis.getFramework();
        return new ComplianceAnalysisSummaryResponse(
                analysis.getId(),
                documentId,
                framework.getId(),
                framework.getCode(),
                framework.getName(),
                analysis.getStatus().name(),
                analysis.getStartedAt(),
                analysis.getCompletedAt(),
                analysis.getFailureReason(),
                statusCounts.total(),
                statusCounts.covered,
                statusCounts.partiallyCovered,
                statusCounts.notCovered,
                statusCounts.humanReviewRequired,
                statusCounts.evidenceRetrieved,
                statusCounts.noEvidenceFound);
    }

    private ComplianceAnalysisResponse toResponse(ComplianceAnalysis analysis) {
        Framework framework = analysis.getFramework();
        List<RequirementAssessment> assessments = assessmentRepository
                .findByAnalysisOrderByFrameworkRequirement_RequirementCodeAsc(analysis);

        List<RequirementAssessmentResponse> assessmentResponses = assessments.stream()
                .sorted(Comparator.comparing(a -> a.getFrameworkRequirement().getRequirementCode()))
                .map(this::toAssessmentResponse)
                .toList();

        return new ComplianceAnalysisResponse(
                analysis.getId(),
                analysis.getDocument().getId(),
                framework.getId(),
                framework.getCode(),
                framework.getName(),
                analysis.getStatus().name(),
                analysis.getStartedAt(),
                analysis.getCompletedAt(),
                analysis.getFailureReason(),
                assessmentResponses.size(),
                assessmentResponses);
    }

    private RequirementAssessmentResponse toAssessmentResponse(RequirementAssessment assessment) {
        FrameworkRequirement requirement = assessment.getFrameworkRequirement();
        return new RequirementAssessmentResponse(
                requirement.getId(),
                requirement.getRequirementCode(),
                requirement.getTitle(),
                requirement.getCategory().name(),
                assessment.getAssessmentStatus().name(),
                assessment.getConfidence(),
                assessment.getExplanation(),
                assessment.getGap(),
                assessment.getRecommendation(),
                assessment.getRetrievalScore(),
                assessment.getEvidenceText(),
                deserializeEvidenceChunks(assessment.getEvidenceChunks()));
    }

    private List<EvidenceChunkResponse> deserializeEvidenceChunks(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<EvidenceChunkResponse>>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private static final class AssessmentStatusCounts {
        int covered;
        int partiallyCovered;
        int notCovered;
        int humanReviewRequired;
        int evidenceRetrieved;
        int noEvidenceFound;

        static AssessmentStatusCounts empty() {
            return new AssessmentStatusCounts();
        }

        void add(AssessmentStatus status, int count) {
            switch (status) {
                case COVERED -> covered += count;
                case PARTIALLY_COVERED -> partiallyCovered += count;
                case NOT_COVERED -> notCovered += count;
                case HUMAN_REVIEW_REQUIRED -> humanReviewRequired += count;
                case EVIDENCE_RETRIEVED -> evidenceRetrieved += count;
                case NO_EVIDENCE_FOUND -> noEvidenceFound += count;
            }
        }

        int total() {
            return covered
                    + partiallyCovered
                    + notCovered
                    + humanReviewRequired
                    + evidenceRetrieved
                    + noEvidenceFound;
        }
    }
}
