package dev.esgenius.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Extracts machine-readable text from PDF files using Apache PDFBox.
 * OCR is not supported in this phase.
 */
@Service
public class PdfTextExtractionService {

    public PdfExtractionResult extract(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document).trim();
            int pageCount = document.getNumberOfPages();
            return new PdfExtractionResult(text, pageCount);
        }
    }
}
