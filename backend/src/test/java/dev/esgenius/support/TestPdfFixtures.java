package dev.esgenius.support;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestPdfFixtures {

    public static final String LINE_ONE = "ESGenius PDF extraction test";
    public static final String LINE_TWO = "Carbon emissions decreased during the reporting year.";

    private TestPdfFixtures() {
    }

    public static byte[] createSamplePdfBytes() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(LINE_ONE);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText(LINE_TWO);
                contentStream.endText();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    public static Path writeSamplePdf(Path directory) throws IOException {
        Path pdfPath = directory.resolve("sample.pdf");
        Files.write(pdfPath, createSamplePdfBytes());
        return pdfPath;
    }
}
