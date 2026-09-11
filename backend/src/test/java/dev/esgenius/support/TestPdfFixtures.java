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
    public static final String PAGE_TWO_LINE = "Page two disclosure about Scope 1 emissions from owned facilities.";

    private TestPdfFixtures() {
    }

    public static byte[] createSamplePdfBytes() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            writePageText(document, page, LINE_ONE, LINE_TWO);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    public static byte[] createMultiPagePdfBytes() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage pageOne = new PDPage();
            document.addPage(pageOne);
            writePageText(document, pageOne, LINE_ONE, LINE_TWO);

            PDPage pageTwo = new PDPage();
            document.addPage(pageTwo);
            writePageText(document, pageTwo, PAGE_TWO_LINE, "Additional context on page two.");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static void writePageText(PDDocument document, PDPage page, String lineOne, String lineTwo)
            throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.newLineAtOffset(50, 700);
            contentStream.showText(lineOne);
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText(lineTwo);
            contentStream.endText();
        }
    }

    public static Path writeSamplePdf(Path directory) throws IOException {
        Path pdfPath = directory.resolve("sample.pdf");
        Files.write(pdfPath, createSamplePdfBytes());
        return pdfPath;
    }

    public static Path writeMultiPagePdf(Path directory) throws IOException {
        Path pdfPath = directory.resolve("multi-page.pdf");
        Files.write(pdfPath, createMultiPagePdfBytes());
        return pdfPath;
    }
}
