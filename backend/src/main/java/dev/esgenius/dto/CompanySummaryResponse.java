package dev.esgenius.dto;

/**
 * Minimal company summary for listing/selection.
 */
public record CompanySummaryResponse(
        Long id,
        String name,
        String ticker,
        String industry) {
}
