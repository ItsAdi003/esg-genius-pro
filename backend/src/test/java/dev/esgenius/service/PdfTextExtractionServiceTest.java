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

        PdfExtractionResult result = pdfTextExtractionService.extract(pdfPath);

        assertEquals(1, result.pageCount());
        assertTrue(result.text().contains(TestPdfFixtures.LINE_ONE));
        assertTrue(result.text().contains(TestPdfFixtures.LINE_TWO));
    }
}
