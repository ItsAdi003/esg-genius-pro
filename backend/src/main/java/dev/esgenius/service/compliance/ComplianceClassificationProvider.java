package dev.esgenius.service.compliance;

public interface ComplianceClassificationProvider {

    ComplianceClassificationResult classify(ComplianceClassificationRequest request);
}
