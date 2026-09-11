package dev.esgenius.service;

/**
 * Text extracted from a single PDF page (1-based page number).
 */
public record ExtractedPdfPage(int pageNumber, String text) {
}
