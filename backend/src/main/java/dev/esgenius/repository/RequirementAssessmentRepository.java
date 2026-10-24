package dev.esgenius.repository;

import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.entity.ComplianceAnalysis;
import dev.esgenius.entity.RequirementAssessment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RequirementAssessmentRepository extends JpaRepository<RequirementAssessment, Long> {

    @EntityGraph(attributePaths = "frameworkRequirement")
    List<RequirementAssessment> findByAnalysisOrderByFrameworkRequirement_RequirementCodeAsc(
            ComplianceAnalysis analysis);

    Optional<RequirementAssessment> findByAnalysis_IdAndFrameworkRequirement_Id(
            Long analysisId, Long requirementId);

    /** Per analysis: (analysis id, section, indicator type, status, count) for the ESG score. */
    @Query("""
            SELECT ra.analysis.id, fr.section, fr.indicatorType, ra.assessmentStatus, COUNT(ra)
            FROM RequirementAssessment ra
            JOIN ra.frameworkRequirement fr
            WHERE ra.analysis.id IN :analysisIds
            GROUP BY ra.analysis.id, fr.section, fr.indicatorType, ra.assessmentStatus
            """)
    List<Object[]> countBySectionForAnalysisIds(@Param("analysisIds") Collection<Long> analysisIds);

    @Query("""
            SELECT ra.analysis.id, ra.assessmentStatus, COUNT(ra)
            FROM RequirementAssessment ra
            WHERE ra.analysis.id IN :analysisIds
            GROUP BY ra.analysis.id, ra.assessmentStatus
            """)
    List<Object[]> countByStatusForAnalysisIds(@Param("analysisIds") Collection<Long> analysisIds);
}
