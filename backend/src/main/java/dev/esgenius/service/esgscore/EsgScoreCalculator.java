package dev.esgenius.service.esgscore;

import dev.esgenius.dto.EsgScoreResponse.KeyIssueScore;
import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.entity.EsgIndustry;
import dev.esgenius.entity.EsgPillar;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, model-free scoring that follows the structure of the MSCI ESG Ratings model:
 *
 * <pre>
 *   requirement points (0-10)  ->  Key Issue Score (0-10)
 *   Key Issue Scores x weights ->  Weighted Average Key Issue Score (WAKIS)
 *   WAKIS, industry min/max    ->  Industry-Adjusted Score = (WAKIS - min) / (max - min) x 10
 *   Industry-Adjusted Score    ->  rating band CCC ... AAA (seven equal bands of 10/7)
 * </pre>
 *
 * Requirements that are human-review or not applicable are left out, so a company is neither rewarded nor
 * penalised for them.
 */
public final class EsgScoreCalculator {

    public static final double POINTS_COVERED = 10.0;
    public static final double POINTS_PARTIAL = 5.0;
    public static final double POINTS_NOT_COVERED = 0.0;
    /** Leadership indicators are voluntary in the BRSR format, so they count half as much as the rest. */
    public static final double LEADERSHIP_WEIGHT = 0.5;
    /** Fewest same-industry analyses needed before the min/max normalisation is applied. */
    public static final int MIN_PEERS_FOR_ADJUSTMENT = 3;

    private static final String[] BANDS = {"CCC", "B", "BB", "BBB", "A", "AA", "AAA"};
    private static final double BAND_WIDTH = 10.0 / BANDS.length;

    private EsgScoreCalculator() {
    }

    /** Number of requirements in one (section, indicator type, status) group. */
    public record ScoreInput(String section, String indicatorType, AssessmentStatus status, int count) {
    }

    public record Result(
            Double wakis,
            Map<EsgPillar, Double> pillarScores,
            Map<EsgPillar, Double> pillarWeights,
            List<KeyIssueScore> keyIssues,
            int scored,
            int notApplicable,
            int humanReview,
            int total) {
        public boolean scorable() {
            return wakis != null;
        }
    }

    private static final class IssueTotals {
        double weightedPoints;
        double weight;
        int scored;
        int notApplicable;
        int humanReview;
        int total;

        Double score() {
            return weight > 0 ? weightedPoints / weight : null;
        }
    }

