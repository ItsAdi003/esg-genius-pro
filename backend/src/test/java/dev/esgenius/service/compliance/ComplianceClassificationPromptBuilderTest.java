package dev.esgenius.service.compliance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceClassificationPromptBuilderTest {

    private final ComplianceClassificationPromptBuilder promptBuilder = new ComplianceClassificationPromptBuilder();

    @Test
    void includesRequirementAndEvidenceOnly() {
        ComplianceClassificationRequest request = new ComplianceClassificationRequest(
                "ENV-003",
                "Scope 1 GHG Emissions",
                "Disclose Scope 1 emissions",
                "BRSR framework text",
                List.of("Scope 1 emissions totalled 1000 tCO2e."));

        String prompt = promptBuilder.buildPrompt(request);

        assertThat(prompt).contains("Scope 1 GHG Emissions");
        assertThat(prompt).contains("BRSR framework text");
        assertThat(prompt).contains("Scope 1 emissions totalled 1000 tCO2e.");
        assertThat(prompt).contains("NOT ESG performance scoring");
        assertThat(prompt).contains("Do not use outside knowledge");
        assertThat(prompt).contains("COVERED");
        assertThat(prompt).contains("HUMAN_REVIEW_REQUIRED");
    }

    @Test
    void prohibitsCriterionExpansionBeyondSuppliedRubric() {
        ComplianceClassificationRequest request = new ComplianceClassificationRequest(
                "ENV-003",
                "Scope 1 GHG Emissions",
                "Direct greenhouse gas emissions from owned or controlled sources.",
                "The entity shall disclose total Scope 1 emissions in metric tonnes of CO2 equivalent.",
                List.of("Total Scope 1 emissions were 12,450 metric tonnes CO2e."));

        String prompt = promptBuilder.buildPrompt(request);

        assertThat(prompt).contains("EVALUATION RUBRIC (complete");
        assertThat(prompt).contains("Do NOT add, infer, or import additional disclosure criteria");
        assertThat(prompt).contains("Do NOT require methodology, emission factors");
        assertThat(prompt).contains("Do NOT infer requirements from the requirement code alone");
        assertThat(prompt).contains("return COVERED even if additional information that could exist in a broader real-world framework is absent");
        assertThat(prompt).contains("total Scope 1 emissions in metric tonnes of CO2 equivalent");
        assertThat(prompt).doesNotContain("Code: ENV-003");
    }

    @Test
    void scope1RubricWithQuantitativeEvidenceAllowsCoveredWithoutMethodology() {
        String frameworkText = "The entity shall disclose total Scope 1 emissions in metric tonnes of CO2 equivalent.";
        ComplianceClassificationRequest request = new ComplianceClassificationRequest(
                "ENV-003",
                "Scope 1 GHG Emissions",
                "Direct greenhouse gas emissions from owned or controlled sources.",
                frameworkText,
                List.of("Total Scope 1 emissions were 12,450 metric tonnes CO2 equivalent for FY 2024-25."));

        String prompt = promptBuilder.buildPrompt(request);

        assertThat(prompt).contains(frameworkText);
        assertThat(prompt).contains("12,450 metric tonnes CO2 equivalent");
        assertThat(prompt).contains("If every material component explicitly requested in the evaluation rubric is present in the evidence, return COVERED");
        assertThat(prompt).contains("never because broader framework practice would normally expect more detail");
        assertThat(prompt).doesNotContain("methodology and emission factors");
    }

    @Test
    void antiCorruptionRubricAllowsCoveredWithoutDisciplinaryActionCounts() {
        String frameworkText = "The entity shall disclose whether an anti-corruption or anti-bribery policy exists.";
        ComplianceClassificationRequest request = new ComplianceClassificationRequest(
                "GOV-001",
                "Anti-Corruption Policy",
                "Anti-bribery and anti-corruption policy existence.",
                frameworkText,
                List.of("The company maintains a formal Anti-Bribery and Anti-Corruption (ABAC) policy applicable to all employees."));

        String prompt = promptBuilder.buildPrompt(request);

        assertThat(prompt).contains(frameworkText);
        assertThat(prompt).contains("Anti-Bribery and Anti-Corruption");
        assertThat(prompt).contains("Do NOT require methodology, emission factors, assurance, breakdowns, ratios, disciplinary-action counts");
        assertThat(prompt).contains("PARTIALLY_COVERED only when a component explicitly requested in the rubric is missing");
        assertThat(prompt).doesNotContain("disciplinary actions taken during the year");
    }

    @Test
    void presentsTitleDescriptionAndFrameworkTextAsAuthoritativeRubric() {
        ComplianceClassificationRequest request = new ComplianceClassificationRequest(
                "ENV-004",
                "Scope 2 GHG Emissions",
                "Indirect emissions from purchased electricity.",
                "The entity shall disclose total Scope 2 emissions in metric tonnes of CO2 equivalent.",
                List.of("Scope 2 emissions from purchased electricity totalled 8,200 metric tonnes CO2e."));

        String prompt = promptBuilder.buildPrompt(request);

        assertThat(prompt).contains("EVALUATION RUBRIC (complete — evaluate only these explicitly stated components):");
        assertThat(prompt).contains("Title: Scope 2 GHG Emissions");
        assertThat(prompt).contains("Description: Indirect emissions from purchased electricity.");
        assertThat(prompt).contains("Framework text: The entity shall disclose total Scope 2 emissions");
        assertThat(prompt).contains("location-based vs market-based methods");
    }
}
