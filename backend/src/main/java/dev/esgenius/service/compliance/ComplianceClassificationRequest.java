package dev.esgenius.service.compliance;

import java.util.List;

public record ComplianceClassificationRequest(
        String requirementCode,
        String requirementTitle,
        String requirementDescription,
        String requirementFrameworkText,
        List<String> evidencePassages) {
}
