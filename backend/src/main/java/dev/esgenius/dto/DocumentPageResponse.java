package dev.esgenius.dto;

/**
 * Per-page extracted text for document detail views.
 */
public record DocumentPageResponse(int pageNumber, String text) {
}
