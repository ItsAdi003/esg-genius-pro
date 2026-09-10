package dev.esgenius.repository;

import dev.esgenius.entity.EsgEvent;
import dev.esgenius.entity.Organization;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EsgEventRepository extends JpaRepository<EsgEvent, Long> {

    /**
     * Find all ESG events for an organization, ordered by event date (descending).
     */
    List<EsgEvent> findByOrganizationOrderByEventDateDesc(Organization organization);

    /**
     * Find recent ESG events for an organization (last N events).
     */
    List<EsgEvent> findByOrganizationOrderByEventDateDesc(Organization organization, Pageable pageable);

    /**
     * Find events within a date range.
     */
    @Query("""
                SELECT e FROM EsgEvent e
                WHERE e.organization = :organization
                AND e.eventDate BETWEEN :startDate AND :endDate
                ORDER BY e.eventDate DESC
            """)
    List<EsgEvent> findByOrganizationAndDateRange(
            @Param("organization") Organization organization,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find prototype events (for demo/testing purposes).
     */
    List<EsgEvent> findByOrganizationAndIsPrototypeOrderByEventDateDesc(Organization organization, Boolean isPrototype);
}
