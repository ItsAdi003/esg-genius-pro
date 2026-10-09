package dev.esgenius.service.compliance;

import dev.esgenius.entity.AssessmentStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class QuotaAwareComplianceClassificationProviderTest {

    @Test
    void stopsDelegatingAfterQuotaOnThirdRequirement() {
        AnalysisRunContext runContext = new AnalysisRunContext();
        AtomicInteger calls = new AtomicInteger();
        ComplianceClassificationProvider fake = request -> {
            int n = calls.incrementAndGet();
            if (n == 3) {
                throw new ComplianceClassificationException(
                        ClassificationFailureCategory.QUOTA_EXHAUSTED,
                        "Gemini API returned HTTP 429",
                        false,
                        429,
                        1,
                        "Daily quota exhausted");
            }
            return new ComplianceClassificationResult(
                    AssessmentStatus.COVERED, 0.9, "OK", null, null);
        };
        QuotaAwareComplianceClassificationProvider wrapper =
                new QuotaAwareComplianceClassificationProvider(fake);

        int quotaFailures = 0;
        int covered = 0;
        for (int i = 0; i < 14; i++) {
            try {
                ComplianceClassificationResult result = wrapper.classify(sampleRequest(runContext));
                if (result.status() == AssessmentStatus.COVERED) {
                    covered++;
                }
            } catch (ComplianceClassificationException ex) {
                assertThat(ex.getCategory()).isEqualTo(ClassificationFailureCategory.QUOTA_EXHAUSTED);
                quotaFailures++;
            }
        }

        assertThat(calls.get()).isEqualTo(3);
        assertThat(covered).isEqualTo(2);
        assertThat(quotaFailures).isEqualTo(12);
    }

    private static ComplianceClassificationRequest sampleRequest(AnalysisRunContext runContext) {
        return new ComplianceClassificationRequest(
                "ENV-003", "Scope 1", "Desc", "Text",
                List.of("Scope 1 emissions totalled 1000 tCO2e."),
                runContext);
    }
}
