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

    /**
     * Fallback for a failed model call, with the reason in plain words so a reviewer (and a developer
     * looking at a result) can tell a blocked response from a timeout or a rate limit. Quota exhaustion
     * keeps its own message.
     */
    public ComplianceClassificationResult handleFailure(
            AnalysisRunContext runContext, ComplianceClassificationException cause) {
        if (runContext != null && runContext.isQuotaExhausted()) {
            return quotaExhaustedResult();
        }
        return new ComplianceClassificationResult(
                AssessmentStatus.HUMAN_REVIEW_REQUIRED,
                null,
                GENERIC_FAILURE_EXPLANATION + " Reason: " + describe(cause) + ".",
                null,
                HUMAN_REVIEW_RECOMMENDATION);
    }

    static String describe(ComplianceClassificationException cause) {
        if (cause == null || cause.getCategory() == null) {
            return "an unexpected error";
        }
        String status = cause.getHttpStatus() != null ? " (HTTP " + cause.getHttpStatus() + ")" : "";
        return switch (cause.getCategory()) {
            case BLOCKED_RESPONSE -> "the AI provider blocked the response";
            case PARSE_FAILURE, MALFORMED_RESPONSE -> "the AI response could not be read (malformed or cut off)";
            case VALIDATION_FAILURE -> "the AI response was incomplete";
            case NETWORK_TIMEOUT -> "the AI request timed out";
            case HTTP_RATE_LIMIT -> "the AI provider's rate limit was reached" + status;
            case HTTP_SERVER_ERROR -> "the AI provider returned a server error" + status;
            case HTTP_CLIENT_ERROR -> "the AI provider rejected the request" + status;
            case CONFIGURATION_ERROR -> "the AI provider is not configured correctly" + status;
            case QUOTA_EXHAUSTED -> "the daily AI quota was reached";
            case OTHER -> "an unexpected error";
        };
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
