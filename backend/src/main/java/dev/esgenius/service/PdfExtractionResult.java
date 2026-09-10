package dev.esgenius.service;

/**
 * Result of PDF text extraction.
 */
public record PdfExtractionResult(String text, int pageCount) {
}
