package dev.esgenius.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true, length = 255)
    private String storedFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "reporting_year")
    private Integer reportingYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    protected Document() {
    }

    public Document(
            Organization organization,
            String originalFilename,
            String storedFilename,
            DocumentType documentType,
            Integer reportingYear,
            Long fileSize,
            String storagePath) {
        this.organization = organization;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.documentType = documentType;
        this.reportingYear = reportingYear;
        this.status = DocumentStatus.UPLOADED;
        this.fileSize = fileSize;
        this.storagePath = storagePath;
        this.uploadedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public Integer getReportingYear() {
        return reportingYear;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
