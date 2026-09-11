package dev.esgenius.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts machine-readable text from PDF files using Apache PDFBox.
 * OCR is not supported in this phase.
 */
@Service
public class PdfTextExtractionService {

    public ExtractedPdf extract(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            int pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();

            String fullText = stripper.getText(document).trim();

            List<ExtractedPdfPage> pages = new ArrayList<>(pageCount);
            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String pageText = stripper.getText(document).trim();
                pages.add(new ExtractedPdfPage(pageNumber, pageText));
            }

            return new ExtractedPdf(pageCount, fullText, pages);
        }
    }
}
