package dev.esgenius.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bounds for PDF text extraction. Defaults live here; {@code PDF_MAX_PAGES} and
 * {@code PDF_MAX_EXTRACTED_CHARS} are bound from configuration elsewhere.
 */
@Component
@ConfigurationProperties(prefix = "app.pdf")
public class PdfLimitsProperties {

    private int maxPages = 400;
    private int maxExtractedChars = 3_000_000;

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getMaxExtractedChars() {
        return maxExtractedChars;
    }

    public void setMaxExtractedChars(int maxExtractedChars) {
        this.maxExtractedChars = maxExtractedChars;
    }
}
