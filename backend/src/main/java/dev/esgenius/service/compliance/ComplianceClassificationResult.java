package dev.esgenius.service.compliance;

import dev.esgenius.entity.AssessmentStatus;

public record ComplianceClassificationResult(
        AssessmentStatus status,
        Double confidence,
        String explanation,
        String gap,
        String recommendation) {
}
