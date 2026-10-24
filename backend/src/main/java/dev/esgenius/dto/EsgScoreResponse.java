package dev.esgenius.dto;

import java.util.List;

/**
 * MSCI-style ESG score derived from a completed BRSR analysis. It measures the quality of disclosure,
 * not real-world ESG performance, and is not an official MSCI rating.
 */
public record EsgScoreResponse(
        Long analysisId,
        /** EsgIndustry name, or null when the reviewer has not chosen a sector. */
        String industry,
        String industryLabel,
        /** False when no requirement could be scored (every item is human review or not applicable). */
        boolean scorable,
        /** Weighted Average Key Issue Score, 0-10. */
        Double weightedAverageKeyIssueScore,
        /** Industry-Adjusted Score, 0-10; equals the WAKIS when industryAdjusted is false. */
        Double industryAdjustedScore,
        boolean industryAdjusted,
        int peerCount,
        Double peerMinimum,
        Double peerMaximum,
        /** AAA to CCC, derived from the industry-adjusted score. */
        String ratingBand,
        Double environmentalScore,
        Double socialScore,
        Double governanceScore,
        Double environmentalWeight,
        Double socialWeight,
        Double governanceWeight,
        int scoredRequirements,
        int notApplicableRequirements,
        int humanReviewRequirements,
        int totalRequirements,
        List<KeyIssueScore> keyIssues,
        String note) {

    public record KeyIssueScore(
            String code,
            String name,
            String pillar,
            String sections,
            /** Share of the WAKIS carried by this issue; null when the issue could not be scored. */
            Double weight,
            Double score,
            int scoredRequirements,
            int notApplicableRequirements,
            int humanReviewRequirements,
            int totalRequirements) {
    }
}
