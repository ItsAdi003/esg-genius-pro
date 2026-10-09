package dev.esgenius.service.compliance;

import dev.esgenius.entity.AssessmentStatus;
import org.springframework.stereotype.Component;

@Component
public class ClassificationFailureHandler {

    public static final String GENERIC_FAILURE_EXPLANATION =
            "Automated classification could not be completed for this requirement.";
    public static final String QUOTA_EXHAUSTED_EXPLANATION =
            "AI classification was unavailable because the daily AI quota was reached. A reviewer should assess this requirement.";
    private static final String HUMAN_REVIEW_RECOMMENDATION =
            "A human reviewer should assess this requirement against the submitted document.";

    public ComplianceClassificationResult handleFailure(AnalysisRunContext runContext) {
        if (runContext != null && runContext.isQuotaExhausted()) {
            return quotaExhaustedResult();
        }
        return unexpectedFailure();
    }

    public ComplianceClassificationResult unexpectedFailure() {
        return new ComplianceClassificationResult(
                AssessmentStatus.HUMAN_REVIEW_REQUIRED,
                null,
                GENERIC_FAILURE_EXPLANATION,
                null,
                HUMAN_REVIEW_RECOMMENDATION);
    }

    private static ComplianceClassificationResult quotaExhaustedResult() {
        return new ComplianceClassificationResult(
                AssessmentStatus.HUMAN_REVIEW_REQUIRED,
                null,
                QUOTA_EXHAUSTED_EXPLANATION,
                null,
                HUMAN_REVIEW_RECOMMENDATION);
    }
}
