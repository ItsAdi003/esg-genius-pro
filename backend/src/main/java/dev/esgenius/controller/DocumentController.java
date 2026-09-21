package dev.esgenius.controller;

import dev.esgenius.dto.DocumentDetailResponse;
import dev.esgenius.dto.DocumentPageResponse;
import dev.esgenius.dto.DocumentSummaryResponse;
import dev.esgenius.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST API for uploaded ESG document ingestion.
 */
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * POST /api/v1/documents
     * Upload and process a PDF document.
     */
    @PostMapping
    public ResponseEntity<DocumentDetailResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long organizationId,
            @RequestParam String documentType,
            @RequestParam(required = false) Integer reportingYear) {
        DocumentDetailResponse response = documentService.uploadDocument(
                file, organizationId, documentType, reportingYear);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/documents?organizationId={id}
     * List documents for an organization.
     */
    @GetMapping
    public ResponseEntity<List<DocumentSummaryResponse>> listDocuments(
            @RequestParam Long organizationId) {
        return ResponseEntity.ok(documentService.listDocuments(organizationId));
    }

    /**
     * GET /api/v1/documents/{id}
     * Get document detail including extracted text.
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDetailResponse> getDocument(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentService.getDocument(documentId));
    }

    /**
     * GET /api/v1/documents/{id}/pages
     * List per-page extracted text in ascending page order.
     */
    @GetMapping("/{documentId}/pages")
    public ResponseEntity<List<DocumentPageResponse>> getDocumentPages(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentService.getDocumentPages(documentId));
    }

    /**
     * DELETE /api/v1/documents/{id}
     * Delete document metadata and stored file.
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
