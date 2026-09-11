package dev.esgenius.service;

import java.util.List;

/**
 * Result of page-aware PDF text extraction.
 */
public record ExtractedPdf(int pageCount, String fullText, List<ExtractedPdfPage> pages) {
}
