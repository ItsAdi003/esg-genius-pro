package dev.esgenius.repository;

import dev.esgenius.entity.Document;
import dev.esgenius.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOrganizationOrderByUploadedAtDesc(Organization organization);

    @Query("""
            SELECT d FROM Document d
            JOIN FETCH d.organization
            WHERE d.id = :id
            """)
    Optional<Document> findByIdWithOrganization(@Param("id") Long id);
}
