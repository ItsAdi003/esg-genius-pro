package dev.esgenius.repository;

import dev.esgenius.entity.ComplianceAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComplianceAnalysisRepository extends JpaRepository<ComplianceAnalysis, Long> {

    @Query("""
            SELECT a FROM ComplianceAnalysis a
            JOIN FETCH a.framework
            WHERE a.document.id = :documentId
            ORDER BY a.startedAt DESC
            """)
    List<ComplianceAnalysis> findByDocumentIdWithFrameworkOrderByStartedAtDesc(
            @Param("documentId") Long documentId);
}
