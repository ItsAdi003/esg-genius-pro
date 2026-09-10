package dev.esgenius.repository;

import dev.esgenius.entity.ComplianceAnalysis;
import dev.esgenius.entity.RequirementAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequirementAssessmentRepository extends JpaRepository<RequirementAssessment, Long> {

    List<RequirementAssessment> findByAnalysisOrderByFrameworkRequirement_RequirementCodeAsc(
            ComplianceAnalysis analysis);
}
