package dev.esgenius.dto;

import java.time.Instant;

public record ComplianceAnalysisSummaryResponse(
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
        int coveredCount,
        int partiallyCoveredCount,
        int notCoveredCount,
        int humanReviewRequiredCount,
        int evidenceRetrievedCount,
        int noEvidenceFoundCount) {
}
