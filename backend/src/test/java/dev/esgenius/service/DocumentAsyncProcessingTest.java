package dev.esgenius.service;

import dev.esgenius.dto.DocumentDetailResponse;
import dev.esgenius.dto.StartAnalysisRequest;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Organization;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentPageRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.support.TestPdfFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Upload hands PDF extraction to a background worker. These tests use a queue-backed executor so
 * the PROCESSING state is observable, and a real database/storage (test profile, H2).
 */
@SpringBootTest
@ActiveProfiles("test")
class DocumentAsyncProcessingTest {

    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private DocumentPageRepository documentPageRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private ComplianceAnalysisRepository analysisRepository;
    @Autowired
    private LocalFileStorageService fileStorageService;
    @Autowired
    private PdfTextExtractionService realExtraction;
    @Autowired
    private DocumentAccessPolicy documentAccessPolicy;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ComplianceAnalysisService complianceAnalysisService;

    private final List<Runnable> queued = new ArrayList<>();
    private Organization organization;

    @BeforeEach
    void setUp() {
        analysisRepository.deleteAll();
        documentPageRepository.deleteAll();
        documentRepository.deleteAll();
        organization = organizationRepository.findByTicker("INFY").orElseThrow();
        queued.clear();
    }

    @Test
    void uploadReturnsProcessingImmediatelyThenWorkerMakesItReady() throws Exception {
        DocumentService service = documentServiceWith(queueingExecutor(), realExtraction);

        DocumentDetailResponse response = service.uploadDocument(sampleUpload(), organization.getId(), "BRSR", 2025);

        assertThat(response.status()).isEqualTo("PROCESSING");
        assertThat(response.extractedText()).isNull();
        assertThat(documentRepository.findById(response.id()).orElseThrow().getStatus())
                .isEqualTo(DocumentStatus.PROCESSING);
        assertThat(fileStorageService.exists(storedFilename(response.id()))).isTrue();
        assertThat(documentPageRepository.count()).isZero();
        assertThat(queued).hasSize(1);

        queued.get(0).run();

        Document done = documentRepository.findById(response.id()).orElseThrow();
        assertThat(done.getStatus()).isEqualTo(DocumentStatus.READY);
        assertThat(done.getProcessedAt()).isNotNull();
        assertThat(done.getPageCount()).isPositive();
        assertThat(done.getExtractedText()).isNotBlank();
        assertThat(documentPageRepository.count()).isPositive();
        assertThat(fileStorageService.exists(done.getStoredFilename())).isTrue();
    }

    @Test
    void ownershipIsRecordedAtUploadTimeBeforeTheWorkerRuns() throws Exception {
        DocumentService service = documentServiceWith(queueingExecutor(), realExtraction);
        java.util.UUID owner = java.util.UUID.fromString("00000000-0000-0000-0000-0000000000a1");

        DocumentDetailResponse response = service.uploadDocument(
                sampleUpload(), organization.getId(), "BRSR", 2025, new Caller(owner, false));

        assertThat(documentRepository.findById(response.id()).orElseThrow().getOwnerUserId()).isEqualTo(owner);
        assertThat(response.canModify()).isTrue();
    }

