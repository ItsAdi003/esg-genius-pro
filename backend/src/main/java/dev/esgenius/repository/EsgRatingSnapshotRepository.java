package dev.esgenius.repository;

import dev.esgenius.entity.EsgRatingSnapshot;
import dev.esgenius.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EsgRatingSnapshotRepository extends JpaRepository<EsgRatingSnapshot, Long> {

    /**
     * Find the latest rating snapshot for an organization.
     */
    @Query("""
                SELECT e FROM EsgRatingSnapshot e
                WHERE e.organization = :organization
                ORDER BY e.assessmentDate DESC
                LIMIT 1
            """)
    Optional<EsgRatingSnapshot> findLatestByOrganization(@Param("organization") Organization organization);

    /**
     * Find all rating snapshots for an organization, ordered by date (descending).
     */
    List<EsgRatingSnapshot> findByOrganizationOrderByAssessmentDateDesc(Organization organization);

    /**
     * Find rating snapshots within a date range.
     */
    @Query("""
                SELECT e FROM EsgRatingSnapshot e
                WHERE e.organization = :organization
                AND e.assessmentDate BETWEEN :startDate AND :endDate
                ORDER BY e.assessmentDate DESC
            """)
    List<EsgRatingSnapshot> findByOrganizationAndDateRange(
            @Param("organization") Organization organization,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
