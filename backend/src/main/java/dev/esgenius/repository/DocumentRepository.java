package dev.esgenius.repository;

import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * List-view columns only. Selecting the {@code Document} entity would also pull the full
     * {@code extracted_text} of every row, which list responses never use.
     */
    interface DocumentListRow {
        Long getId();

        String getOriginalFilename();

        DocumentType getDocumentType();

        Integer getReportingYear();

        DocumentStatus getStatus();

        Long getFileSize();

        Integer getPageCount();

        Instant getUploadedAt();

        Instant getProcessedAt();

        UUID getOwnerUserId();
    }

    String LIST_ROW_SELECT = """
            SELECT d.id AS id, d.originalFilename AS originalFilename, d.documentType AS documentType,
                   d.reportingYear AS reportingYear, d.status AS status, d.fileSize AS fileSize,
                   d.pageCount AS pageCount, d.uploadedAt AS uploadedAt, d.processedAt AS processedAt,
                   d.ownerUserId AS ownerUserId
            FROM Document d
            """;

    List<Document> findByStatusIn(Collection<DocumentStatus> statuses);

    @Query(LIST_ROW_SELECT + """
            WHERE d.organization = :organization
            ORDER BY d.uploadedAt DESC
            """)
    List<DocumentListRow> findListRowsByOrganization(@Param("organization") Organization organization);

    @Query(LIST_ROW_SELECT + """
            WHERE d.organization = :organization
              AND (d.ownerUserId IS NULL OR d.ownerUserId = :userId)
            ORDER BY d.uploadedAt DESC
            """)
    List<DocumentListRow> findVisibleListRowsByOrganization(
            @Param("organization") Organization organization,
            @Param("userId") UUID userId);

    @Query("""
            SELECT d FROM Document d
            JOIN FETCH d.organization
            WHERE d.id = :id
            """)
    Optional<Document> findByIdWithOrganization(@Param("id") Long id);
}
