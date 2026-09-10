package dev.esgenius.repository;

import dev.esgenius.entity.ComplianceAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceAnalysisRepository extends JpaRepository<ComplianceAnalysis, Long> {
}
