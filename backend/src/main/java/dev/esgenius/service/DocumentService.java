package dev.esgenius.service;

import dev.esgenius.dto.DocumentDetailResponse;
import dev.esgenius.dto.DocumentPageResponse;
import dev.esgenius.dto.DocumentSummaryResponse;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentPage;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Organization;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.DocumentPageRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final long MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024;
    private static final int MAX_ORIGINAL_FILENAME_LENGTH = 255;
    private static final int MIN_EXTRACTED_TEXT_LENGTH = 10;
    private static final String PDF_EXTRACTION_FAILED = "PDF extraction failed.";
    private static final String STORE_FAILED = "Failed to store uploaded file";
    private static final String DELETE_FAILED = "Failed to delete stored file";
    private static final String SCANNED_PDF_MESSAGE =
            "No extractable text found. Scanned or image-only PDFs are unsupported in this phase.";

    private final DocumentRepository documentRepository;
    private final DocumentPageRepository documentPageRepository;
    private final OrganizationRepository organizationRepository;
    private final LocalFileStorageService fileStorageService;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final DocumentAccessPolicy documentAccessPolicy;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentPageRepository documentPageRepository,
            OrganizationRepository organizationRepository,
            LocalFileStorageService fileStorageService,
            PdfTextExtractionService pdfTextExtractionService,
            DocumentAccessPolicy documentAccessPolicy) {
        this.documentRepository = documentRepository;
        this.documentPageRepository = documentPageRepository;
        this.organizationRepository = organizationRepository;
        this.fileStorageService = fileStorageService;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.documentAccessPolicy = documentAccessPolicy;
    }

    @Transactional
    public DocumentDetailResponse uploadDocument(
            MultipartFile file,
            Long organizationId,
            String documentType,
            Integer reportingYear) {
        return uploadDocument(
                file, organizationId, documentType, reportingYear, documentAccessPolicy.callerWhenIdentityAbsent());
    }

    @Transactional
    public DocumentDetailResponse uploadDocument(
            MultipartFile file,
            Long organizationId,
            String documentType,
            Integer reportingYear,
            Caller caller) {
        validateUploadRequest(file, organizationId, documentType, reportingYear);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        DocumentType parsedType = parseDocumentType(documentType);
        byte[] fileContent = readAndValidatePdfContent(file);
        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + ".pdf";

        Path storedPath;
        try {
            storedPath = fileStorageService.store(new ByteArrayInputStream(fileContent), storedFilename);
        } catch (IOException ex) {
            log.error("Failed to store uploaded file", ex);
            throw new BadRequestException(STORE_FAILED);
        }

        Document document = new Document(
                organization,
                originalFilename,
                storedFilename,
                parsedType,
                reportingYear,
                (long) fileContent.length,
                storedFilename);
        document.setOwnerUserId(caller.userId());

        document = documentRepository.save(document);
        processDocument(document, storedPath);
        document = documentRepository.save(document);

        return toDetailResponse(document, caller);
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryResponse> listDocuments(Long organizationId) {
        return listDocuments(organizationId, documentAccessPolicy.callerWhenIdentityAbsent());
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryResponse> listDocuments(Long organizationId, Caller caller) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        List<Document> documents = caller.admin()
                ? documentRepository.findByOrganizationOrderByUploadedAtDesc(organization)
                : documentRepository.findVisibleByOrganization(organization, caller.userId());

        return documents.stream()
                .map(document -> toSummaryResponse(document, caller))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse getDocument(Long documentId) {
        return getDocument(documentId, documentAccessPolicy.callerWhenIdentityAbsent());
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse getDocument(Long documentId, Caller caller) {
        Document document = documentRepository.findByIdWithOrganization(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        documentAccessPolicy.requireView(document, caller, "Document not found: " + documentId);
        return toDetailResponse(document, caller);
    }

    @Transactional(readOnly = true)
    public List<DocumentPageResponse> getDocumentPages(Long documentId) {
        return getDocumentPages(documentId, documentAccessPolicy.callerWhenIdentityAbsent());
    }

    @Transactional(readOnly = true)
    public List<DocumentPageResponse> getDocumentPages(Long documentId, Caller caller) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        documentAccessPolicy.requireView(document, caller, "Document not found: " + documentId);

        return documentPageRepository.findByDocumentOrderByPageNumberAsc(document).stream()
                .map(page -> new DocumentPageResponse(page.getPageNumber(), page.getExtractedText()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        deleteDocument(documentId, documentAccessPolicy.callerWhenIdentityAbsent());
    }

    @Transactional
    public void deleteDocument(Long documentId, Caller caller) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        documentAccessPolicy.requireModify(document, caller, "Document not found: " + documentId);

        try {
            fileStorageService.delete(document.getStoredFilename());
        } catch (IOException ex) {
            log.error("Failed to delete stored file for document {}", documentId, ex);
            throw new BadRequestException(DELETE_FAILED);
        }

        documentRepository.delete(document);
    }

    private void processDocument(Document document, Path storedPath) {
        document.setStatus(DocumentStatus.PROCESSING);

        try {
            ExtractedPdf extractionResult = pdfTextExtractionService.extract(storedPath);
            document.setPageCount(extractionResult.pageCount());

            if (extractionResult.fullText().length() < MIN_EXTRACTED_TEXT_LENGTH) {
                failDocument(document, SCANNED_PDF_MESSAGE);
                return;
            }

            document.setExtractedText(extractionResult.fullText());
            persistDocumentPages(document, extractionResult.pages());
            document.setStatus(DocumentStatus.READY);
            document.setProcessedAt(Instant.now());
        } catch (PdfExtractionLimitException ex) {
            log.warn("PDF extraction rejected documentId={} reason={}", document.getId(), ex.getMessage());
            failDocument(document, ex.getMessage());
        } catch (IOException ex) {
            log.error("PDF extraction failed documentId={}", document.getId(), ex);
            failDocument(document, PDF_EXTRACTION_FAILED);
        }
    }

    private void failDocument(Document document, String reason) {
        document.setStatus(DocumentStatus.FAILED);
        document.setFailureReason(reason);
        document.setProcessedAt(Instant.now());
        try {
            fileStorageService.delete(document.getStoredFilename());
        } catch (IOException ex) {
            log.error("Failed to delete stored file after document failure documentId={}", document.getId(), ex);
        }
    }

    private void persistDocumentPages(Document document, List<ExtractedPdfPage> pages) {
        documentPageRepository.deleteByDocument(document);
        for (ExtractedPdfPage page : pages) {
            documentPageRepository.save(new DocumentPage(document, page.pageNumber(), page.text()));
        }
    }

    private void validateUploadRequest(
            MultipartFile file,
            Long organizationId,
            String documentType,
            Integer reportingYear) {
        if (organizationId == null) {
            throw new BadRequestException("organizationId is required");
        }
        if (documentType == null || documentType.isBlank()) {
            throw new BadRequestException("documentType is required");
        }
        if (reportingYear != null && (reportingYear < 1900 || reportingYear > 2100)) {
            throw new BadRequestException("reportingYear must be between 1900 and 2100");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds maximum allowed size of 25 MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are supported");
        }

        String contentType = file.getContentType();
        if (contentType != null
                && !contentType.equalsIgnoreCase("application/pdf")
                && !contentType.equalsIgnoreCase("application/x-pdf")) {
            throw new BadRequestException("Only PDF content type is supported");
        }

        parseDocumentType(documentType);
    }

    private byte[] readAndValidatePdfContent(MultipartFile file) {
        try {
            byte[] content = file.getBytes();
            if (content.length < 4 || !new String(content, 0, 4).startsWith("%PDF")) {
                throw new BadRequestException("Uploaded file is not a valid PDF");
            }
            return content;
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read uploaded file");
        }
    }

    private DocumentType parseDocumentType(String documentType) {
        try {
            return DocumentType.valueOf(documentType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid documentType: " + documentType);
        }
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.pdf";
        }
        String stripped = stripControlCharacters(originalFilename).trim();
        if (stripped.isBlank()) {
            return "upload.pdf";
        }
        String filename;
        try {
            filename = Paths.get(stripped).getFileName().toString().trim();
        } catch (InvalidPathException ex) {
            throw new BadRequestException("Invalid original filename");
        }
        if (filename.isBlank()) {
            return "upload.pdf";
        }
        if (filename.contains("..")) {
            throw new BadRequestException("Invalid original filename");
        }
        return truncatePreservingPdfExtension(filename);
    }

    private static String stripControlCharacters(String value) {
        StringBuilder stripped = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            if (!Character.isISOControl(codePoint)) {
                stripped.appendCodePoint(codePoint);
            }
        });
        return stripped.toString();
    }

    private static String truncatePreservingPdfExtension(String filename) {
        if (filename.length() <= MAX_ORIGINAL_FILENAME_LENGTH) {
            return filename;
        }
        String extension = "";
        if (filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            extension = filename.substring(filename.length() - 4);
        }
        int stemBudget = MAX_ORIGINAL_FILENAME_LENGTH - extension.length();
        if (stemBudget <= 0) {
            return filename.substring(0, MAX_ORIGINAL_FILENAME_LENGTH);
        }
        return filename.substring(0, stemBudget) + extension;
    }

    private DocumentSummaryResponse toSummaryResponse(Document document, Caller caller) {
        Organization organization = document.getOrganization();
        return new DocumentSummaryResponse(
                document.getId(),
                organization.getId(),
                organization.getName(),
                document.getOriginalFilename(),
                document.getDocumentType().name(),
                document.getReportingYear(),
                document.getStatus().name(),
                document.getFileSize(),
                document.getPageCount(),
                document.getUploadedAt(),
                document.getProcessedAt(),
                documentAccessPolicy.isShared(document),
                documentAccessPolicy.canModify(document, caller));
    }

    private DocumentDetailResponse toDetailResponse(Document document, Caller caller) {
        Organization organization = document.getOrganization();
        return new DocumentDetailResponse(
                document.getId(),
                organization.getId(),
                organization.getName(),
                document.getOriginalFilename(),
                document.getDocumentType().name(),
                document.getReportingYear(),
                document.getStatus().name(),
                document.getFileSize(),
                document.getPageCount(),
                document.getUploadedAt(),
                document.getProcessedAt(),
                document.getExtractedText(),
                document.getFailureReason(),
                documentAccessPolicy.isShared(document),
                documentAccessPolicy.canModify(document, caller));
    }
}
