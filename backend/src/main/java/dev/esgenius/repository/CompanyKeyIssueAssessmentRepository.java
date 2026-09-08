package dev.esgenius.repository;

import dev.esgenius.entity.CompanyKeyIssueAssessment;
import dev.esgenius.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CompanyKeyIssueAssessmentRepository extends JpaRepository<CompanyKeyIssueAssessment, Long> {

    /**
     * Find all key issue assessments for an organization.
     */
    List<CompanyKeyIssueAssessment> findByOrganization(Organization organization);

    /**
     * Find latest assessments for an organization (most recent assessment date).
     */
    @Query("""
                SELECT a FROM CompanyKeyIssueAssessment a
                WHERE a.organization = :organization
                AND a.assessmentDate = (
                    SELECT MAX(a2.assessmentDate)
                    FROM CompanyKeyIssueAssessment a2
                    WHERE a2.organization = :organization
                )
                ORDER BY a.keyIssue.pillar, a.keyIssue.name
            """)
    List<CompanyKeyIssueAssessment> findLatestAssessmentsByOrganization(
            @Param("organization") Organization organization);

    /**
     * Find assessments for a specific date.
     */
    List<CompanyKeyIssueAssessment> findByOrganizationAndAssessmentDate(Organization organization,
            LocalDate assessmentDate);
}
