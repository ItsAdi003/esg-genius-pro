package dev.esgenius.repository;

import dev.esgenius.dto.CompanySummaryResponse;
import dev.esgenius.dto.CompanyComparisonResponse;
import dev.esgenius.dto.CompanyEsgProfileResponse;
import dev.esgenius.dto.EsgRatingHistoryResponse;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.service.CompanyEsgService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void infyRatingHistoryIsChronologicalOldestToNewest() {
        var infy = organizationRepository.findByTicker("INFY").orElseThrow();

        CompanyEsgProfileResponse profile = companyEsgService.getCompanyEsgProfile(infy.getId());

        assertThat(profile.ratingHistory()).extracting(EsgRatingHistoryResponse::quarter)
                .containsExactly("Q4 2025", "Q1 2026", "Q2 2026", "Q3 2026");
        assertThat(profile.ratingHistory()).extracting(EsgRatingHistoryResponse::score)
                .containsExactly(7.5, 7.6, 7.7, 7.8);
    }

    @Test
    void infyVsTcsComparisonInsightMatchesPillarScores() {
        var infy = organizationRepository.findByTicker("INFY").orElseThrow();
        var tcs = organizationRepository.findByTicker("TCS").orElseThrow();

        CompanyComparisonResponse comparison = companyEsgService.compareCompanies(infy.getId(), tcs.getId());
        String insight = comparison.comparisonInsight();

        assertThat(insight).contains("Infosys Limited");
        assertThat(insight).contains("stronger Environmental (7.3 vs 6.7)");
        assertThat(insight).contains("stronger Social (8.2 vs 7.4)");
        assertThat(insight).contains("stronger Governance (8.0 vs 7.7)");
        assertThat(insight).doesNotContain("Tata Consultancy Services leads on Governance");
        assertThat(insight).doesNotContain("remains comparatively strong in Governance");
        assertThat(insight).contains("Tata Consultancy Services does not lead on Environmental, Social or Governance");
    }

    @Test
    void v7DocumentTableIsAvailableAfterMigration() {
        String version = jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE version = '7'",
                String.class);
        assertThat(version).isEqualTo("7");
        assertThat(documentRepository.findAll()).isNotNull();
    }

    @Test
    void v8ComplianceAnalysisSchemaIsAvailableAfterMigration() {
        String version = jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE version = '8'",
                String.class);
        assertThat(version).isEqualTo("8");

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM compliance_analysis", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM requirement_assessment", Integer.class))
                .isZero();
    }
}
