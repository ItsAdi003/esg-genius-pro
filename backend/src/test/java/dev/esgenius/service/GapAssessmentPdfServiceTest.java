package dev.esgenius.service;

import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.DocumentDetailResponse;
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
                1L, code, title, category, status, 0.8, "explanation", gap, recommendation, 12.0, evidenceText, chunks);
    }
}
