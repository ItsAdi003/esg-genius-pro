package dev.esgenius.service.compliance;

import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.entity.FrameworkRequirement;
import org.springframework.stereotype.Component;

@Component
public class DeterministicNoEvidenceClassifier {

    private static final double NO_EVIDENCE_CONFIDENCE = 0.75;

    public ComplianceClassificationResult classify(FrameworkRequirement requirement) {
        String title = requirement.getTitle();
        String gap = "The submitted document does not appear to contain disclosure addressing: " + title + ".";
        String recommendation = "Include clear disclosure in the submitted document that addresses the requirement: "
                + title + ".";
        if (requirement.getDescription() != null && !requirement.getDescription().isBlank()) {
            recommendation += " " + requirement.getDescription();
        }

        return new ComplianceClassificationResult(
                AssessmentStatus.NOT_COVERED,
                NO_EVIDENCE_CONFIDENCE,
                "No relevant disclosure for this requirement was retrieved from the submitted document.",
                gap,
                recommendation);
    }
}
