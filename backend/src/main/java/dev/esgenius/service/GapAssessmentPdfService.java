package dev.esgenius.service;

import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.DocumentDetailResponse;
import dev.esgenius.dto.EvidenceChunkResponse;
import dev.esgenius.dto.RequirementAssessmentResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Builds a plain multi-page gap-assessment PDF from persisted analysis results.
 */
@Service
public class GapAssessmentPdfService {

    static final String PROTOTYPE_DISCLAIMER =
            "This report is a research prototype covering 14 selected BRSR requirements. "
                    + "It is not a full BRSR assessment and does not constitute SEBI certification, "
                    + "an official MSCI rating, regulatory compliance, or an assurance opinion.";

    static final String REVIEW_DISCLAIMER =
            "This document presents an AI-assisted ESG reporting readiness assessment based on "
                    + "organizational documents and framework requirement sources. Findings should be "
                    + "validated by qualified compliance professionals before external disclosure.";

    private static final float MARGIN = 54f;
    private static final float BODY_SIZE = 10f;
    private static final float HEADING_SIZE = 14f;
    private static final float TITLE_SIZE = 16f;
    private static final float SECTION_SIZE = 12f;
    private static final float BODY_LEADING = 13f;
    private static final float SECTION_LEADING = 16f;

    private static final List<String> ESG_CATEGORIES = List.of("ENVIRONMENTAL", "SOCIAL", "GOVERNANCE");

    private final ComplianceAnalysisService complianceAnalysisService;
    private final DocumentService documentService;

    public GapAssessmentPdfService(
            ComplianceAnalysisService complianceAnalysisService,
            DocumentService documentService) {
        this.complianceAnalysisService = complianceAnalysisService;
        this.documentService = documentService;
    }

    public byte[] generate(Long analysisId) {
        ComplianceAnalysisResponse analysis = complianceAnalysisService.getAnalysis(analysisId);
        DocumentDetailResponse document = documentService.getDocument(analysis.documentId());
        return render(analysis, document);
    }

    byte[] render(ComplianceAnalysisResponse analysis, DocumentDetailResponse document) {
        try {
            return writePdf(analysis, document);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to generate gap assessment PDF for analysis " + analysis.id(), ex);
        }
    }

