package dev.esgenius.dto;

import java.time.Instant;
import java.util.List;

public record ComplianceAnalysisResponse(
        Long id,
        Long documentId,
        Long frameworkId,
        String frameworkCode,
        String frameworkName,
        String status,
        Instant startedAt,
        Instant completedAt,
        String failureReason,
        int requirementCount,
        List<RequirementAssessmentResponse> assessments) {
}
