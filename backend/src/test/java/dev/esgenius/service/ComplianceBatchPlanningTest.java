package dev.esgenius.service;

import dev.esgenius.entity.EsgCategory;
import dev.esgenius.entity.Framework;
import dev.esgenius.entity.FrameworkRequirement;
import dev.esgenius.service.ComplianceAnalysisService.PendingClassification;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** How requirements are grouped and split into model calls. */
class ComplianceBatchPlanningTest {

    private static PendingClassification pending(String code, String section) {
        FrameworkRequirement requirement = new FrameworkRequirement(
                (Framework) null, code, "Title " + code, EsgCategory.GOVERNANCE, "Description", "Text", true, "v")
                .withPlacement(section, "ESSENTIAL", 1);
        return new PendingClassification(requirement, List.of(new RetrievedChunk(0, "text", 1.0, 1)));
    }

    private static List<PendingClassification> section(String section, int count) {
        List<PendingClassification> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            items.add(pending(section + "-" + i, section));
        }
        return items;
    }

    private static List<Integer> sizes(List<List<PendingClassification>> batches) {
        return batches.stream().map(List::size).toList();
    }

    @Test
    void aSectionWithinTheLimitIsOneCall() {
        assertThat(sizes(ComplianceAnalysisService.planBatches(section("P4", 5), 12))).containsExactly(5);
        assertThat(sizes(ComplianceAnalysisService.planBatches(section("P1", 12), 12))).containsExactly(12);
    }

    @Test
    void aLargeSectionIsSplitEvenlyNotIntoARunt() {
        // 21 over a limit of 12 is 11 + 10, not 12 + 9; 24 is 12 + 12; 25 is 9 + 8 + 8.
        assertThat(sizes(ComplianceAnalysisService.planBatches(section("P6", 21), 12))).containsExactly(11, 10);
        assertThat(sizes(ComplianceAnalysisService.planBatches(section("A", 24), 12))).containsExactly(12, 12);
        assertThat(sizes(ComplianceAnalysisService.planBatches(section("A", 25), 12))).containsExactly(9, 8, 8);
    }

    @Test
    void everyBatchStaysWithinTheLimitAndNothingIsLost() {
        for (int candidate = 1; candidate <= 20; candidate++) {
            final int limit = candidate;
            for (int total = 1; total <= 60; total++) {
                List<List<PendingClassification>> batches =
                        ComplianceAnalysisService.planBatches(section("P3", total), limit);
                assertThat(batches).allSatisfy(batch -> assertThat(batch.size()).isBetween(1, limit));
                assertThat(batches.stream().mapToInt(List::size).sum()).isEqualTo(total);
                int smallest = batches.stream().mapToInt(List::size).min().orElseThrow();
                int largest = batches.stream().mapToInt(List::size).max().orElseThrow();
                assertThat(largest - smallest).isLessThanOrEqualTo(1);
            }
        }
    }

    @Test
    void sectionsNeverShareACallAndKeepFrameworkOrder() {
        List<PendingClassification> all = new ArrayList<>();
        all.addAll(section("A", 3));
        all.addAll(section("B", 2));
        all.addAll(section("P1", 4));

        List<List<PendingClassification>> batches = ComplianceAnalysisService.planBatches(all, 12);

        assertThat(batches).hasSize(3);
        assertThat(batches.get(0)).extracting(p -> p.requirement().getSection()).containsOnly("A");
        assertThat(batches.get(1)).extracting(p -> p.requirement().getSection()).containsOnly("B");
        assertThat(batches.get(2)).extracting(p -> p.requirement().getSection()).containsOnly("P1");
        assertThat(batches.get(2)).extracting(p -> p.requirement().getRequirementCode())
                .containsExactly("P1-1", "P1-2", "P1-3", "P1-4");
    }

    @Test
    void aLimitOfOneGivesOneCallPerRequirement() {
        List<PendingClassification> all = new ArrayList<>();
        all.addAll(section("A", 3));
        all.addAll(section("B", 2));

        assertThat(sizes(ComplianceAnalysisService.planBatches(all, 1))).containsExactly(1, 1, 1, 1, 1);
    }

    @Test
    void requirementsWithoutASectionAreGroupedTogether() {
        List<PendingClassification> legacy = new ArrayList<>();
        legacy.add(pending("ENV-001", null));
        legacy.add(pending("ENV-002", null));

        assertThat(sizes(ComplianceAnalysisService.planBatches(legacy, 12))).containsExactly(2);
    }

    @Test
    void theFullBrsrSetNeedsFifteenCallsAtTheDefaultLimit() {
        // Per section: A 24, B 12, P1 9, P2 9, P3 21, P4 5, P5 15, P6 21, P7 3, P8 10, P9 11.
        int[] sectionSizes = {24, 12, 9, 9, 21, 5, 15, 21, 3, 10, 11};
        List<PendingClassification> all = new ArrayList<>();
        for (int i = 0; i < sectionSizes.length; i++) {
            all.addAll(section("S" + i, sectionSizes[i]));
        }

        List<List<PendingClassification>> batches = ComplianceAnalysisService.planBatches(all, 12);

        assertThat(all).hasSize(140);
        // 140 requirements go to the model in 15 calls instead of 140.
        assertThat(batches).hasSize(15);
    }
}
