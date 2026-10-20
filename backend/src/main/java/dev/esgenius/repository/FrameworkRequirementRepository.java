package dev.esgenius.repository;

import dev.esgenius.entity.EsgCategory;
import dev.esgenius.entity.Framework;
import dev.esgenius.entity.FrameworkRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FrameworkRequirementRepository extends JpaRepository<FrameworkRequirement, Long> {

    /** Every requirement row, including inactive legacy ones. Use the active variants for product behaviour. */
    List<FrameworkRequirement> findByFramework(Framework framework);

    /** Active requirements in official framework order. */
    @Query("""
            SELECT r FROM FrameworkRequirement r
            WHERE r.framework = :framework AND r.active = TRUE
            ORDER BY r.sortOrder, r.requirementCode
            """)
    List<FrameworkRequirement> findActiveByFramework(@Param("framework") Framework framework);

    @Query("""
            SELECT r FROM FrameworkRequirement r
            WHERE r.framework = :framework AND r.category = :category AND r.active = TRUE
            ORDER BY r.sortOrder, r.requirementCode
            """)
    List<FrameworkRequirement> findActiveByFrameworkAndCategory(
            @Param("framework") Framework framework, @Param("category") EsgCategory category);

    @Query("SELECT COUNT(r) FROM FrameworkRequirement r WHERE r.framework = :framework AND r.active = TRUE")
    long countActiveByFramework(@Param("framework") Framework framework);

    @Query("""
            SELECT r.framework.id, COUNT(r)
            FROM FrameworkRequirement r
            WHERE r.active = TRUE
            GROUP BY r.framework.id
            """)
    List<Object[]> countActiveGroupedByFrameworkId();
}
