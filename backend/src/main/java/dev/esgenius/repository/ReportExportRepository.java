package dev.esgenius.repository;

import dev.esgenius.entity.ReportExport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReportExportRepository extends JpaRepository<ReportExport, Long> {

    @Query("""
            SELECT e FROM ReportExport e
            JOIN FETCH e.analysis a
            JOIN FETCH a.document
            JOIN FETCH a.framework
            ORDER BY e.generatedAt DESC, e.id DESC
            """)
    List<ReportExport> findRecentAll(Pageable pageable);

    @Query("""
            SELECT e FROM ReportExport e
            JOIN FETCH e.analysis a
            JOIN FETCH a.document
            JOIN FETCH a.framework
            WHERE e.userId = :userId
            ORDER BY e.generatedAt DESC, e.id DESC
            """)
    List<ReportExport> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);
}