    @Test
    void ioFailureInWorkerMarksFailedWithFixedReasonAndDeletesFile() throws Exception {
        PdfTextExtractionService failing = throwing(new IOException("secret internal detail"));
        DocumentService service = documentServiceWith(queueingExecutor(), failing);

        DocumentDetailResponse response = service.uploadDocument(sampleUpload(), organization.getId(), "BRSR", 2025);
        String stored = storedFilename(response.id());
        queued.get(0).run();

        Document failed = documentRepository.findById(response.id()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("PDF extraction failed.");
        assertThat(failed.getFailureReason()).doesNotContain("secret");
        assertThat(fileStorageService.exists(stored)).isFalse();
    }

    @Test
    void unexpectedRuntimeErrorInWorkerNeverLeavesDocumentProcessing() throws Exception {
        PdfTextExtractionService exploding = throwing(new IllegalStateException("boom"));
        DocumentService service = documentServiceWith(queueingExecutor(), exploding);

        DocumentDetailResponse response = service.uploadDocument(sampleUpload(), organization.getId(), "BRSR", 2025);
        queued.get(0).run();

        Document failed = documentRepository.findById(response.id()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("PDF extraction failed.");
    }

    @Test
    void rejectedSubmissionFailsTheDocumentAsBusy() throws Exception {
        TaskExecutor rejecting = runnable -> {
            throw new RejectedExecutionException("queue full");
        };
        DocumentService service = documentServiceWith(rejecting, realExtraction);

        DocumentDetailResponse response = service.uploadDocument(sampleUpload(), organization.getId(), "BRSR", 2025);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.failureReason()).isEqualTo(DocumentProcessingService.BUSY_REASON);
        assertThat(fileStorageService.exists(storedFilename(response.id()))).isFalse();
    }

    @Test
    void documentDeletedBeforeWorkerRunsIsIgnoredQuietly() throws Exception {
        DocumentService service = documentServiceWith(queueingExecutor(), realExtraction);
        DocumentDetailResponse response = service.uploadDocument(sampleUpload(), organization.getId(), "BRSR", 2025);

        service.deleteDocument(response.id(), Caller.unidentifiedAdmin());
        queued.get(0).run();

        assertThat(documentRepository.existsById(response.id())).isFalse();
        assertThat(documentPageRepository.count()).isZero();
    }

    @Test
    void startupRecoveryFailsOnlyUnfinishedDocuments() {
        Document uploaded = saveDocument("uploaded.pdf", DocumentStatus.UPLOADED);
        Document processing = saveDocument("processing.pdf", DocumentStatus.PROCESSING);
        Document ready = saveDocument("ready.pdf", DocumentStatus.READY);
        Document failed = saveDocument("failed.pdf", DocumentStatus.FAILED);
        DocumentProcessingService processingService = processingServiceWith(queueingExecutor(), realExtraction);

        int recovered = processingService.markInterruptedDocumentsAsFailed();

        assertThat(recovered).isEqualTo(2);
        assertThat(reload(uploaded).getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(reload(uploaded).getFailureReason()).isEqualTo(DocumentProcessingService.INTERRUPTED_REASON);
        assertThat(reload(processing).getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(reload(ready).getStatus()).isEqualTo(DocumentStatus.READY);
        assertThat(reload(failed).getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(reload(ready).getFailureReason()).isNull();
    }

    @Test
    void analysisCannotStartWhileDocumentIsStillProcessing() throws Exception {
        DocumentService service = documentServiceWith(queueingExecutor(), realExtraction);
        DocumentDetailResponse response = service.uploadDocument(sampleUpload(), organization.getId(), "BRSR", 2025);

        assertThatThrownBy(() -> complianceAnalysisService.startAnalysis(
                response.id(), new StartAnalysisRequest(null, "BRSR"), Caller.unidentifiedAdmin()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("READY");
    }

    private DocumentService documentServiceWith(TaskExecutor executor, PdfTextExtractionService extraction) {
        return new DocumentService(
                documentRepository,
                documentPageRepository,
                organizationRepository,
                fileStorageService,
                processingServiceWith(executor, extraction),
                documentAccessPolicy);
    }

    private DocumentProcessingService processingServiceWith(
            TaskExecutor executor, PdfTextExtractionService extraction) {
        return new DocumentProcessingService(
                documentRepository,
                documentPageRepository,
                fileStorageService,
                extraction,
                executor,
                transactionManager);
    }

    /** Mockito cannot subclass concrete classes on this JDK, so use a hand-written stub. */
    private static PdfTextExtractionService throwing(Exception failure) {
        return new PdfTextExtractionService(new dev.esgenius.config.PdfLimitsProperties()) {
            @Override
            public ExtractedPdf extract(Path pdfPath) throws IOException {
                if (failure instanceof IOException io) {
                    throw io;
                }
                throw (RuntimeException) failure;
            }
        };
    }

    private TaskExecutor queueingExecutor() {
        return queued::add;
    }

    private MockMultipartFile sampleUpload() throws Exception {
        return new MockMultipartFile("file", "sample.pdf", "application/pdf", TestPdfFixtures.createSamplePdfBytes());
    }

    private String storedFilename(Long documentId) {
        return documentRepository.findById(documentId).orElseThrow().getStoredFilename();
    }

    private Document saveDocument(String filename, DocumentStatus status) {
        Document document = new Document(
                organization, filename, "missing-" + filename, DocumentType.BRSR, 2025, 1L, "missing-" + filename);
        document.setStatus(status);
        return documentRepository.save(document);
    }

    private Document reload(Document document) {
        return documentRepository.findById(document.getId()).orElseThrow();
    }
}
