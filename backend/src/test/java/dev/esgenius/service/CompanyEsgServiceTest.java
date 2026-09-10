package dev.esgenius.service;

import dev.esgenius.dto.*;
import dev.esgenius.entity.*;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(CompanyEsgService.class)
@ActiveProfiles("test")
class CompanyEsgServiceTest {

    @Autowired
    private CompanyEsgService companyEsgService;

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

    private Organization company1;
    private Organization company2;

    @BeforeEach
    void setup() {
        // Create two companies
        company1 = organizationRepository.save(new Organization("Company A", "CMPA", "IT", "Tech", null));
        company2 = organizationRepository.save(new Organization("Company B", "CMPB", "IT", "Tech", null));

        // Create rating snapshots
        ratingSnapshotRepository.save(
                new EsgRatingSnapshot(company1, 7.5, 7.2, 7.8, 7.6, EsgRatingBand.AA, LocalDate.of(2026, 9, 7), 7.4));
        ratingSnapshotRepository.save(
                new EsgRatingSnapshot(company2, 6.8, 6.5, 7.0, 7.1, EsgRatingBand.A, LocalDate.of(2026, 9, 7), 6.7));

        // Create key issues
        EsgKeyIssue issue1 = keyIssueRepository
                .save(new EsgKeyIssue("CARBON", "Carbon Emissions", EsgPillar.ENVIRONMENTAL, "GHG Emissions"));
        EsgKeyIssue issue2 = keyIssueRepository
                .save(new EsgKeyIssue("HUMAN_CAP", "Human Capital", EsgPillar.SOCIAL, "Employee Practices"));

        // Create assessments
        assessmentRepository.save(
                new CompanyKeyIssueAssessment(company1, issue1, 7.5, RiskLevel.MODERATE, LocalDate.of(2026, 9, 7)));
        assessmentRepository
                .save(new CompanyKeyIssueAssessment(company1, issue2, 8.0, RiskLevel.LOW, LocalDate.of(2026, 9, 7)));
        assessmentRepository
                .save(new CompanyKeyIssueAssessment(company2, issue1, 6.5, RiskLevel.HIGH, LocalDate.of(2026, 9, 7)));
        assessmentRepository.save(
                new CompanyKeyIssueAssessment(company2, issue2, 7.2, RiskLevel.MODERATE, LocalDate.of(2026, 9, 7)));

        // Create events
        eventRepository.save(new EsgEvent(company1, "Green Initiative", "Renewable Energy", EsgPillar.ENVIRONMENTAL,
                EsgEventSeverity.LOW, LocalDate.of(2026, 8, 1), 0.2, true));
        eventRepository.save(new EsgEvent(company2, "Supply Chain Issue", "Audit Gap", EsgPillar.ENVIRONMENTAL,
                EsgEventSeverity.HIGH, LocalDate.of(2026, 7, 15), -0.3, true));
    }

    @Test
    void testListCompanies() {
        List<CompanySummaryResponse> companies = companyEsgService.listCompanies();

        assertTrue(companies.size() >= 2);
        assertTrue(companies.stream().anyMatch(c -> "CMPA".equals(c.ticker()) && "Company A".equals(c.name())));
        assertTrue(companies.stream().anyMatch(c -> "CMPB".equals(c.ticker()) && "Company B".equals(c.name())));
    }

    @Test
    void testGetCompanyEsgProfile() {
        // Test
        CompanyEsgProfileResponse profile = companyEsgService.getCompanyEsgProfile(company1.getId());

        // Verify
        assertEquals("Company A", profile.name());
        assertEquals(7.5, profile.currentRating().overallScore());
        assertEquals("AA", profile.currentRating().ratingBand());
        assertEquals(2, profile.materialIssues().size());
        assertEquals(1, profile.recentEvents().size());
    }

