package dev.esgenius.dto;

import java.util.List;

public record RequirementAssessmentResponse(
        Long requirementId,
        String requirementCode,
        String requirementTitle,
        String category,
        String assessmentStatus,
        Double confidence,
        String explanation,
        String gap,
        String recommendation,
        Double retrievalScore,
        String evidenceText,
        List<EvidenceChunkResponse> evidenceChunks,
        /** Framework section (e.g. "P6") and indicator type (e.g. "ESSENTIAL"); null for legacy requirements. */
        String section,
        String indicatorType,
        /** Reviewer's reason when the status is NOT_APPLICABLE; otherwise null. */
        String notApplicableReason) {
}
