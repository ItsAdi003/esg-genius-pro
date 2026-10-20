package dev.esgenius.repository;

import dev.esgenius.entity.EsgCategory;
import dev.esgenius.entity.Framework;
import dev.esgenius.entity.FrameworkRequirement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class FrameworkRepositoryTest {

    @Autowired
    private FrameworkRepository frameworkRepository;

    @Autowired
    private FrameworkRequirementRepository requirementRepository;

    @Test
    void flywayMigrationsLoadSuccessfully() {
        // If we get here, Flyway ran all migrations and JPA validate passed
        assertThat(frameworkRepository.count()).isGreaterThan(0);
    }

    @Test
    void brsrFrameworkExistsByCode() {
        Optional<Framework> brsr = frameworkRepository.findByCode("BRSR");
        assertThat(brsr).isPresent();
        assertThat(brsr.get().getName()).isEqualTo("SEBI BRSR");
        assertThat(brsr.get().getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void completeBrsrRequirementSetIsActive() {
        Framework brsr = frameworkRepository.findByCode("BRSR").orElseThrow();
        List<FrameworkRequirement> active = requirementRepository.findActiveByFramework(brsr);

        // Section A 24 + Section B 12 + Section C 62 Essential + 42 Leadership indicators.
        assertThat(active).hasSize(140);
        assertThat(requirementRepository.countActiveByFramework(brsr)).isEqualTo(140);
        assertThat(active).allSatisfy(requirement -> {
            assertThat(requirement.getSection()).isNotBlank();
            assertThat(requirement.getIndicatorType()).isNotBlank();
            assertThat(requirement.getSortOrder()).isNotNull();
        });
    }

    @Test
    void legacyMvpRequirementsAreKeptButInactive() {
        Framework brsr = frameworkRepository.findByCode("BRSR").orElseThrow();
        List<FrameworkRequirement> all = requirementRepository.findByFramework(brsr);
        List<FrameworkRequirement> legacy = all.stream().filter(r -> !r.isActive()).toList();

        assertThat(all).hasSize(154);
        assertThat(legacy).hasSize(14);
        assertThat(legacy).allSatisfy(requirement -> assertThat(requirement.getSection()).isNull());
        assertThat(requirementRepository.findActiveByFramework(brsr))
                .extracting(FrameworkRequirement::getRequirementCode)
                .doesNotContain("ENV-001", "SOC-004", "GOV-003");
    }

    @Test
    void sectionCountsMatchTheOfficialFormat() {
        Framework brsr = frameworkRepository.findByCode("BRSR").orElseThrow();
        List<FrameworkRequirement> active = requirementRepository.findActiveByFramework(brsr);

        assertThat(countOf(active, "A", "GENERAL")).isEqualTo(24);
        assertThat(countOf(active, "B", "MANAGEMENT")).isEqualTo(12);
        int[] essential = {7, 4, 15, 2, 10, 12, 2, 4, 6};
        int[] leadership = {2, 5, 6, 3, 5, 9, 1, 6, 5};
        for (int principle = 1; principle <= 9; principle++) {
            assertThat(countOf(active, "P" + principle, "ESSENTIAL"))
                    .as("P%d essential", principle).isEqualTo(essential[principle - 1]);
            assertThat(countOf(active, "P" + principle, "LEADERSHIP"))
                    .as("P%d leadership", principle).isEqualTo(leadership[principle - 1]);
        }
        assertThat(active.stream().filter(FrameworkRequirement::isMandatory).count()).isEqualTo(98);
    }

    @Test
    void activeRequirementsComeInFrameworkOrder() {
        Framework brsr = frameworkRepository.findByCode("BRSR").orElseThrow();
        List<String> codes = requirementRepository.findActiveByFramework(brsr).stream()
                .map(FrameworkRequirement::getRequirementCode).toList();

        assertThat(codes.subList(0, 3)).containsExactly("A-01", "A-02", "A-03");
        assertThat(codes.indexOf("B-01")).isEqualTo(24);
        assertThat(codes.indexOf("P1-E01")).isEqualTo(36);
        assertThat(codes.indexOf("P1-L01")).isLessThan(codes.indexOf("P2-E01"));
        assertThat(codes.get(codes.size() - 1)).isEqualTo("P9-L05");
    }

    @Test
    void requirementsFilterByCategory() {
        Framework brsr = frameworkRepository.findByCode("BRSR").orElseThrow();

        List<FrameworkRequirement> env = requirementRepository.findActiveByFrameworkAndCategory(brsr, EsgCategory.ENVIRONMENTAL);
        List<FrameworkRequirement> soc = requirementRepository.findActiveByFrameworkAndCategory(brsr, EsgCategory.SOCIAL);
        List<FrameworkRequirement> gov = requirementRepository.findActiveByFrameworkAndCategory(brsr, EsgCategory.GOVERNANCE);

        assertThat(env).hasSize(30);
        assertThat(soc).hasSize(66);
        assertThat(gov).hasSize(44);
    }

    @Test
    void requirementCodesAreUniqueAndFitTheColumn() {
        Framework brsr = frameworkRepository.findByCode("BRSR").orElseThrow();
        List<String> codes = requirementRepository.findActiveByFramework(brsr).stream()
                .map(FrameworkRequirement::getRequirementCode).toList();

        assertThat(codes).doesNotHaveDuplicates();
        assertThat(codes).allSatisfy(code -> assertThat(code.length()).isLessThanOrEqualTo(20));
        assertThat(codes).contains("A-01", "B-12", "P1-E01", "P6-E06", "P6-L04", "P9-L05");
    }

    private static long countOf(List<FrameworkRequirement> requirements, String section, String indicatorType) {
        return requirements.stream()
                .filter(r -> section.equals(r.getSection()) && indicatorType.equals(r.getIndicatorType()))
                .count();
    }
}
