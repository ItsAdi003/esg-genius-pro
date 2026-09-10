package dev.esgenius.repository;

import dev.esgenius.entity.Document;
import dev.esgenius.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOrganizationOrderByUploadedAtDesc(Organization organization);
}
