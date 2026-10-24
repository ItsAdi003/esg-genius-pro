package dev.esgenius.service;

import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.DocumentDetailResponse;
import dev.esgenius.dto.EsgScoreResponse;
import dev.esgenius.dto.EvidenceChunkResponse;
import dev.esgenius.dto.RequirementAssessmentResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GapAssessmentPdfServiceTest {

    private final GapAssessmentPdfService pdfService = new GapAssessmentPdfService(null, null);

    @Test
    void renderIncludesHeaderCountsGapsCoveredCitationsAndDisclaimer() throws IOException {
        ComplianceAnalysisResponse analysis = analysis(
                List.of(
                        assessment("ENV-001", "Total Energy Consumption", "ENVIRONMENTAL", "NOT_COVERED",
                                "Energy intensity is not disclosed.", "Disclose energy intensity.",
                                null, List.of()),
                        assessment("SOC-001", "Employee Health & Safety", "SOCIAL", "PARTIALLY_COVERED",
                                "LTIFR is missing for contractors.", "Include contractor LTIFR.",
                                null, List.of()),
                        assessment("GOV-003", "Whistleblower Mechanism", "GOVERNANCE", "HUMAN_REVIEW_REQUIRED",
                                null, null, null, List.of()),
                        assessment("ENV-003", "Scope 1 GHG Emissions", "ENVIRONMENTAL", "COVERED",
                                null, null, "Scope 1 emissions were 3,200 tCO2e.",
                                List.of(new EvidenceChunkResponse(4, 12, "Scope 1 emissions were 3,200 tCO2e.", 12.5))),
                        assessment("ENV-004", "Scope 2 GHG Emissions", "ENVIRONMENTAL", "COVERED",
                                null, null, "Purchased electricity emissions.",
                                List.of(new EvidenceChunkResponse(7, null, "Purchased electricity emissions.", 9.1)))));

        DocumentDetailResponse document = document("Northwind Limited", "northwind-brsr.pdf", 2025);

        byte[] pdf = pdfService.render(analysis, document);
        assertThat(pdf).startsWith("%PDF".getBytes());

        String text = extractText(pdf);
        assertThat(text).contains("ESG Gap Assessment Report");
        assertThat(text).contains("Northwind Limited");
        assertThat(text).contains("Reporting year: 2025");
        assertThat(text).contains("northwind-brsr.pdf");
        assertThat(text).contains("Covered: 2");
        assertThat(text).contains("Not covered: 1");
        assertThat(text).contains("Partially covered: 1");
        assertThat(text).contains("Human review required: 1");
        assertThat(text).contains("Environmental");
        assertThat(text).contains("ENV-001");
        assertThat(text).contains("Energy intensity is not disclosed.");
        assertThat(text).contains("Disclose energy intensity.");
        assertThat(text).contains("GOV-003");
        assertThat(text).contains("Page 12");
        assertThat(text).contains("Source chunk 7");
        assertThat(text).contains("Started: 2026-01-15 10:15 UTC");
        assertThat(text).contains("Completed: 2026-01-15 10:16 UTC");
        assertThat(text).doesNotContain("T10:15:30Z");
        assertThat(text).contains("14 selected BRSR requirements");
        assertThat(text).doesNotContain("official SEBI certification");
        assertThat(text).doesNotContain("official MSCI ratings");
    }

    @Test
    void renderPaginatesWhenContentOverflowsASinglePage() throws IOException {
        List<RequirementAssessmentResponse> assessments = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            assessments.add(assessment(
                    "GAP-%03d".formatted(i),
                    "Long paraphrased disclosure title " + i,
                    "ENVIRONMENTAL",
                    "NOT_COVERED",
                    "This gap description is intentionally verbose so the generated PDF must continue onto additional pages. ".repeat(3),
                    "This recommendation is also long enough to wrap across several lines of Helvetica body text. ".repeat(3),
                    null,
                    List.of()));
        }
        byte[] pdf = pdfService.render(analysis(assessments), document("Paginated Org", "paginated.pdf", 2024));

        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            assertThat(loaded.getNumberOfPages()).isGreaterThan(1);
        }
        assertThat(extractText(pdf)).contains("GAP-030");
    }

    @Test
    void formatEvidenceSourceLabelMatchesFrontendPageThenChunkFallback() {
        assertThat(GapAssessmentPdfService.formatEvidenceSourceLabel(
                new EvidenceChunkResponse(2, 8, "text", 1.0))).isEqualTo("Page 8");
        assertThat(GapAssessmentPdfService.formatEvidenceSourceLabel(
                new EvidenceChunkResponse(2, null, "text", 1.0))).isEqualTo("Source chunk 2");
    }

    @Test
    void structuredBrsrAnalysisShowsSectionBreakdownPlacementAndFullFrameworkDisclaimer() throws IOException {
        ComplianceAnalysisResponse analysis = analysis(List.of(
                placed(assessment("A-01", "Corporate Identity Number (CIN)", "GOVERNANCE", "COVERED",
                        null, null, "CIN is L85110KA1981PLC013115.",
                        List.of(new EvidenceChunkResponse(1, 2, "CIN", 9.0))), "A", "GENERAL"),
                placed(assessment("P6-E06", "Scope 1 and Scope 2 GHG Emissions", "ENVIRONMENTAL", "PARTIALLY_COVERED",
                        "Scope 2 method is not stated.", "State the Scope 2 method.", null, List.of()),
                        "P6", "ESSENTIAL"),
                placed(assessment("P6-L04", "Scope 3 GHG Emissions and Intensity", "ENVIRONMENTAL", "NOT_COVERED",
                        "No Scope 3 total.", "Disclose Scope 3.", null, List.of()), "P6", "LEADERSHIP"),
                placed(assessment("P3-E10", "Occupational Health and Safety Management System", "SOCIAL",
                        "HUMAN_REVIEW_REQUIRED", null, null, null, List.of()), "P3", "ESSENTIAL")));

        String text = extractText(pdfService.render(analysis, document("Infosys Limited", "infosys.pdf", 2025)));

        assertThat(text).contains("Coverage by BRSR section");
        assertThat(text).contains("Section A");
        assertThat(text).contains("Principle 6");
        assertThat(text).contains("Principle 3");
        // Principle 6 holds two of the four assessments: one partial, one not covered.
        assertThat(text).containsPattern("Principle 6 .{1,4}total 2, covered 0, partial 1, not covered 1, review 0");
        assertThat(text).contains("Principle 6, Essential");
        assertThat(text).contains("Principle 6, Leadership");
        assertThat(text).contains("Section A, Section B and the nine principles of Section C");
        assertThat(text).doesNotContain("14 selected BRSR requirements");
    }

    @Test
    void legacyAnalysisWithoutSectionsKeepsThePrototypeDisclaimerAndNoSectionBreakdown() throws IOException {
        ComplianceAnalysisResponse analysis = analysis(List.of(
                assessment("ENV-003", "Scope 1 GHG Emissions", "ENVIRONMENTAL", "COVERED", null, null,
                        "Scope 1 was 3,200 tCO2e.", List.of(new EvidenceChunkResponse(4, 12, "Scope 1", 12.5)))));

        String text = extractText(pdfService.render(analysis, document("Northwind", "n.pdf", 2025)));

        assertThat(text).contains("14 selected BRSR requirements");
        assertThat(text).doesNotContain("Coverage by BRSR section");
    }

    @Test
    void formatsSectionsAndPlacements() {
        assertThat(GapAssessmentPdfService.formatSection("A")).isEqualTo("Section A");
        assertThat(GapAssessmentPdfService.formatSection("P6")).isEqualTo("Principle 6");
        assertThat(GapAssessmentPdfService.formatSection(null)).isEmpty();
        assertThat(GapAssessmentPdfService.formatPlacement("P6", "ESSENTIAL")).isEqualTo("Principle 6, Essential");
        assertThat(GapAssessmentPdfService.formatPlacement("B", "MANAGEMENT")).isEqualTo("Section B");
        assertThat(GapAssessmentPdfService.formatPlacement(null, null)).isEmpty();
    }

    @Test
    void renderListsNotApplicableItemsWithReasonAndShowsTheEsgScore() throws IOException {
        RequirementAssessmentResponse notApplicable = new RequirementAssessmentResponse(
                9L, "P6-E04", "Water withdrawal", "ENVIRONMENTAL", "NOT_APPLICABLE",
                null, null, null, null, null, null, List.of(), "P6", "ESSENTIAL",
                "The entity does not use process water.");
        ComplianceAnalysisResponse analysis = analysis(List.of(
                assessment("ENV-003", "Scope 1 GHG Emissions", "ENVIRONMENTAL", "COVERED",
                        null, null, "Scope 1 emissions were 3,200 tCO2e.", List.of()),
                notApplicable));
        EsgScoreResponse score = new EsgScoreResponse(
                1L, "ENERGY", "Energy", true, 7.5, 6.0, true, 4, 3.0, 9.0, "A",
                8.0, 7.0, 6.0, 0.45, 0.22, 0.33, 1, 1, 0, 2, List.of(), "note");

        byte[] pdf = pdfService.render(analysis, document("Northwind Limited", "northwind-brsr.pdf", 2025), score);

        String text = extractText(pdf);
        assertThat(text).contains("Not applicable (marked by a reviewer, excluded from the score): 1");
        assertThat(text).contains("P6-E04");
        assertThat(text).contains("The entity does not use process water.");
        assertThat(text).contains("MSCI-style ESG score");
        assertThat(text).contains("Rating band: A");
        assertThat(text).contains("Adjusted against 4 analyses in the industry: Energy.");
    }

    private static RequirementAssessmentResponse placed(
            RequirementAssessmentResponse base, String section, String indicatorType) {
        return new RequirementAssessmentResponse(
                base.requirementId(), base.requirementCode(), base.requirementTitle(), base.category(),
                base.assessmentStatus(), base.confidence(), base.explanation(), base.gap(), base.recommendation(),
                base.retrievalScore(), base.evidenceText(), base.evidenceChunks(), section, indicatorType, base.notApplicableReason());
    }

    private static String extractText(byte[] pdf) throws IOException {
        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(loaded);
        }
    }

    private static ComplianceAnalysisResponse analysis(List<RequirementAssessmentResponse> assessments) {
        return new ComplianceAnalysisResponse(
                42L,
                7L,
                1L,
                "BRSR",
                "Business Responsibility and Sustainability Report",
                "COMPLETED",
                Instant.parse("2026-01-15T10:15:30Z"),
                Instant.parse("2026-01-15T10:16:00Z"),
                null,
                assessments.size(),
                assessments);
    }

    private static DocumentDetailResponse document(String orgName, String filename, Integer year) {
        return new DocumentDetailResponse(
                7L,
                1L,
                orgName,
                filename,
                "BRSR",
                year,
                "READY",
                1024L,
                10,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:01Z"),
                "extracted",
                null,
                false,
                false);
    }

    private static RequirementAssessmentResponse assessment(
            String code,
            String title,
            String category,
            String status,
            String gap,
            String recommendation,
            String evidenceText,
            List<EvidenceChunkResponse> chunks) {
        return new RequirementAssessmentResponse(
                1L, code, title, category, status, 0.8, "explanation", gap, recommendation, 12.0, evidenceText, chunks, null, null, null);
    }
}