    @Test
    void testRatingHistoryOrderedOldestToNewestByAssessmentDate() {
        ratingSnapshotRepository.save(
                new EsgRatingSnapshot(company1, 7.2, 7.0, 7.1, 7.3, EsgRatingBand.A, LocalDate.of(2025, 12, 31), 7.0));
        ratingSnapshotRepository.save(
                new EsgRatingSnapshot(company1, 7.3, 7.1, 7.2, 7.4, EsgRatingBand.A, LocalDate.of(2026, 3, 31), 7.2));
        ratingSnapshotRepository.save(
                new EsgRatingSnapshot(company1, 7.4, 7.2, 7.3, 7.5, EsgRatingBand.AA, LocalDate.of(2026, 6, 30), 7.3));

        CompanyEsgProfileResponse profile = companyEsgService.getCompanyEsgProfile(company1.getId());

        assertEquals(
                List.of("Q4 2025", "Q1 2026", "Q2 2026", "Q3 2026"),
                profile.ratingHistory().stream().map(EsgRatingHistoryResponse::quarter).toList());
        assertEquals(List.of(7.2, 7.3, 7.4, 7.5), profile.ratingHistory().stream().map(EsgRatingHistoryResponse::score).toList());
    }

    @Test
    void testCompareCompanies() {
        // Test
        CompanyComparisonResponse comparison = companyEsgService.compareCompanies(company1.getId(), company2.getId());

        // Verify
        assertEquals("Company A", comparison.companyA().name());
        assertEquals("Company B", comparison.companyB().name());
        assertNotNull(comparison.comparisonInsight());
        assertTrue(comparison.comparisonInsight().contains("Company A"));
        assertTrue(comparison.comparisonInsight().contains("higher"));
    }

    @Test
    void testCompareCompanies_SameCompanyRejects() {
        // Test & Verify
        assertThrows(BadRequestException.class,
                () -> companyEsgService.compareCompanies(company1.getId(), company1.getId()));
    }

    @Test
    void testCompareCompanies_MissingCompanyThrows() {
        // Test & Verify
        assertThrows(ResourceNotFoundException.class,
                () -> companyEsgService.compareCompanies(company1.getId(), 999L));
    }

    @Test
    void testComparisonInsightLogic() {
        CompanyComparisonResponse comparison = companyEsgService.compareCompanies(company1.getId(), company2.getId());

        String insight = comparison.comparisonInsight();
        assertTrue(insight.contains("Company A"));
        assertTrue(insight.contains("higher"));
        assertTrue(insight.contains("stronger Environmental"));
        assertTrue(insight.contains("stronger Social"));
        assertFalse(insight.contains("Company B leads on Governance"));
        assertFalse(insight.contains("Company B leads on Environmental"));
        assertFalse(insight.contains("Company B leads on Social"));
        assertTrue(insight.contains("Company B does not lead on Environmental, Social or Governance"));
    }

    @Test
    void testComparisonInsightReportsPillarLeadOnlyWhenScoreIsHigher() {
        Organization leader = organizationRepository.save(new Organization("Leader Co", "LEAD", "IT", "Tech", null));
        Organization other = organizationRepository.save(new Organization("Other Co", "OTHR", "IT", "Tech", null));

        ratingSnapshotRepository.save(
                new EsgRatingSnapshot(leader, 7.0, 7.5, 6.5, 6.8, EsgRatingBand.A, LocalDate.of(2026, 9, 7), 6.9));
        ratingSnapshotRepository.save(
                new EsgRatingSnapshot(other, 6.8, 6.0, 7.2, 7.0, EsgRatingBand.A, LocalDate.of(2026, 9, 7), 6.7));

        String insight = companyEsgService.compareCompanies(leader.getId(), other.getId()).comparisonInsight();

        assertTrue(insight.contains("Leader Co"));
        assertTrue(insight.contains("stronger Environmental"));
        assertTrue(insight.contains("Other Co leads on Social (7.2 vs 6.5)"));
        assertTrue(insight.contains("Governance (7.0 vs 6.8)"));
        assertFalse(insight.contains("Environmental (6.0 vs 7.5)"));
    }
}
