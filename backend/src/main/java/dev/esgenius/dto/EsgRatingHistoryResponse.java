package dev.esgenius.dto;

/**
 * Historical rating snapshot for trend charts.
 */
public record EsgRatingHistoryResponse(
        String quarter,
        Double score) {
}
