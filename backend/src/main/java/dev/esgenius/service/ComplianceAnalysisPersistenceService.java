package dev.esgenius.service;

import dev.esgenius.entity.AnalysisStatus;
import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.entity.ComplianceAnalysis;
import dev.esgenius.entity.FrameworkRequirement;
import dev.esgenius.entity.RequirementAssessment;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.FrameworkRequirementRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.service.compliance.ComplianceClassificationException;
import dev.esgenius.service.compliance.ComplianceClassificationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ComplianceAnalysisPersistenceService {

    static final String GENERIC_FAILURE_REASON = "Compliance analysis processing failed unexpectedly.";
    static final String DISPATCH_FAILURE_REASON =
            "Analysis could not be queued for background processing.";

    private final ComplianceAnalysisRepository analysisRepository;
    private final RequirementAssessmentRepository assessmentRepository;
    private final FrameworkRequirementRepository requirementRepository;

    public ComplianceAnalysisPersistenceService(
            ComplianceAnalysisRepository analysisRepository,
            RequirementAssessmentRepository assessmentRepository,
            FrameworkRequirementRepository requirementRepository) {
        this.analysisRepository = analysisRepository;
        this.assessmentRepository = assessmentRepository;
        this.requirementRepository = requirementRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistRequirementAssessment(
            Long analysisId,
            Long requirementId,
            double retrievalScore,
            String evidenceText,
            String evidenceChunksJson,
            ComplianceClassificationResult result) {
        ComplianceAnalysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found: " + analysisId));
        FrameworkRequirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Framework requirement not found: " + requirementId));

        RequirementAssessment assessment = new RequirementAssessment(analysis, requirement);
        assessment.setRetrievalScore(retrievalScore);
        assessment.setEvidenceText(evidenceText);
        assessment.setEvidenceChunks(evidenceChunksJson);
        applyClassificationResult(assessment, result);
        assessmentRepository.save(assessment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeCompleted(Long analysisId) {
        ComplianceAnalysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found: " + analysisId));

        if (analysis.getStatus() != AnalysisStatus.IN_PROGRESS) {
            return;
        }

        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setCompletedAt(Instant.now());
        analysisRepository.save(analysis);
    }

    @Transactional
    public int markInterruptedAnalysesAsFailed(String failureReason) {
        List<ComplianceAnalysis> interrupted = analysisRepository.findByStatus(AnalysisStatus.IN_PROGRESS);
        if (interrupted.isEmpty()) {
            return 0;
        }

        Instant completedAt = Instant.now();
        for (ComplianceAnalysis analysis : interrupted) {
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setFailureReason(failureReason);
            analysis.setCompletedAt(completedAt);
        }
        analysisRepository.saveAll(interrupted);
        return interrupted.size();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAnalysisFailed(Long analysisId, Throwable cause) {
        ComplianceAnalysis analysis = analysisRepository.findById(analysisId).orElse(null);
        if (analysis == null || analysis.getStatus() != AnalysisStatus.IN_PROGRESS) {
            return;
        }

        analysis.setStatus(AnalysisStatus.FAILED);
        analysis.setFailureReason(sanitizeFailureReason(cause));
        analysis.setCompletedAt(Instant.now());
        analysisRepository.save(analysis);
    }

    static String sanitizeFailureReason(Throwable cause) {
        if (cause == null) {
            return GENERIC_FAILURE_REASON;
        }
        if (cause instanceof ComplianceClassificationException classificationException) {
            String safeDetail = classificationException.getSafeDetail();
            if (safeDetail != null && !safeDetail.isBlank()) {
                return safeDetail;
            }
        }
        String message = cause.getMessage();
        if (message != null && !message.isBlank() && !looksSensitive(message)) {
            return message;
        }
        return GENERIC_FAILURE_REASON;
    }

    private static boolean looksSensitive(String message) {
        String normalized = message.toLowerCase();
        return normalized.contains("api key")
                || normalized.contains("apikey")
                || normalized.contains("authorization")
                || normalized.contains("bearer ")
                || normalized.contains("secret");
    }

    private void applyClassificationResult(
            RequirementAssessment assessment, ComplianceClassificationResult result) {
        if (result.status().isLegacyRetrievalStatus()) {
            throw new IllegalStateException(
                    "Cannot persist legacy retrieval assessment status: " + result.status());
        }
        AssessmentStatus status = result.status();
        assessment.setAssessmentStatus(status);
        assessment.setConfidence(result.confidence());
        assessment.setExplanation(result.explanation());
        assessment.setGap(result.gap());
        assessment.setRecommendation(result.recommendation());
    }
}