    public static Result calculate(EsgIndustry industry, List<ScoreInput> inputs) {
        Map<BrsrKeyIssue, IssueTotals> totals = new EnumMap<>(BrsrKeyIssue.class);
        for (BrsrKeyIssue issue : BrsrKeyIssue.values()) {
            totals.put(issue, new IssueTotals());
        }
        for (ScoreInput input : inputs) {
            BrsrKeyIssue issue = BrsrKeyIssue.forSection(input.section()).orElse(null);
            if (issue == null) {
                continue;
            }
            IssueTotals t = totals.get(issue);
            t.total += input.count();
            switch (input.status()) {
                case NOT_APPLICABLE -> t.notApplicable += input.count();
                case HUMAN_REVIEW_REQUIRED -> t.humanReview += input.count();
                case COVERED, PARTIALLY_COVERED, NOT_COVERED -> {
                    double weight = "LEADERSHIP".equals(input.indicatorType()) ? LEADERSHIP_WEIGHT : 1.0;
                    t.scored += input.count();
                    t.weight += weight * input.count();
                    t.weightedPoints += weight * input.count() * points(input.status());
                }
                default -> {
                    // Legacy retrieval-stage statuses carry no judgement and are not scored.
                }
            }
        }

        Map<EsgPillar, Integer> scoredIssuesPerPillar = new EnumMap<>(EsgPillar.class);
        for (BrsrKeyIssue issue : BrsrKeyIssue.values()) {
            if (totals.get(issue).score() != null) {
                scoredIssuesPerPillar.merge(issue.getPillar(), 1, Integer::sum);
            }
        }

        Map<EsgPillar, Double> pillarWeights = activePillarWeights(industry, scoredIssuesPerPillar);
        Map<EsgPillar, Double> pillarScoreSum = new EnumMap<>(EsgPillar.class);
        double wakis = 0.0;
        boolean anyScored = false;
        List<KeyIssueScore> details = new ArrayList<>();
        int scored = 0;
        int notApplicable = 0;
        int humanReview = 0;
        int total = 0;

        for (BrsrKeyIssue issue : BrsrKeyIssue.values()) {
            IssueTotals t = totals.get(issue);
            Double score = t.score();
            Double weight = null;
            if (score != null) {
                weight = pillarWeights.get(issue.getPillar()) / scoredIssuesPerPillar.get(issue.getPillar());
                wakis += weight * score;
                anyScored = true;
                pillarScoreSum.merge(issue.getPillar(), score, Double::sum);
            }
            scored += t.scored;
            notApplicable += t.notApplicable;
            humanReview += t.humanReview;
            total += t.total;
            details.add(new KeyIssueScore(
                    issue.name(), issue.getLabel(), issue.getPillar().name(), issue.getSections(),
                    weight, score, t.scored, t.notApplicable, t.humanReview, t.total));
        }

        Map<EsgPillar, Double> pillarScores = new EnumMap<>(EsgPillar.class);
        pillarScoreSum.forEach((pillar, sum) -> pillarScores.put(pillar, sum / scoredIssuesPerPillar.get(pillar)));

        return new Result(anyScored ? wakis : null, pillarScores, pillarWeights, details,
                scored, notApplicable, humanReview, total);
    }

    /** Pillar weights restricted to pillars that have at least one scored issue, renormalised to 1. */
    private static Map<EsgPillar, Double> activePillarWeights(
            EsgIndustry industry, Map<EsgPillar, Integer> scoredIssuesPerPillar) {
        Map<EsgPillar, Double> base = new EnumMap<>(EsgPillar.class);
        base.put(EsgPillar.ENVIRONMENTAL, industry != null ? industry.getEnvironmentalWeight() : 1.0 / 3);
        base.put(EsgPillar.SOCIAL, industry != null ? industry.getSocialWeight() : 1.0 / 3);
        base.put(EsgPillar.GOVERNANCE, industry != null ? industry.getGovernanceWeight() : 1.0 / 3);

        double activeSum = 0.0;
        for (EsgPillar pillar : EsgPillar.values()) {
            if (scoredIssuesPerPillar.containsKey(pillar)) {
                activeSum += base.get(pillar);
            }
        }
        Map<EsgPillar, Double> weights = new EnumMap<>(EsgPillar.class);
        for (EsgPillar pillar : EsgPillar.values()) {
            weights.put(pillar, scoredIssuesPerPillar.containsKey(pillar) && activeSum > 0
                    ? base.get(pillar) / activeSum
                    : 0.0);
        }
        return weights;
    }

    private static double points(AssessmentStatus status) {
        return switch (status) {
            case COVERED -> POINTS_COVERED;
            case PARTIALLY_COVERED -> POINTS_PARTIAL;
            default -> POINTS_NOT_COVERED;
        };
    }

    /** Min/max normalisation of the WAKIS against same-industry analyses, clamped to 0-10. */
    public static double industryAdjusted(double wakis, double minimum, double maximum) {
        double adjusted = (wakis - minimum) / (maximum - minimum) * 10.0;
        return Math.max(0.0, Math.min(10.0, adjusted));
    }

    /** Seven equal bands of width 10/7 (1.429): [0, 1.429) is CCC ... [8.571, 10] is AAA. */
    public static String band(double industryAdjustedScore) {
        int index = (int) Math.floor((industryAdjustedScore + 1e-9) / BAND_WIDTH);
        return BANDS[Math.max(0, Math.min(BANDS.length - 1, index))];
    }
}
