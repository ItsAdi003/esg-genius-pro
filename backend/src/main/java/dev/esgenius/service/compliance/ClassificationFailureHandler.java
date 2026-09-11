package dev.esgenius.service.compliance;

import dev.esgenius.entity.AssessmentStatus;
import org.springframework.stereotype.Component;

@Component
public class ClassificationFailureHandler {

    public ComplianceClassificationResult handleFailure() {
        return new ComplianceClassificationResult(
                AssessmentStatus.HUMAN_REVIEW_REQUIRED,
                null,
                "Automated classification could not be completed for this requirement.",
                null,
                "A human reviewer should assess this requirement against the submitted document.");
    }
}
