package dev.esgenius.repository;

import dev.esgenius.dto.CompanySummaryResponse;
import dev.esgenius.service.CompanyEsgService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FlywaySeedDataIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EsgRatingSnapshotRepository ratingSnapshotRepository;

    @Autowired
    private EsgKeyIssueRepository keyIssueRepository;

    @Autowired
    private CompanyKeyIssueAssessmentRepository assessmentRepository;

    @Autowired
    private EsgEventRepository eventRepository;

    @Autowired
    private CompanyEsgService companyEsgService;

    @Test
    void flywaySeedDataIsPresentAfterAllMigrations() {
        assertThat(organizationRepository.findByTicker("INFY")).isPresent();
        assertThat(organizationRepository.findByTicker("TCS")).isPresent();
        assertThat(organizationRepository.findByTicker("WPRO")).isPresent();
        assertThat(organizationRepository.findByTicker("HCLT")).isPresent();

        assertThat(organizationRepository.findAll().stream()
                .anyMatch(org -> "ABC Industries Ltd.".equals(org.getName()) && org.getTicker() == null))
                .isTrue();

        assertThat(keyIssueRepository.count()).isEqualTo(5);
        assertThat(ratingSnapshotRepository.count()).isEqualTo(16);
        assertThat(assessmentRepository.count()).isEqualTo(20);
        assertThat(eventRepository.count()).isEqualTo(11);
    }

    @Test
    void listCompaniesReturnsOnlyTickerListedOrganizations() {
        List<CompanySummaryResponse> companies = companyEsgService.listCompanies();

        assertThat(companies).hasSize(4);
        assertThat(companies.stream().map(CompanySummaryResponse::ticker).collect(java.util.stream.Collectors.toSet()))
                .containsExactlyInAnyOrder("INFY", "TCS", "WPRO", "HCLT");
        assertThat(companies.stream().map(CompanySummaryResponse::name).collect(java.util.stream.Collectors.toSet()))
                .isEqualTo(Set.of(
                        "Infosys Limited",
                        "Tata Consultancy Services",
                        "Wipro Limited",
                        "HCLTech"));
    }
}
