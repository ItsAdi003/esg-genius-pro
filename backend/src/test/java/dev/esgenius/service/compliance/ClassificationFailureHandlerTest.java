package dev.esgenius.service.compliance;

import dev.esgenius.entity.AssessmentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificationFailureHandlerTest {

    private final ClassificationFailureHandler handler = new ClassificationFailureHandler();

    @BeforeEach
    void setUp() {
        QuotaExhaustionScope.begin();
    }

    @AfterEach
    void tearDown() {
        QuotaExhaustionScope.end();
    }

    @Test
    void genericFailureUsesAutomatedClassificationMessage() {
        ComplianceClassificationResult result = handler.handleFailure();

        assertThat(result.status()).isEqualTo(AssessmentStatus.HUMAN_REVIEW_REQUIRED);
        assertThat(result.explanation()).isEqualTo(ClassificationFailureHandler.GENERIC_FAILURE_EXPLANATION);
    }

    @Test
    void quotaExhaustionUsesDailyQuotaExplanation() {
        QuotaExhaustionScope.markExhausted();

        ComplianceClassificationResult result = handler.handleFailure();

        assertThat(result.status()).isEqualTo(AssessmentStatus.HUMAN_REVIEW_REQUIRED);
        assertThat(result.explanation()).isEqualTo(ClassificationFailureHandler.QUOTA_EXHAUSTED_EXPLANATION);
        assertThat(result.explanation()).doesNotContain("Automated classification could not be completed");
    }
}
