package dev.esgenius.dto;

/**
 * ESG rating snapshot response.
 */
public record EsgRatingResponse(
        Double overallScore,
        Double environmentalScore,
        Double socialScore,
        Double governanceScore,
        String ratingBand,
        String assessmentDate) {
}
