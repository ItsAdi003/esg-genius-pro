package dev.esgenius.service;

import dev.esgenius.support.TestPdfFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfTextExtractionServiceTest {

    private final PdfTextExtractionService pdfTextExtractionService = new PdfTextExtractionService();

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
}
