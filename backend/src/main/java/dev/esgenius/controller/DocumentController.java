package dev.esgenius.controller;

import dev.esgenius.config.AuthenticatedUser;
import dev.esgenius.dto.DocumentDetailResponse;
import dev.esgenius.dto.DocumentPageResponse;
import dev.esgenius.dto.DocumentSummaryResponse;
import dev.esgenius.service.Caller;
import dev.esgenius.service.DocumentAccessPolicy;
import dev.esgenius.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final DocumentAccessPolicy documentAccessPolicy;

    public DocumentController(DocumentService documentService, DocumentAccessPolicy documentAccessPolicy) {
        this.documentService = documentService;
        this.documentAccessPolicy = documentAccessPolicy;
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
            @RequestParam(required = false) Integer reportingYear,
            HttpServletRequest request) {
        DocumentDetailResponse response = documentService.uploadDocument(
                file, organizationId, documentType, reportingYear, caller(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/documents?organizationId={id}
     * List documents for an organization.
     */
    @GetMapping
    public ResponseEntity<List<DocumentSummaryResponse>> listDocuments(
            @RequestParam Long organizationId,
            HttpServletRequest request) {
        return ResponseEntity.ok(documentService.listDocuments(organizationId, caller(request)));
    }

    /**
     * GET /api/v1/documents/{id}
     * Get document detail including extracted text.
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDetailResponse> getDocument(
            @PathVariable Long documentId,
            HttpServletRequest request) {
        return ResponseEntity.ok(documentService.getDocument(documentId, caller(request)));
    }

    /**
     * GET /api/v1/documents/{id}/pages
     * List per-page extracted text in ascending page order.
     */
    @GetMapping("/{documentId}/pages")
    public ResponseEntity<List<DocumentPageResponse>> getDocumentPages(
            @PathVariable Long documentId,
            HttpServletRequest request) {
        return ResponseEntity.ok(documentService.getDocumentPages(documentId, caller(request)));
    }

    /**
     * DELETE /api/v1/documents/{id}
     * Delete document metadata and stored file.
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId, HttpServletRequest request) {
        documentService.deleteDocument(documentId, caller(request));
        return ResponseEntity.noContent().build();
    }

    private Caller caller(HttpServletRequest request) {
        return documentAccessPolicy.resolve(AuthenticatedUser.from(request));
    }
}
