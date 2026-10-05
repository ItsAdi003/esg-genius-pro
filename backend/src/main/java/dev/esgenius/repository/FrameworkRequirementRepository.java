package dev.esgenius.repository;

import dev.esgenius.entity.EsgCategory;
import dev.esgenius.entity.Framework;
import dev.esgenius.entity.FrameworkRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FrameworkRequirementRepository extends JpaRepository<FrameworkRequirement, Long> {

    List<FrameworkRequirement> findByFramework(Framework framework);

    List<FrameworkRequirement> findByFrameworkAndCategory(Framework framework, EsgCategory category);

    long countByFramework(Framework framework);

    @Query("""
            SELECT r.framework.id, COUNT(r)
            FROM FrameworkRequirement r
            GROUP BY r.framework.id
            """)
    List<Object[]> countGroupedByFrameworkId();
}
