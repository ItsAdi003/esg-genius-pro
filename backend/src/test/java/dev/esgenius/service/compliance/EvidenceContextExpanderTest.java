package dev.esgenius.service.compliance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceContextExpanderTest {

    private final EvidenceContextExpander expander = new EvidenceContextExpander();

    @Test
    void scope1ContextIncludesNearbyQuantitativeRow() {
        String chunk = String.join("\n",
                "Greenhouse gas emissions overview",
                "The entity reports direct and indirect emissions across operations.",
                "Scope 1 emissions boundary includes owned facilities and company vehicles.",
                "Total Scope 1 emissions were 12,450 metric tonnes CO2 equivalent for FY 2024-25.",
                "Scope 2 emissions from purchased electricity were reported separately.");

        String matched = "Scope 1 emissions boundary includes owned facilities and company vehicles.";

        String expanded = expander.expandPassageContext(chunk, matched);

        assertThat(expanded).contains("Scope 1 emissions boundary");
        assertThat(expanded).contains("12,450 metric tonnes CO2 equivalent");
        assertThat(expanded.length()).isLessThanOrEqualTo(EvidenceContextExpander.MAX_EXPANDED_PASSAGE_CHARS);
    }

    @Test
    void waterContextIncludesNearbyQuantitativeRows() {
        String chunk = String.join("\n",
                "Water stewardship",
                "Water withdrawal by source (in kilolitres)",
                "Groundwater withdrawal totalled 450,000 kilolitres.",
                "Surface water withdrawal totalled 320,000 kilolitres.",
                "Third-party water withdrawal totalled 180,000 kilolitres.");

        String matched = "Water withdrawal by source (in kilolitres)";

        String expanded = expander.expandPassageContext(chunk, matched);

        assertThat(expanded).contains("Water withdrawal by source");
        assertThat(expanded).contains("450,000 kilolitres");
        assertThat(expanded).contains("320,000 kilolitres");
    }

    @Test
    void expandedContextRemainsBounded() {
        String chunk = "A".repeat(5000) + " Scope 1 emissions totalled 999 tCO2e. " + "B".repeat(5000);
        String matched = "Scope 1 emissions totalled 999 tCO2e.";

        String expanded = expander.expandPassageContext(chunk, matched);

        assertThat(expanded).contains("999 tCO2e");
        assertThat(expanded.length()).isLessThanOrEqualTo(EvidenceContextExpander.MAX_EXPANDED_PASSAGE_CHARS);
    }
}
