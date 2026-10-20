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
import dev.esgenius.ratelimit.UsageLimitExceededException;
import dev.esgenius.ratelimit.UsageLimiter;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.config.ComplianceClassificationExecutorConfig;
import dev.esgenius.config.GeminiProperties;
import dev.esgenius.service.compliance.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

@Service
public class ComplianceAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceAnalysisService.class);

    /** Official framework order; requirements without a sort order (legacy) follow, by code. */
    private static final Comparator<RequirementAssessment> FRAMEWORK_ORDER = Comparator
            .<RequirementAssessment, Integer>comparing(
                    a -> a.getFrameworkRequirement().getSortOrder() == null
                            ? Integer.MAX_VALUE
                            : a.getFrameworkRequirement().getSortOrder())
            .thenComparing(a -> a.getFrameworkRequirement().getRequirementCode());
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
    private final ThreadPoolTaskExecutor classificationExecutor;
    private final ObjectMapper objectMapper;
    private final ComplianceAnalysisCreationService creationService;
    private final ComplianceAnalysisPersistenceService persistenceService;
    private final ComplianceAnalysisProcessor complianceAnalysisProcessor;
    private final DocumentAccessPolicy documentAccessPolicy;
    private final UsageLimiter usageLimiter;

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
            @Qualifier(ComplianceClassificationExecutorConfig.EXECUTOR_BEAN_NAME)
            ThreadPoolTaskExecutor classificationExecutor,
            ObjectMapper objectMapper,
            ComplianceAnalysisCreationService creationService,
            ComplianceAnalysisPersistenceService persistenceService,
            @Lazy ComplianceAnalysisProcessor complianceAnalysisProcessor,
            DocumentAccessPolicy documentAccessPolicy,
            UsageLimiter usageLimiter) {
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
        this.classificationExecutor = classificationExecutor;
        this.objectMapper = objectMapper;
        this.creationService = creationService;
        this.persistenceService = persistenceService;
        this.complianceAnalysisProcessor = complianceAnalysisProcessor;
        this.documentAccessPolicy = documentAccessPolicy;
        this.usageLimiter = usageLimiter;
    }

    public ComplianceAnalysisResponse startAnalysis(Long documentId, StartAnalysisRequest request) {
        return startAnalysis(documentId, request, documentAccessPolicy.callerWhenIdentityAbsent());
    }

    public ComplianceAnalysisResponse startAnalysis(Long documentId, StartAnalysisRequest request, Caller caller) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        documentAccessPolicy.requireModify(document, caller, "Document not found: " + documentId);
        Long analysisId = creationService.createInProgressAnalysis(documentId, request);
        try {
            usageLimiter.consumeAnalysis(caller);
        } catch (UsageLimitExceededException ex) {
            // Creation commits in its own transaction. Drop that row so a rejected
            // start does not stay IN_PROGRESS and block the next attempt.
            analysisRepository.deleteById(analysisId);
            throw ex;
        }
        ComplianceAnalysisResponse response = getAnalysis(analysisId, caller);
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

        List<FrameworkRequirement> requirements = requirementRepository.findActiveByFramework(framework);
        List<TextChunk> chunks = buildChunksForDocument(document);
        AnalysisRunContext runContext = new AnalysisRunContext();
        List<Future<?>> classifications = new ArrayList<>();

        // Retrieval is local and fast; only requirements with evidence go to the model.
        List<PendingClassification> pending = new ArrayList<>();
        for (FrameworkRequirement requirement : requirements) {
            List<RetrievedChunk> retrieved = retrievalService.retrieve(requirement, chunks);
            if (retrieved.isEmpty()) {
                persistenceService.persistRequirementAssessment(
                        analysisId,
                        requirement.getId(),
                        0.0,
                        null,
                        null,
                        noEvidenceClassifier.classify(requirement));
                continue;
            }
            pending.add(new PendingClassification(requirement, retrieved));
        }

        // One model call per batch: requirements are grouped by framework section (so a call covers one
        // principle) and large sections are split evenly, which keeps a full analysis near a dozen calls.
        for (List<PendingClassification> batch : planBatches(pending, geminiProperties.getMaxRequirementsPerCall())) {
            try {
                classifications.add(classificationExecutor.submit(
                        () -> classifyAndPersistBatch(analysisId, batch, chunks, runContext)));
            } catch (RejectedExecutionException ex) {
                log.error(
                        "Classification batch was not accepted analysisId={} firstRequirement={} size={}",
                        analysisId,
                        batch.get(0).requirement().getRequirementCode(),
                        batch.size(),
                        ex);
                batch.forEach(item -> persistUnexpectedFailure(analysisId, item.requirement(), item.retrieved()));
            }
        }

        awaitClassifications(analysisId, classifications);
        persistenceService.finalizeCompleted(analysisId);
    }

    /**
     * Groups pending requirements by section (keeping framework order) and splits each group into the
     * fewest batches of at most {@code maxPerCall}, sized as evenly as possible.
     */
    static List<List<PendingClassification>> planBatches(List<PendingClassification> pending, int maxPerCall) {
        Map<String, List<PendingClassification>> bySection = new LinkedHashMap<>();
        for (PendingClassification item : pending) {
            String section = item.requirement().getSection() == null ? "" : item.requirement().getSection();
            bySection.computeIfAbsent(section, ignored -> new ArrayList<>()).add(item);
        }

        List<List<PendingClassification>> batches = new ArrayList<>();
        for (List<PendingClassification> group : bySection.values()) {
            int batchCount = (group.size() + maxPerCall - 1) / maxPerCall;
            int baseSize = group.size() / batchCount;
            int extra = group.size() % batchCount;
            int start = 0;
            for (int i = 0; i < batchCount; i++) {
                int size = baseSize + (i < extra ? 1 : 0);
                batches.add(List.copyOf(group.subList(start, start + size)));
                start += size;
            }
        }
        return batches;
    }

    record PendingClassification(FrameworkRequirement requirement, List<RetrievedChunk> retrieved) {
    }

    private void awaitClassifications(Long analysisId, List<Future<?>> classifications) {
        for (Future<?> classification : classifications) {
            try {
                classification.get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Classification interrupted", ex);
            } catch (ExecutionException ex) {
                log.error("Classification task failed analysisId={}", analysisId, ex.getCause());
            }
        }
    }

    private void classifyAndPersistBatch(
            Long analysisId,
            List<PendingClassification> batch,
            List<TextChunk> sourceChunks,
            AnalysisRunContext runContext) {
        Map<String, ComplianceClassificationResult> results = classifyBatchWithEvidence(batch, sourceChunks, runContext);
        for (PendingClassification item : batch) {
            FrameworkRequirement requirement = item.requirement();
            List<RetrievedChunk> retrieved = item.retrieved();
            try {
                ComplianceClassificationResult classification = results != null
                        ? results.get(requirement.getRequirementCode())
                        : null;
                if (classification == null) {
                    // Whole call failed (quota aware), or the model left this requirement out of its answer.
                    classification = results == null
                            ? failureHandler.handleFailure(runContext)
                            : failureHandler.unexpectedFailure();
                }
                persistenceService.persistRequirementAssessment(
                        analysisId,
                        requirement.getId(),
                        retrieved.get(0).score(),
                        buildEvidenceText(retrieved),
                        serializeEvidenceChunks(retrieved),
                        classification);
            } catch (RuntimeException ex) {
                log.error(
                        "Unexpected classification failure analysisId={} requirementCode={}",
                        analysisId,
                        requirement.getRequirementCode(),
                        ex);
                persistUnexpectedFailure(analysisId, requirement, retrieved);
            }
        }
        paceBeforeNextClassification();
    }

    /** Returns the batch results by requirement code, or null when the whole call failed. */
    private Map<String, ComplianceClassificationResult> classifyBatchWithEvidence(
            List<PendingClassification> batch,
            List<TextChunk> sourceChunks,
            AnalysisRunContext runContext) {
        if (runContext.isQuotaExhausted()) {
            return null;
        }
        try {
            List<ComplianceClassificationRequest> requests = new ArrayList<>();
            for (PendingClassification item : batch) {
                FrameworkRequirement requirement = item.requirement();
                requests.add(new ComplianceClassificationRequest(
                        requirement.getRequirementCode(),
                        requirement.getTitle(),
                        requirement.getDescription(),
                        requirement.getFrameworkText(),
                        classificationEvidenceBuilder.buildExpandedPassages(item.retrieved(), sourceChunks),
                        runContext));
            }
            return classificationProvider.classifyBatch(requests);
        } catch (ComplianceClassificationException ex) {
            if (ex.getCategory() == ClassificationFailureCategory.QUOTA_EXHAUSTED) {
                runContext.markQuotaExhausted();
            }
            log.warn(
                    "Classification fallback for batch firstRequirement={} size={} category={} httpStatus={} attempt={} detail={}",
                    batch.get(0).requirement().getRequirementCode(),
                    batch.size(),
                    ex.getCategory(),
                    ex.getHttpStatus(),
                    ex.getAttempt(),
                    ex.getSafeDetail());
            return null;
        } catch (RuntimeException ex) {
            // Never let one bad call lose a whole batch: its requirements fall back to human review.
            log.error(
                    "Unexpected classification failure firstRequirement={} size={}",
                    batch.get(0).requirement().getRequirementCode(),
                    batch.size(),
                    ex);
            return null;
        }
    }

    private void persistUnexpectedFailure(
            Long analysisId, FrameworkRequirement requirement, List<RetrievedChunk> retrieved) {
        try {
            persistenceService.persistRequirementAssessment(
                    analysisId,
                    requirement.getId(),
                    retrieved.isEmpty() ? 0.0 : retrieved.get(0).score(),
                    retrieved.isEmpty() ? null : buildEvidenceText(retrieved),
                    retrieved.isEmpty() ? null : serializeEvidenceChunks(retrieved),
                    failureHandler.unexpectedFailure());
        } catch (RuntimeException persistEx) {
            log.error(
                    "Failed to persist human-review fallback analysisId={} requirementCode={}",
                    analysisId,
                    requirement.getRequirementCode(),
                    persistEx);
        }
    }

    @Transactional(readOnly = true)
    public ComplianceAnalysisResponse getAnalysis(Long analysisId) {
        return getAnalysis(analysisId, documentAccessPolicy.callerWhenIdentityAbsent());
    }

    @Transactional(readOnly = true)
    public ComplianceAnalysisResponse getAnalysis(Long analysisId, Caller caller) {
        ComplianceAnalysis analysis = analysisRepository.findByIdWithFrameworkAndDocument(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found: " + analysisId));
        documentAccessPolicy.requireView(
                analysis.getDocument(), caller, "Analysis not found: " + analysisId);
        return toResponse(analysis);
    }

    @Transactional(readOnly = true)
    public List<ComplianceAnalysisSummaryResponse> listAnalysesForDocument(Long documentId) {
        return listAnalysesForDocument(documentId, documentAccessPolicy.callerWhenIdentityAbsent());
    }

    @Transactional(readOnly = true)
    public List<ComplianceAnalysisSummaryResponse> listAnalysesForDocument(Long documentId, Caller caller) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        documentAccessPolicy.requireView(document, caller, "Document not found: " + documentId);

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
                .sorted(FRAMEWORK_ORDER)
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
                deserializeEvidenceChunks(assessment.getEvidenceChunks()),
                requirement.getSection(),
                requirement.getIndicatorType());
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
