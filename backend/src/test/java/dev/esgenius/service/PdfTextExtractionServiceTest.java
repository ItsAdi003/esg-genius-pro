package dev.esgenius.service;

import dev.esgenius.config.PdfLimitsProperties;
import dev.esgenius.support.TestPdfFixtures;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfTextExtractionServiceTest {

    private final PdfTextExtractionService pdfTextExtractionService =
            new PdfTextExtractionService(new PdfLimitsProperties());

    @Test
    void extractsKnownTextFromGeneratedPdf(@TempDir Path tempDir) throws Exception {
        Path pdfPath = TestPdfFixtures.writeSamplePdf(tempDir);

        ExtractedPdf result = pdfTextExtractionService.extract(pdfPath);

        assertEquals(1, result.pageCount());
        assertTrue(result.fullText().contains(TestPdfFixtures.LINE_ONE));
        assertTrue(result.fullText().contains(TestPdfFixtures.LINE_TWO));
        assertEquals(1, result.pages().size());
        assertEquals(1, result.pages().get(0).pageNumber());
        assertTrue(result.pages().get(0).text().contains(TestPdfFixtures.LINE_ONE));
    }

    @Test
    void pageNumbersAreOneBased(@TempDir Path tempDir) throws Exception {
        Path pdfPath = TestPdfFixtures.writeMultiPagePdf(tempDir);

        ExtractedPdf result = pdfTextExtractionService.extract(pdfPath);

        assertEquals(2, result.pages().size());
        assertEquals(1, result.pages().get(0).pageNumber());
        assertEquals(2, result.pages().get(1).pageNumber());
    }

    @Test
    void fullTextStillProducedForMultiPagePdf(@TempDir Path tempDir) throws Exception {
        Path pdfPath = TestPdfFixtures.writeMultiPagePdf(tempDir);

        ExtractedPdf result = pdfTextExtractionService.extract(pdfPath);

        assertFalse(result.fullText().isBlank());
        assertTrue(result.fullText().contains(TestPdfFixtures.LINE_ONE));
        assertTrue(result.fullText().contains(TestPdfFixtures.PAGE_TWO_LINE));
    }

    @Test
    void rejectsPdfOverPageLimitBeforeReturningText(@TempDir Path tempDir) throws Exception {
        Path pdfPath = tempDir.resolve("too-many-pages.pdf");
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < 401; i++) {
                document.addPage(new PDPage());
            }
            document.save(pdfPath.toFile());
        }

        PdfExtractionLimitException ex = assertThrows(PdfExtractionLimitException.class,
                () -> pdfTextExtractionService.extract(pdfPath));

        assertEquals("Document has 401 pages; the limit is 400.", ex.getMessage());
    }

    @Test
    void rejectsPdfWhenExtractedCharactersExceedLimit(@TempDir Path tempDir) throws Exception {
        PdfLimitsProperties limits = new PdfLimitsProperties();
        limits.setMaxExtractedChars(40);
        PdfTextExtractionService limited = new PdfTextExtractionService(limits);

        Path pdfPath = tempDir.resolve("too-much-text.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("A".repeat(80));
                contentStream.endText();
            }
            document.save(pdfPath.toFile());
        }

        PdfExtractionLimitException ex = assertThrows(PdfExtractionLimitException.class,
                () -> limited.extract(pdfPath));

        assertTrue(ex.getMessage().startsWith("Extracted text is "));
        assertTrue(ex.getMessage().contains("the limit is 40."));
        assertFalse(ex.getMessage().contains("A".repeat(20)));
        assertTrue(Files.size(pdfPath) > 0);
    }
}
