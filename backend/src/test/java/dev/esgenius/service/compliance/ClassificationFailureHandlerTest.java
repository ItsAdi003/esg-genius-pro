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
