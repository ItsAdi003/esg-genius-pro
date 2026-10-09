package dev.esgenius.repository;

import dev.esgenius.entity.Document;
import dev.esgenius.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOrganizationOrderByUploadedAtDesc(Organization organization);

    @Query("""
            SELECT d FROM Document d
            WHERE d.organization = :organization
              AND (d.ownerUserId IS NULL OR d.ownerUserId = :userId)
            ORDER BY d.uploadedAt DESC
            """)
    List<Document> findVisibleByOrganization(
            @Param("organization") Organization organization,
            @Param("userId") UUID userId);

    @Query("""
            SELECT d FROM Document d
            JOIN FETCH d.organization
            WHERE d.id = :id
            """)
    Optional<Document> findByIdWithOrganization(@Param("id") Long id);
}
