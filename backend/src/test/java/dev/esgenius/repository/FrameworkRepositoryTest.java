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
    void seededRequirementsAreRetrievable() {
        Framework brsr = frameworkRepository.findByCode("BRSR").orElseThrow();
        List<FrameworkRequirement> requirements = requirementRepository.findByFramework(brsr);
        assertThat(requirements).hasSize(14);
    }

    @Test
    void requirementsFilterByCategory() {
        Framework brsr = frameworkRepository.findByCode("BRSR").orElseThrow();

        List<FrameworkRequirement> env = requirementRepository.findByFrameworkAndCategory(brsr, EsgCategory.ENVIRONMENTAL);
        List<FrameworkRequirement> soc = requirementRepository.findByFrameworkAndCategory(brsr, EsgCategory.SOCIAL);
        List<FrameworkRequirement> gov = requirementRepository.findByFrameworkAndCategory(brsr, EsgCategory.GOVERNANCE);

        assertThat(env).hasSize(7);
        assertThat(soc).hasSize(4);
        assertThat(gov).hasSize(3);
    }

    @Test
    void requirementCodesMatchExpectedValues() {
        Framework brsr = frameworkRepository.findByCode("BRSR").orElseThrow();
        List<FrameworkRequirement> requirements = requirementRepository.findByFramework(brsr);

        List<String> codes = requirements.stream().map(FrameworkRequirement::getRequirementCode).toList();
        assertThat(codes).contains("ENV-001", "ENV-007", "SOC-001", "SOC-004", "GOV-001", "GOV-003");
    }
}
