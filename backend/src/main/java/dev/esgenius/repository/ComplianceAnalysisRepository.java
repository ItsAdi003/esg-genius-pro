package dev.esgenius.repository;

import dev.esgenius.entity.AnalysisStatus;
import dev.esgenius.entity.ComplianceAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ComplianceAnalysisRepository extends JpaRepository<ComplianceAnalysis, Long> {

    @Query("""
            SELECT a FROM ComplianceAnalysis a
            JOIN FETCH a.framework
            WHERE a.document.id = :documentId
            ORDER BY a.startedAt DESC
            """)
    List<ComplianceAnalysis> findByDocumentIdWithFrameworkOrderByStartedAtDesc(
            @Param("documentId") Long documentId);

    boolean existsByDocumentIdAndFrameworkIdAndStatus(
            Long documentId, Long frameworkId, AnalysisStatus status);

    List<ComplianceAnalysis> findByStatus(AnalysisStatus status);

    @Query("""
            SELECT a FROM ComplianceAnalysis a
            JOIN FETCH a.framework
            JOIN FETCH a.document
            WHERE a.id = :analysisId
            """)
    Optional<ComplianceAnalysis> findByIdWithFrameworkAndDocument(@Param("analysisId") Long analysisId);
}
