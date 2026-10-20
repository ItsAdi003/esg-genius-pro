package dev.esgenius.service.compliance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceBatchPromptBuilderTest {

    private final ComplianceClassificationPromptBuilder builder = new ComplianceClassificationPromptBuilder();

    private static ComplianceClassificationRequest request(String code, String title, String passage) {
        return new ComplianceClassificationRequest(
                code, title, "Description of " + title, "Framework text of " + title, List.of(passage));
    }

    @Test
    void batchPromptListsEveryRequirementWithItsOwnRubricAndEvidence() {
        String prompt = builder.buildBatchPrompt(List.of(
                request("P6-E01", "Total Energy Consumption", "Energy was 2.4 million GJ."),
                request("P6-E03", "Water Withdrawal", "Water withdrawn was 1.2 million kilolitres.")));

        assertThat(prompt)
                .contains("requirementCode: P6-E01")
                .contains("requirementCode: P6-E03")
                .contains("Title: Total Energy Consumption")
                .contains("Framework text of Water Withdrawal")
                .contains("DOCUMENT EVIDENCE PASSAGES for P6-E01")
                .contains("DOCUMENT EVIDENCE PASSAGES for P6-E03")
                .contains("Energy was 2.4 million GJ.")
                .contains("Water withdrawn was 1.2 million kilolitres.");
        // each requirement's evidence sits under its own header, in order
        assertThat(prompt.indexOf("Energy was 2.4 million GJ.")).isLessThan(prompt.indexOf("requirementCode: P6-E03"));
        assertThat(prompt.indexOf("requirementCode: P6-E03")).isLessThan(prompt.indexOf("Water withdrawn was"));
    }

    @Test
    void batchPromptAsksForOneObjectPerRequirementKeyedByCode() {
        String prompt = builder.buildBatchPrompt(List.of(
                request("A-01", "CIN", "CIN is L85110KA1981PLC013115."),
                request("A-02", "Name", "Infosys Limited."),
                request("A-03", "Year", "Incorporated 1981.")));

        assertThat(prompt)
                .contains("For EACH of the 3 requirements")
                .contains("JSON array with exactly one object per requirement")
                .contains("requirementCode copied exactly")
                .contains("never use another requirement's passages as evidence");
    }

    @Test
    void batchPromptKeepsTheSameStrictRubricRulesAsTheSinglePrompt() {
        ComplianceClassificationRequest only = request("P1-E04", "Anti-Corruption Policy", "ABAC policy exists.");
        String single = builder.buildPrompt(only);
        String batch = builder.buildBatchPrompt(List.of(only, request("P1-E05", "Disciplinary Action", "None.")));

        for (String rule : List.of(
                "CLASSIFICATION STATUSES (use exactly one):",
                "STRICT RUBRIC RULES (mandatory):",
                "Do NOT add, infer, or import additional disclosure criteria",
                "Never use the words 'rubric', 'evaluation rubric', 'prompt', or 'instructions'")) {
            assertThat(single).contains(rule);
            assertThat(batch).contains(rule);
        }
    }

    @Test
    void singlePromptIsUnchangedByTheRefactor() {
        String prompt = builder.buildPrompt(request("P1-E04", "Anti-Corruption Policy", "ABAC policy exists."));

        assertThat(prompt)
                .startsWith("You are an AI assistant performing disclosure coverage analysis")
                .contains("TASK: Determine whether the supplied document evidence contains the disclosure requested by the requirement rubric below.")
                .contains("EVALUATION RUBRIC (complete")
                .contains("Title: Anti-Corruption Policy")
                .contains("\nDOCUMENT EVIDENCE PASSAGES:\n--- Passage 1 ---\nABAC policy exists.\n")
                .doesNotContain("requirementCode");
    }
}
