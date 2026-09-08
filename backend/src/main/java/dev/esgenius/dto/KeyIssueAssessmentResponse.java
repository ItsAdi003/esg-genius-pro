package dev.esgenius.dto;

/**
 * Key issue assessment response.
 */
public record KeyIssueAssessmentResponse(
        Long id,
        String issueCode,
        String issueName,
        String pillar,
        Double score,
        String riskLevel) {
}
