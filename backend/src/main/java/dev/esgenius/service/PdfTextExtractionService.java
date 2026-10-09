package dev.esgenius.service;

import dev.esgenius.config.PdfLimitsProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts machine-readable text from PDF files using Apache PDFBox.
 * OCR is not supported in this phase.
 */
@Service
public class PdfTextExtractionService {

    private final PdfLimitsProperties pdfLimits;

    public PdfTextExtractionService(PdfLimitsProperties pdfLimits) {
        this.pdfLimits = pdfLimits;
    }

    public ExtractedPdf extract(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile(), IOUtils.createTempFileOnlyStreamCache())) {
            int pageCount = document.getNumberOfPages();
            if (pageCount > pdfLimits.getMaxPages()) {
                throw new PdfExtractionLimitException(
                        "Document has " + pageCount + " pages; the limit is " + pdfLimits.getMaxPages() + ".");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            List<ExtractedPdfPage> pages = new ArrayList<>(pageCount);
            long cumulativeChars = 0;
            int maxChars = pdfLimits.getMaxExtractedChars();

            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String pageText = stripper.getText(document).trim();
                cumulativeChars += pageText.length();
                if (cumulativeChars > maxChars) {
                    throw new PdfExtractionLimitException(
                            "Extracted text is " + cumulativeChars + " characters; the limit is " + maxChars + ".");
                }
                pages.add(new ExtractedPdfPage(pageNumber, pageText));
            }

            String fullText = pages.stream()
                    .map(ExtractedPdfPage::text)
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining("\n"))
                    .trim();
            return new ExtractedPdf(pageCount, fullText, pages);
        }
    }
}
