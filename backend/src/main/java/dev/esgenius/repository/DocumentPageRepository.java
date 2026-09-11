package dev.esgenius.repository;

import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentPageRepository extends JpaRepository<DocumentPage, Long> {

    List<DocumentPage> findByDocumentOrderByPageNumberAsc(Document document);

    void deleteByDocument(Document document);
}
