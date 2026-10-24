package dev.esgenius.service.esgscore;

import dev.esgenius.dto.EsgScoreResponse.KeyIssueScore;
import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.entity.EsgIndustry;
import dev.esgenius.entity.EsgPillar;
import dev.esgenius.service.esgscore.EsgScoreCalculator.Result;
import dev.esgenius.service.esgscore.EsgScoreCalculator.ScoreInput;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EsgScoreCalculatorTest {

    private static final String[] ALL_SECTIONS = {"A", "B", "P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9"};

    private static ScoreInput in(String section, AssessmentStatus status, int count) {
        return new ScoreInput(section, "ESSENTIAL", status, count);
    }

    private static List<ScoreInput> everySection(AssessmentStatus status) {
        List<ScoreInput> inputs = new ArrayList<>();
        for (String section : ALL_SECTIONS) {
            inputs.add(in(section, status, 4));
        }
        return inputs;
    }

    private static KeyIssueScore issue(Result result, BrsrKeyIssue issue) {
        return result.keyIssues().stream().filter(k -> k.code().equals(issue.name())).findFirst().orElseThrow();
    }

    @Test
    void fullyCoveredReportScoresTen() {
        Result result = EsgScoreCalculator.calculate(EsgIndustry.ENERGY, everySection(AssessmentStatus.COVERED));

        assertThat(result.wakis()).isCloseTo(10.0, within(1e-9));
        assertThat(result.pillarScores().get(EsgPillar.ENVIRONMENTAL)).isCloseTo(10.0, within(1e-9));
    }

    @Test
    void nothingCoveredScoresZero() {
        Result result = EsgScoreCalculator.calculate(null, everySection(AssessmentStatus.NOT_COVERED));

        assertThat(result.wakis()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void partialCoverageEarnsHalfThePoints() {
        Result result = EsgScoreCalculator.calculate(null, everySection(AssessmentStatus.PARTIALLY_COVERED));

        assertThat(result.wakis()).isCloseTo(5.0, within(1e-9));
    }

    @Test
    void keyIssueWeightsSumToOne() {
        Result result = EsgScoreCalculator.calculate(EsgIndustry.HEALTH_CARE, everySection(AssessmentStatus.COVERED));

        double sum = result.keyIssues().stream().mapToDouble(k -> k.weight() == null ? 0 : k.weight()).sum();
        assertThat(sum).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void industryProfileShiftsWeightTowardsTheMaterialPillar() {
        List<ScoreInput> inputs = new ArrayList<>(everySection(AssessmentStatus.NOT_COVERED));
        // Fully covered environmental sections only.
        inputs.removeIf(i -> i.section().equals("P2") || i.section().equals("P6"));
        inputs.add(in("P2", AssessmentStatus.COVERED, 4));
        inputs.add(in("P6", AssessmentStatus.COVERED, 4));

        double energy = EsgScoreCalculator.calculate(EsgIndustry.ENERGY, inputs).wakis();
        double software = EsgScoreCalculator.calculate(EsgIndustry.INFORMATION_TECHNOLOGY, inputs).wakis();

        assertThat(energy).isCloseTo(4.5, within(1e-9));
        assertThat(software).isCloseTo(1.5, within(1e-9));
    }

    @Test
    void everyIndustryProfileSumsToOneAndKeepsGovernanceAtOrAboveAThird() {
        for (EsgIndustry industry : EsgIndustry.values()) {
            double sum = industry.getEnvironmentalWeight() + industry.getSocialWeight() + industry.getGovernanceWeight();
            assertThat(sum).as(industry.name()).isCloseTo(1.0, within(1e-9));
            assertThat(industry.getGovernanceWeight()).as(industry.name()).isGreaterThanOrEqualTo(0.33);
        }
    }

    @Test
    void notApplicableAndHumanReviewItemsDoNotChangeTheScore() {
        List<ScoreInput> base = new ArrayList<>(everySection(AssessmentStatus.COVERED));
        double before = EsgScoreCalculator.calculate(EsgIndustry.MATERIALS, base).wakis();

        List<ScoreInput> withExtras = new ArrayList<>(base);
        withExtras.add(in("P6", AssessmentStatus.NOT_APPLICABLE, 6));
        withExtras.add(in("P3", AssessmentStatus.HUMAN_REVIEW_REQUIRED, 3));
        Result after = EsgScoreCalculator.calculate(EsgIndustry.MATERIALS, withExtras);

        assertThat(after.wakis()).isCloseTo(before, within(1e-9));
        assertThat(after.notApplicable()).isEqualTo(6);
        assertThat(after.humanReview()).isEqualTo(3);
        assertThat(issue(after, BrsrKeyIssue.ENVIRONMENTAL_FOOTPRINT).notApplicableRequirements()).isEqualTo(6);
    }

    @Test
    void markingMissingItemsNotApplicableRaisesTheScoreInsteadOfPenalisingTheCompany() {
        List<ScoreInput> asMissing = new ArrayList<>(everySection(AssessmentStatus.COVERED));
        asMissing.removeIf(i -> i.section().equals("P6"));
        asMissing.add(in("P6", AssessmentStatus.COVERED, 2));
        asMissing.add(in("P6", AssessmentStatus.NOT_COVERED, 2));

        List<ScoreInput> asNotApplicable = new ArrayList<>(asMissing);
        asNotApplicable.removeIf(i -> i.status() == AssessmentStatus.NOT_COVERED);
        asNotApplicable.add(in("P6", AssessmentStatus.NOT_APPLICABLE, 2));

        double penalised = EsgScoreCalculator.calculate(null, asMissing).wakis();
        double excluded = EsgScoreCalculator.calculate(null, asNotApplicable).wakis();

        assertThat(excluded).isGreaterThan(penalised);
        assertThat(excluded).isCloseTo(10.0, within(1e-9));
    }

    @Test
    void leadershipIndicatorsCountHalfAsMuchAsEssentialOnes() {
        List<ScoreInput> inputs = List.of(
                new ScoreInput("P6", "ESSENTIAL", AssessmentStatus.COVERED, 1),
                new ScoreInput("P6", "LEADERSHIP", AssessmentStatus.NOT_COVERED, 1));

        Result result = EsgScoreCalculator.calculate(null, inputs);

        // (1.0 * 10 + 0.5 * 0) / 1.5
        assertThat(issue(result, BrsrKeyIssue.ENVIRONMENTAL_FOOTPRINT).score()).isCloseTo(6.6667, within(1e-3));
    }

    @Test
    void issueWithNothingToScoreDropsOutAndOthersAreRenormalised() {
        List<ScoreInput> inputs = List.of(
                in("P6", AssessmentStatus.COVERED, 3),
                in("P3", AssessmentStatus.HUMAN_REVIEW_REQUIRED, 5));

        Result result = EsgScoreCalculator.calculate(EsgIndustry.ENERGY, inputs);

        assertThat(issue(result, BrsrKeyIssue.HUMAN_CAPITAL).score()).isNull();
        assertThat(issue(result, BrsrKeyIssue.HUMAN_CAPITAL).weight()).isNull();
        assertThat(issue(result, BrsrKeyIssue.ENVIRONMENTAL_FOOTPRINT).weight()).isCloseTo(1.0, within(1e-9));
        assertThat(result.wakis()).isCloseTo(10.0, within(1e-9));
    }

    @Test
    void noScorableRequirementMeansNoScore() {
        Result result = EsgScoreCalculator.calculate(
                EsgIndustry.ENERGY, everySection(AssessmentStatus.HUMAN_REVIEW_REQUIRED));

        assertThat(result.scorable()).isFalse();
        assertThat(result.wakis()).isNull();
    }

    @Test
    void legacyRequirementsWithoutASectionAreIgnored() {
        Result result = EsgScoreCalculator.calculate(
                null, List.of(new ScoreInput(null, null, AssessmentStatus.COVERED, 14)));

        assertThat(result.scorable()).isFalse();
        assertThat(result.total()).isZero();
    }

    @Test
    void industryAdjustedScoreNormalisesAgainstPeerMinAndMax() {
        assertThat(EsgScoreCalculator.industryAdjusted(6.0, 4.0, 8.0)).isCloseTo(5.0, within(1e-9));
        assertThat(EsgScoreCalculator.industryAdjusted(4.0, 4.0, 8.0)).isCloseTo(0.0, within(1e-9));
        assertThat(EsgScoreCalculator.industryAdjusted(8.0, 4.0, 8.0)).isCloseTo(10.0, within(1e-9));
        assertThat(EsgScoreCalculator.industryAdjusted(9.0, 4.0, 8.0)).isCloseTo(10.0, within(1e-9));
    }

    @Test
    void ratingBandsUseSevenEqualWidthsFromCccToAaa() {
        assertThat(EsgScoreCalculator.band(0.0)).isEqualTo("CCC");
        assertThat(EsgScoreCalculator.band(1.42)).isEqualTo("CCC");
        assertThat(EsgScoreCalculator.band(1.43)).isEqualTo("B");
        assertThat(EsgScoreCalculator.band(2.86)).isEqualTo("BB");
        assertThat(EsgScoreCalculator.band(4.29)).isEqualTo("BBB");
        assertThat(EsgScoreCalculator.band(5.72)).isEqualTo("A");
        assertThat(EsgScoreCalculator.band(7.15)).isEqualTo("AA");
        assertThat(EsgScoreCalculator.band(8.58)).isEqualTo("AAA");
        assertThat(EsgScoreCalculator.band(10.0)).isEqualTo("AAA");
    }

    @Test
    void everyBrsrSectionFeedsExactlyOneKeyIssue() {
        for (String section : ALL_SECTIONS) {
            assertThat(BrsrKeyIssue.forSection(section)).as(section).isPresent();
        }
        assertThat(BrsrKeyIssue.forSection("C")).isEmpty();
        assertThat(BrsrKeyIssue.forSection(null)).isEmpty();
    }
}
