package dev.esgenius.service.compliance;

import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.entity.EsgCategory;
import dev.esgenius.entity.Framework;
import dev.esgenius.entity.FrameworkRequirement;
import dev.esgenius.entity.FrameworkStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicNoEvidenceClassifierTest {

    private final DeterministicNoEvidenceClassifier classifier = new DeterministicNoEvidenceClassifier();

    @Test
    void producesDeterministicNotCoveredResult() {
        Framework framework = new Framework(
                "BRSR", "BRSR", "Business Responsibility", "India", "1.0", FrameworkStatus.ACTIVE);
        FrameworkRequirement requirement = new FrameworkRequirement(
                framework, "ENV-099", "Scope 1 GHG emissions",
                EsgCategory.ENVIRONMENTAL, "Disclose Scope 1 emissions", "Framework text", true, "1.0");

        ComplianceClassificationResult result = classifier.classify(requirement);

        assertThat(result.status()).isEqualTo(AssessmentStatus.NOT_COVERED);
        assertThat(result.confidence()).isEqualTo(0.75);
        assertThat(result.explanation())
                .isEqualTo("No relevant disclosure for this requirement was retrieved from the submitted document.");
        assertThat(result.gap()).contains("Scope 1 GHG emissions");
        assertThat(result.recommendation()).contains("Scope 1 GHG emissions");
    }
}
