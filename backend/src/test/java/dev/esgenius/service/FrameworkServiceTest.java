package dev.esgenius.service;

import dev.esgenius.dto.FrameworkRequirementResponse;
import dev.esgenius.dto.FrameworkResponse;
import dev.esgenius.entity.EsgCategory;
import dev.esgenius.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class FrameworkServiceTest {

    @Autowired
    private FrameworkService frameworkService;

    @Test
    void listFrameworksReturnsBrsr() {
        List<FrameworkResponse> frameworks = frameworkService.listFrameworks();
        assertThat(frameworks).isNotEmpty();

        FrameworkResponse brsr = frameworks.stream()
                .filter(f -> "BRSR".equals(f.code()))
                .findFirst()
                .orElseThrow();

        assertThat(brsr.name()).isEqualTo("SEBI BRSR");
        assertThat(brsr.status()).isEqualTo("ACTIVE");
        assertThat(brsr.requirementCount()).isEqualTo(14);
    }

    @Test
    void getFrameworkReturnsCorrectData() {
        List<FrameworkResponse> all = frameworkService.listFrameworks();
        Long brsrId = all.stream()
                .filter(f -> "BRSR".equals(f.code()))
                .map(FrameworkResponse::id)
                .findFirst()
                .orElseThrow();

        FrameworkResponse result = frameworkService.getFramework(brsrId);
        assertThat(result.fullName()).isEqualTo("Business Responsibility and Sustainability Reporting");
        assertThat(result.region()).isEqualTo("India");
    }

    @Test
    void getFrameworkThrowsForUnknownId() {
        assertThatThrownBy(() -> frameworkService.getFramework(99999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRequirementsReturnsAll14() {
        Long brsrId = getBrsrId();
        List<FrameworkRequirementResponse> requirements = frameworkService.getRequirements(brsrId, null);
        assertThat(requirements).hasSize(14);
    }

    @Test
    void getRequirementsFiltersByCategory() {
        Long brsrId = getBrsrId();

        List<FrameworkRequirementResponse> env = frameworkService.getRequirements(brsrId, EsgCategory.ENVIRONMENTAL);
        List<FrameworkRequirementResponse> gov = frameworkService.getRequirements(brsrId, EsgCategory.GOVERNANCE);

        assertThat(env).hasSize(7);
        assertThat(gov).hasSize(3);
        assertThat(env).allSatisfy(r -> assertThat(r.category()).isEqualTo("ENVIRONMENTAL"));
    }

    @Test
    void getRequirementsThrowsForUnknownFramework() {
        assertThatThrownBy(() -> frameworkService.getRequirements(99999L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Long getBrsrId() {
        return frameworkService.listFrameworks().stream()
                .filter(f -> "BRSR".equals(f.code()))
                .map(FrameworkResponse::id)
                .findFirst()
                .orElseThrow();
    }
}
