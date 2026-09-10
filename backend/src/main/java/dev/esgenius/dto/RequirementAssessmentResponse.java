package dev.esgenius.dto;

import java.util.List;

public record RequirementAssessmentResponse(
        Long requirementId,
        String requirementCode,
        String requirementTitle,
        String category,
        String assessmentStatus,
        Double retrievalScore,
        String evidenceText,
        List<EvidenceChunkResponse> evidenceChunks) {
}