    private byte[] writePdf(ComplianceAnalysisResponse analysis, DocumentDetailResponse document)
            throws IOException {
        List<RequirementAssessmentResponse> assessments =
                analysis.assessments() == null ? List.of() : analysis.assessments();
        StatusCounts summary = countStatuses(assessments);
        List<RequirementAssessmentResponse> gaps = filterStatuses(assessments, "NOT_COVERED", "PARTIALLY_COVERED");
        List<RequirementAssessmentResponse> review = filterStatuses(assessments, "HUMAN_REVIEW_REQUIRED");
        List<RequirementAssessmentResponse> covered = filterStatuses(assessments, "COVERED");

        PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfLayout layout = new PdfLayout(pdf, regular, bold);

            String organizationName = document.organizationName() != null
                    ? document.organizationName()
                    : "Document #" + analysis.documentId();

            layout.write(bold, TITLE_SIZE, SECTION_LEADING + 4, "ESG Gap Assessment Report");
            layout.write(bold, HEADING_SIZE, SECTION_LEADING, organizationName);
            layout.write(regular, BODY_SIZE, BODY_LEADING,
                    "Framework: " + nullSafe(analysis.frameworkName())
                            + " (" + nullSafe(analysis.frameworkCode()) + ")"
                            + reportingYearSuffix(document.reportingYear()));
            if (document.originalFilename() != null && !document.originalFilename().isBlank()) {
                layout.write(regular, BODY_SIZE, BODY_LEADING, "Source document: " + document.originalFilename());
            }
            layout.blank();
            layout.write(regular, BODY_SIZE, BODY_LEADING, "Analysis #" + analysis.id());
            layout.write(regular, BODY_SIZE, BODY_LEADING, "Status: " + formatAnalysisStatus(analysis.status()));
            layout.write(regular, BODY_SIZE, BODY_LEADING, "Started: " + formatInstant(analysis.startedAt()));
            layout.write(regular, BODY_SIZE, BODY_LEADING, "Completed: " + formatInstant(analysis.completedAt()));
            layout.write(regular, BODY_SIZE, BODY_LEADING,
                    "Requirements in analysis: " + analysis.requirementCount()
                            + "  |  Assessments returned: " + assessments.size());
            layout.blank();

            layout.write(bold, SECTION_SIZE, SECTION_LEADING, "1. Executive Summary");
            layout.writeWrapped(regular, BODY_SIZE, BODY_LEADING, executiveSummary(analysis, assessments.size(), summary));
            layout.blank();
            layout.write(regular, BODY_SIZE, BODY_LEADING, "Covered: " + summary.covered);
            layout.write(regular, BODY_SIZE, BODY_LEADING, "Partially covered: " + summary.partiallyCovered);
            layout.write(regular, BODY_SIZE, BODY_LEADING, "Not covered: " + summary.notCovered);
            layout.write(regular, BODY_SIZE, BODY_LEADING, "Human review required: " + summary.humanReviewRequired);
            if (summary.evidenceRetrieved > 0 || summary.noEvidenceFound > 0) {
                layout.write(regular, BODY_SIZE, BODY_LEADING,
                        "Legacy evidence retrieved: " + summary.evidenceRetrieved
                                + "  |  Legacy no evidence found: " + summary.noEvidenceFound);
            }
            layout.blank();

            layout.write(bold, SECTION_SIZE, SECTION_LEADING, "2. Coverage by ESG Category");
            boolean anyCategory = false;
            for (String category : ESG_CATEGORIES) {
                List<RequirementAssessmentResponse> inCategory = assessments.stream()
                        .filter(item -> category.equals(item.category()))
                        .toList();
                if (inCategory.isEmpty()) {
                    continue;
                }
                anyCategory = true;
                StatusCounts counts = countStatuses(inCategory);
                layout.write(regular, BODY_SIZE, BODY_LEADING,
                        formatCategory(category)
                                + " — total " + inCategory.size()
                                + ", covered " + counts.covered
                                + ", partial " + counts.partiallyCovered
                                + ", not covered " + counts.notCovered
                                + ", review " + counts.humanReviewRequired);
            }
            if (!anyCategory) {
                layout.write(regular, BODY_SIZE, BODY_LEADING, "No assessments are available to summarize by category.");
            }
            layout.blank();

            layout.write(bold, SECTION_SIZE, SECTION_LEADING, "3. Identified Disclosure Gaps");
            if (gaps.isEmpty()) {
                layout.write(regular, BODY_SIZE, BODY_LEADING,
                        "No not-covered or partially covered gaps were identified in this analysis.");
            } else {
                for (RequirementAssessmentResponse item : gaps) {
                    writeGapItem(layout, regular, bold, item);
                }
            }
            layout.blank();

            layout.write(bold, SECTION_SIZE, SECTION_LEADING, "4. Human Review Required");
            if (review.isEmpty()) {
                layout.write(regular, BODY_SIZE, BODY_LEADING,
                        "No requirements are flagged for human review in this analysis.");
            } else {
                for (RequirementAssessmentResponse item : review) {
                    layout.write(bold, BODY_SIZE, BODY_LEADING,
                            item.requirementCode() + "  " + nullSafe(item.requirementTitle()));
                    if (hasText(item.explanation())) {
                        layout.writeWrapped(regular, BODY_SIZE, BODY_LEADING, "Explanation: " + item.explanation());
                    }
                    if (hasText(item.gap())) {
                        layout.writeWrapped(regular, BODY_SIZE, BODY_LEADING, "Gap: " + item.gap());
                    }
                    layout.blank();
                }
            }

            layout.write(bold, SECTION_SIZE, SECTION_LEADING, "5. Covered Requirements");
            if (covered.isEmpty()) {
                layout.write(regular, BODY_SIZE, BODY_LEADING,
                        "No requirements are marked as covered in this analysis.");
            } else {
                for (RequirementAssessmentResponse item : covered) {
                    layout.write(bold, BODY_SIZE, BODY_LEADING,
                            item.requirementCode() + "  " + nullSafe(item.requirementTitle()));
                    layout.write(regular, BODY_SIZE, BODY_LEADING, "Status: " + formatAssessmentStatus(item.assessmentStatus()));
                    layout.writeWrapped(regular, BODY_SIZE, BODY_LEADING, "Evidence: " + formatEvidenceCitations(item));
                    layout.blank();
                }
            }

            layout.write(bold, SECTION_SIZE, SECTION_LEADING, "6. Disclaimer");
            layout.writeWrapped(regular, BODY_SIZE, BODY_LEADING, REVIEW_DISCLAIMER);
            layout.blank();
            layout.writeWrapped(regular, BODY_SIZE, BODY_LEADING, PROTOTYPE_DISCLAIMER);

            layout.close();
            pdf.save(output);
            return output.toByteArray();
        }
    }

    private void writeGapItem(
            PdfLayout layout,
            PDFont regular,
            PDFont bold,
            RequirementAssessmentResponse item) throws IOException {
        layout.write(bold, BODY_SIZE, BODY_LEADING,
                item.requirementCode() + "  " + nullSafe(item.requirementTitle()));
        layout.write(regular, BODY_SIZE, BODY_LEADING,
                formatCategory(item.category()) + "  |  " + formatAssessmentStatus(item.assessmentStatus()));
        if (hasText(item.gap())) {
            layout.writeWrapped(regular, BODY_SIZE, BODY_LEADING, "Gap: " + item.gap());
        }
        if (hasText(item.recommendation())) {
            layout.writeWrapped(regular, BODY_SIZE, BODY_LEADING, "Recommendation: " + item.recommendation());
        }
        layout.blank();
    }

    private static String executiveSummary(
            ComplianceAnalysisResponse analysis,
            int totalAssessed,
            StatusCounts summary) {
        StringBuilder text = new StringBuilder();
        text.append("This gap assessment report is generated from compliance analysis #")
                .append(analysis.id())
                .append(" against ")
                .append(nullSafe(analysis.frameworkName()))
                .append(" (")
                .append(nullSafe(analysis.frameworkCode()))
                .append(") using document #")
                .append(analysis.documentId())
                .append(".");
        if (totalAssessed > 0) {
            text.append(" Of ")
                    .append(totalAssessed)
                    .append(" requirement assessments, ")
                    .append(summary.covered)
                    .append(" are covered, ")
                    .append(summary.partiallyCovered)
                    .append(" are partially covered, ")
                    .append(summary.notCovered)
                    .append(" are not covered, and ")
                    .append(summary.humanReviewRequired)
                    .append(" require human review.");
        } else {
            text.append(" No requirement assessments are available for this analysis.");
        }
        text.append(" This assessment is AI-assisted and intended to support, not replace, review by qualified compliance professionals.");
        return text.toString();
    }

    static String formatEvidenceSourceLabel(EvidenceChunkResponse chunk) {
        if (chunk.pageNumber() != null) {
            return "Page " + chunk.pageNumber();
        }
        return "Source chunk " + chunk.chunkIndex();
    }

    private static String formatEvidenceCitations(RequirementAssessmentResponse item) {
        List<EvidenceChunkResponse> chunks =
                item.evidenceChunks() == null ? List.of() : item.evidenceChunks();
        if (!chunks.isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (EvidenceChunkResponse chunk : chunks) {
                labels.add(formatEvidenceSourceLabel(chunk));
            }
            return String.join("; ", labels);
        }
        if (hasText(item.evidenceText())) {
            return "Evidence text is available without a page-numbered citation.";
        }
        return "No evidence citations were stored for this requirement.";
    }

    private static List<RequirementAssessmentResponse> filterStatuses(
            List<RequirementAssessmentResponse> assessments,
            String... statuses) {
        List<String> wanted = List.of(statuses);
        return assessments.stream()
                .filter(item -> item.assessmentStatus() != null && wanted.contains(item.assessmentStatus()))
                .toList();
    }

    private static StatusCounts countStatuses(List<RequirementAssessmentResponse> assessments) {
        StatusCounts counts = new StatusCounts();
        for (RequirementAssessmentResponse item : assessments) {
            if (item.assessmentStatus() == null) {
                continue;
            }
            switch (item.assessmentStatus()) {
                case "COVERED" -> counts.covered++;
                case "PARTIALLY_COVERED" -> counts.partiallyCovered++;
                case "NOT_COVERED" -> counts.notCovered++;
                case "HUMAN_REVIEW_REQUIRED" -> counts.humanReviewRequired++;
                case "EVIDENCE_RETRIEVED" -> counts.evidenceRetrieved++;
                case "NO_EVIDENCE_FOUND" -> counts.noEvidenceFound++;
                default -> {
                }
            }
        }
        return counts;
    }

    private static String reportingYearSuffix(Integer reportingYear) {
        if (reportingYear == null) {
            return "";
        }
        return "  |  Reporting year: " + reportingYear;
    }

    private static String formatInstant(Instant instant) {
        return instant == null ? "—" : instant.toString();
    }

    private static String formatAnalysisStatus(String status) {
        return humanize(status);
    }

    private static String formatAssessmentStatus(String status) {
        return humanize(status);
    }

    private static String formatCategory(String category) {
        return humanize(category);
    }

    private static String humanize(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        String[] parts = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (i > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                out.append(parts[i].substring(1));
            }
        }
        return out.toString();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String nullSafe(String value) {
        return value == null ? "—" : value;
    }

    private static final class StatusCounts {
        int covered;
        int partiallyCovered;
        int notCovered;
        int humanReviewRequired;
        int evidenceRetrieved;
        int noEvidenceFound;
    }

    private static final class PdfLayout implements AutoCloseable {
        private final PDDocument document;
        private final PDFont fallbackFont;
        private PDPageContentStream stream;
        private float y;
        private final float width;
        private final float top;
        private final float bottom;

        private PdfLayout(PDDocument document, PDFont regular, PDFont bold) throws IOException {
            this.document = document;
            this.fallbackFont = regular;
            PDRectangle media = PDRectangle.LETTER;
            this.width = media.getWidth() - (MARGIN * 2);
            this.top = media.getHeight() - MARGIN;
            this.bottom = MARGIN;
            newPage();
            Objects.requireNonNull(bold);
        }

        private void newPage() throws IOException {
            closeStream();
            PDPage nextPage = new PDPage(PDRectangle.LETTER);
            document.addPage(nextPage);
            stream = new PDPageContentStream(document, nextPage);
            y = top;
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < bottom) {
                newPage();
            }
        }

        void blank() throws IOException {
            ensureSpace(BODY_LEADING);
            y -= BODY_LEADING / 2;
        }

        void write(PDFont font, float size, float leading, String text) throws IOException {
            writeWrapped(font, size, leading, text);
        }

        void writeWrapped(PDFont font, float size, float leading, String text) throws IOException {
            String sanitized = sanitize(text);
            if (sanitized.isBlank()) {
                return;
            }
            for (String paragraph : sanitized.split("\n", -1)) {
                List<String> lines = wrapLine(paragraph, font, size, width);
                for (String line : lines) {
                    ensureSpace(leading);
                    stream.beginText();
                    stream.setFont(font, size);
                    stream.newLineAtOffset(MARGIN, y);
                    stream.showText(line.isEmpty() ? " " : line);
                    stream.endText();
                    y -= leading;
                }
            }
        }

        private void closeStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        @Override
        public void close() throws IOException {
            closeStream();
        }

        private List<String> wrapLine(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            if (text.isEmpty()) {
                lines.add("");
                return lines;
            }
            String[] words = text.split(" ");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (stringWidth(font, fontSize, candidate) <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                if (stringWidth(font, fontSize, word) <= maxWidth) {
                    current.append(word);
                } else {
                    lines.addAll(splitLongToken(word, font, fontSize, maxWidth));
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            if (lines.isEmpty()) {
                lines.add("");
            }
            return lines;
        }

        private List<String> splitLongToken(String token, PDFont font, float fontSize, float maxWidth)
                throws IOException {
            List<String> parts = new ArrayList<>();
            StringBuilder chunk = new StringBuilder();
            for (int i = 0; i < token.length(); i++) {
                String candidate = chunk.toString() + token.charAt(i);
                if (stringWidth(font, fontSize, candidate) <= maxWidth) {
                    chunk.append(token.charAt(i));
                } else {
                    if (!chunk.isEmpty()) {
                        parts.add(chunk.toString());
                        chunk.setLength(0);
                    }
                    chunk.append(token.charAt(i));
                }
            }
            if (!chunk.isEmpty()) {
                parts.add(chunk.toString());
            }
            return parts;
        }

        private float stringWidth(PDFont font, float fontSize, String text) throws IOException {
            PDFont used = font != null ? font : fallbackFont;
            return used.getStringWidth(text) / 1000f * fontSize;
        }
    }

    static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\t', ' ')
                .replace('\u00a0', ' ')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace("\u2026", "...");
        StringBuilder out = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == '\n' || (c >= 32 && c <= 126)) {
                out.append(c);
            }
        }
        return out.toString();
    }
}
