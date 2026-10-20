package dev.esgenius.service.compliance;

import dev.esgenius.entity.AssessmentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificationFailureHandlerTest {

    private final ClassificationFailureHandler handler = new ClassificationFailureHandler();

    @Test
    void genericFailureUsesAutomatedClassificationMessage() {
        ComplianceClassificationResult result = handler.handleFailure(new AnalysisRunContext());

        assertThat(result.status()).isEqualTo(AssessmentStatus.HUMAN_REVIEW_REQUIRED);
        assertThat(result.explanation()).isEqualTo(ClassificationFailureHandler.GENERIC_FAILURE_EXPLANATION);
    }

    private static ComplianceClassificationException failure(ClassificationFailureCategory category, Integer status) {
        return new ComplianceClassificationException(category, "failed", false, status, 1, "detail");
    }

    @Test
    void failureReasonIsIncludedInTheExplanation() {
        AnalysisRunContext context = new AnalysisRunContext();

        assertThat(handler.handleFailure(context, failure(ClassificationFailureCategory.BLOCKED_RESPONSE, null))
                .explanation())
                .startsWith(ClassificationFailureHandler.GENERIC_FAILURE_EXPLANATION)
                .endsWith("Reason: the AI provider blocked the response.");
        assertThat(handler.handleFailure(context, failure(ClassificationFailureCategory.PARSE_FAILURE, null))
                .explanation()).contains("malformed or cut off");
        assertThat(handler.handleFailure(context, failure(ClassificationFailureCategory.NETWORK_TIMEOUT, null))
                .explanation()).contains("timed out");
        assertThat(handler.handleFailure(context, failure(ClassificationFailureCategory.HTTP_RATE_LIMIT, 429))
                .explanation()).contains("rate limit was reached (HTTP 429)");
        assertThat(handler.handleFailure(context, failure(ClassificationFailureCategory.HTTP_SERVER_ERROR, 503))
                .explanation()).contains("server error (HTTP 503)");
        assertThat(handler.handleFailure(context, failure(ClassificationFailureCategory.HTTP_CLIENT_ERROR, 400))
                .explanation()).contains("rejected the request (HTTP 400)");
    }

    @Test
    void failureResultStillRoutesToHumanReviewWithNoConfidence() {
        ComplianceClassificationResult result = handler.handleFailure(
                new AnalysisRunContext(), failure(ClassificationFailureCategory.OTHER, null));

        assertThat(result.status()).isEqualTo(AssessmentStatus.HUMAN_REVIEW_REQUIRED);
        assertThat(result.confidence()).isNull();
        assertThat(result.recommendation()).isNotBlank();
    }

    @Test
    void quotaExhaustionOverridesTheSpecificReason() {
        AnalysisRunContext context = new AnalysisRunContext();
        context.markQuotaExhausted();

        assertThat(handler.handleFailure(context, failure(ClassificationFailureCategory.HTTP_SERVER_ERROR, 503))
                .explanation()).isEqualTo(ClassificationFailureHandler.QUOTA_EXHAUSTED_EXPLANATION);
    }

    @Test
    void aMissingCauseIsDescribedAsUnexpected() {
        assertThat(handler.handleFailure(new AnalysisRunContext(), null).explanation())
                .endsWith("Reason: an unexpected error.");
    }

    @Test
    void quotaExhaustionUsesDailyQuotaExplanation() {
        AnalysisRunContext runContext = new AnalysisRunContext();
        runContext.markQuotaExhausted();

        ComplianceClassificationResult result = handler.handleFailure(runContext);

        assertThat(result.status()).isEqualTo(AssessmentStatus.HUMAN_REVIEW_REQUIRED);
        assertThat(result.explanation()).isEqualTo(ClassificationFailureHandler.QUOTA_EXHAUSTED_EXPLANATION);
        assertThat(result.explanation()).doesNotContain("Automated classification could not be completed");
    }
}
