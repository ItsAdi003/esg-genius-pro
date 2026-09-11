package dev.esgenius.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "document_page",
       uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "page_number"}))
public class DocumentPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    protected DocumentPage() {
    }

    public DocumentPage(Document document, int pageNumber, String extractedText) {
        this.document = document;
        this.pageNumber = pageNumber;
        this.extractedText = extractedText;
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public String getExtractedText() {
        return extractedText;
    }
}
