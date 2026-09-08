package dev.esgenius.dto;

import java.util.List;

/**
 * Complete ESG profile for a single company.
 */
public record CompanyEsgProfileResponse(
        Long id,
        String name,
        String ticker,
        String industry,

        // Current ESG rating
        EsgRatingResponse currentRating,

        // Rating history (for trend chart)
        List<EsgRatingHistoryResponse> ratingHistory,

        // Material ESG issues and scores
        List<KeyIssueAssessmentResponse> materialIssues,

        // Recent ESG events
        List<EsgEventResponse> recentEvents) {
}
