package dev.esgenius.service;

import dev.esgenius.dto.*;
import dev.esgenius.entity.*;
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
        // Test
        List<CompanySummaryResponse> companies = companyEsgService.listCompanies();

        // Verify
        assertEquals(2, companies.size());
        assertTrue(companies.stream().anyMatch(c -> "Company A".equals(c.name())));
        assertTrue(companies.stream().anyMatch(c -> "Company B".equals(c.name())));
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
        assertThrows(IllegalArgumentException.class,
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
        // Company A has higher overall score, better env/social, fewer high-severity
        // events
        // Expected: insight should mention A has higher score with reasons

        // Test
        CompanyComparisonResponse comparison = companyEsgService.compareCompanies(company1.getId(), company2.getId());

        // Verify
        String insight = comparison.comparisonInsight();
        assertTrue(insight.contains("Company A"));
        assertTrue(insight.contains("higher"));
        // Since Company A has better scores in env and social
        assertTrue(insight.toLowerCase().contains("strong") || insight.toLowerCase().contains("higher"));
    }
}
