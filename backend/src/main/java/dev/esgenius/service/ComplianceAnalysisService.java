package dev.esgenius.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.esgenius.dto.*;
import dev.esgenius.entity.*;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.FrameworkRequirementRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplianceAnalysisService {

    private final ComplianceAnalysisRepository analysisRepository;
    private final RequirementAssessmentRepository assessmentRepository;
    private final DocumentRepository documentRepository;
    private final FrameworkRepository frameworkRepository;
    private final FrameworkRequirementRepository requirementRepository;
    private final TextChunkingService chunkingService;
    private final LexicalEvidenceRetrievalService retrievalService;
    private final ObjectMapper objectMapper;

    public ComplianceAnalysisService(
            ComplianceAnalysisRepository analysisRepository,
            RequirementAssessmentRepository assessmentRepository,
            DocumentRepository documentRepository,
            FrameworkRepository frameworkRepository,
            FrameworkRequirementRepository requirementRepository,
            TextChunkingService chunkingService,
            LexicalEvidenceRetrievalService retrievalService,
            ObjectMapper objectMapper) {
        this.analysisRepository = analysisRepository;
        this.assessmentRepository = assessmentRepository;
        this.documentRepository = documentRepository;
        this.frameworkRepository = frameworkRepository;
        this.requirementRepository = requirementRepository;
        this.chunkingService = chunkingService;
        this.retrievalService = retrievalService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ComplianceAnalysisResponse startAnalysis(Long documentId, StartAnalysisRequest request) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        if (document.getStatus() != DocumentStatus.READY) {
            throw new BadRequestException(
                    "Document must be in READY status before analysis. Current status: " + document.getStatus());
        }

        if (document.getExtractedText() == null || document.getExtractedText().isBlank()) {
            throw new BadRequestException("Document has no extracted text available for analysis");
        }

        Framework framework = resolveFramework(request);
        List<FrameworkRequirement> requirements = requirementRepository.findByFramework(framework);
        if (requirements.isEmpty()) {
            throw new BadRequestException("Framework has no requirements: " + framework.getCode());
        }

        ComplianceAnalysis analysis = new ComplianceAnalysis(document, framework);
        analysisRepository.save(analysis);

        List<TextChunk> chunks = chunkingService.chunk(document.getExtractedText());

        try {
            for (FrameworkRequirement requirement : requirements) {
                RequirementAssessment assessment = new RequirementAssessment(analysis, requirement);
                List<RetrievedChunk> retrieved = retrievalService.retrieve(requirement, chunks);

                if (retrieved.isEmpty()) {
                    assessment.setAssessmentStatus(AssessmentStatus.NO_EVIDENCE_FOUND);
                    assessment.setRetrievalScore(0.0);
                } else {
                    assessment.setAssessmentStatus(AssessmentStatus.EVIDENCE_RETRIEVED);
                    double topScore = retrieved.get(0).score();
                    assessment.setRetrievalScore(topScore);
                    assessment.setEvidenceText(buildEvidenceText(retrieved));
                    assessment.setEvidenceChunks(serializeEvidenceChunks(retrieved));
                }

                analysis.addAssessment(assessment);
            }

            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysis.setCompletedAt(Instant.now());
            analysisRepository.save(analysis);

            return toResponse(analysis);
        } catch (RuntimeException ex) {
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setFailureReason(ex.getMessage());
            analysis.setCompletedAt(Instant.now());
            analysisRepository.save(analysis);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ComplianceAnalysisResponse getAnalysis(Long analysisId) {
        ComplianceAnalysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found: " + analysisId));
        return toResponse(analysis);
    }

    private Framework resolveFramework(StartAnalysisRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required with frameworkId or frameworkCode");
        }

        if (request.frameworkId() != null) {
            return frameworkRepository.findById(request.frameworkId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Framework not found: " + request.frameworkId()));
        }

        if (request.frameworkCode() != null && !request.frameworkCode().isBlank()) {
            return frameworkRepository.findByCode(request.frameworkCode().trim().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Framework not found: " + request.frameworkCode()));
        }

        throw new BadRequestException("frameworkId or frameworkCode is required");
    }

    private String buildEvidenceText(List<RetrievedChunk> chunks) {
        return chunks.stream()
                .map(RetrievedChunk::text)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String serializeEvidenceChunks(List<RetrievedChunk> chunks) {
        List<EvidenceChunkResponse> payload = chunks.stream()
                .map(chunk -> new EvidenceChunkResponse(chunk.chunkIndex(), chunk.text(), chunk.score()))
                .toList();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Failed to serialize evidence chunks");
        }
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
}
