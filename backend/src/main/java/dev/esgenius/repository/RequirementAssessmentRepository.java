package dev.esgenius.repository;

import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.entity.ComplianceAnalysis;
import dev.esgenius.entity.RequirementAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RequirementAssessmentRepository extends JpaRepository<RequirementAssessment, Long> {

    List<RequirementAssessment> findByAnalysisOrderByFrameworkRequirement_RequirementCodeAsc(
            ComplianceAnalysis analysis);

    @Query("""
            SELECT ra.analysis.id, ra.assessmentStatus, COUNT(ra)
            FROM RequirementAssessment ra
            WHERE ra.analysis.id IN :analysisIds
            GROUP BY ra.analysis.id, ra.assessmentStatus
            """)
    List<Object[]> countByStatusForAnalysisIds(@Param("analysisIds") Collection<Long> analysisIds);
}
