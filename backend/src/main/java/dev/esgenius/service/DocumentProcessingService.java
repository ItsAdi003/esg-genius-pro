package dev.esgenius.service;

import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentPage;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.repository.DocumentPageRepository;
import dev.esgenius.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

/**
 * Extracts text from an uploaded PDF in the background.
 *
 * <p>The upload request saves the document as PROCESSING and calls {@link #submit(Long)}; this
 * service then moves it to READY or FAILED. Extraction itself runs outside any database
 * transaction (PDFBox can take a while), and each state change is its own short transaction.
 * Documents left PROCESSING by a crash or restart are failed by
 * {@link #markInterruptedDocumentsAsFailed()}.
 */
@Service
public class DocumentProcessingService {

    public static final String BUSY_REASON = "The server is busy. Please upload again in a minute.";
    public static final String INTERRUPTED_REASON =
            "Processing was interrupted by a restart. Please upload the document again.";
    static final String PDF_EXTRACTION_FAILED = "PDF extraction failed.";
    static final String SCANNED_PDF_MESSAGE =
            "No extractable text found. Scanned or image-only PDFs are unsupported in this phase.";
    private static final int MIN_EXTRACTED_TEXT_LENGTH = 10;

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final DocumentRepository documentRepository;
    private final DocumentPageRepository documentPageRepository;
    private final LocalFileStorageService fileStorageService;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final TaskExecutor executor;
    private final TransactionTemplate transactionTemplate;

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            DocumentPageRepository documentPageRepository,
            LocalFileStorageService fileStorageService,
            PdfTextExtractionService pdfTextExtractionService,
            @Qualifier("documentProcessingExecutor") TaskExecutor executor,
            PlatformTransactionManager transactionManager) {
        this.documentRepository = documentRepository;
        this.documentPageRepository = documentPageRepository;
        this.fileStorageService = fileStorageService;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.executor = executor;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Queue background processing for a document that is already saved as PROCESSING.
     * If the executor refuses the work the document is failed immediately so it never
     * sits in PROCESSING with nobody working on it.
     */
    public void submit(Long documentId) {
        try {
            executor.execute(() -> process(documentId));
        } catch (RejectedExecutionException ex) {
            log.error("Document processing was not accepted by the executor documentId={}", documentId, ex);
            markFailed(documentId, BUSY_REASON, null);
        }
    }

    void process(Long documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.info("Skipping processing of document {}: it no longer exists", documentId);
            return;
        }
        if (document.getStatus() != DocumentStatus.PROCESSING) {
            log.warn("Skipping processing of document {} because status is {}", documentId, document.getStatus());
            return;
        }

        try {
            ExtractedPdf extracted = pdfTextExtractionService.extract(
                    fileStorageService.resolveStoredPath(document.getStoredFilename()));
            if (extracted.fullText().length() < MIN_EXTRACTED_TEXT_LENGTH) {
                markFailed(documentId, SCANNED_PDF_MESSAGE, extracted.pageCount());
                return;
            }
            markReady(documentId, extracted);
        } catch (PdfExtractionLimitException ex) {
            log.warn("PDF extraction rejected documentId={} reason={}", documentId, ex.getMessage());
            markFailed(documentId, ex.getMessage(), null);
        } catch (IOException ex) {
            log.error("PDF extraction failed documentId={}", documentId, ex);
            markFailed(documentId, PDF_EXTRACTION_FAILED, null);
        } catch (RuntimeException ex) {
            // A worker thread must never leave the document stuck in PROCESSING.
            log.error("Unexpected failure while processing documentId={}", documentId, ex);
            markFailed(documentId, PDF_EXTRACTION_FAILED, null);
        }
    }

    /**
     * Fail every document still UPLOADED or PROCESSING. Called once at startup: nothing can be
     * working on them any more because the in-memory queue did not survive the restart.
     */
    public int markInterruptedDocumentsAsFailed() {
        List<String> storedFilenames = new ArrayList<>();
        Integer recovered = transactionTemplate.execute(status -> {
            List<Document> stuck = documentRepository.findByStatusIn(
                    List.of(DocumentStatus.UPLOADED, DocumentStatus.PROCESSING));
            for (Document document : stuck) {
                applyFailure(document, INTERRUPTED_REASON, null);
                storedFilenames.add(document.getStoredFilename());
            }
            documentRepository.saveAll(stuck);
            return stuck.size();
        });
        storedFilenames.forEach(this::deleteStoredFileQuietly);
        return recovered == null ? 0 : recovered;
    }

    private void markReady(Long documentId, ExtractedPdf extracted) {
        transactionTemplate.executeWithoutResult(status -> {
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document == null || document.getStatus() != DocumentStatus.PROCESSING) {
                // Deleted (or already handled) while extraction was running.
                return;
            }
            document.setPageCount(extracted.pageCount());
            document.setExtractedText(extracted.fullText());
            documentPageRepository.deleteByDocument(document);
            for (ExtractedPdfPage page : extracted.pages()) {
                documentPageRepository.save(new DocumentPage(document, page.pageNumber(), page.text()));
            }
            document.setStatus(DocumentStatus.READY);
            document.setProcessedAt(Instant.now());
            documentRepository.save(document);
        });
    }

    private void markFailed(Long documentId, String reason, Integer pageCount) {
        String storedFilename = transactionTemplate.execute(status -> {
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document == null) {
                return null;
            }
            applyFailure(document, reason, pageCount);
            documentRepository.save(document);
            return document.getStoredFilename();
        });
        if (storedFilename != null) {
            deleteStoredFileQuietly(storedFilename);
        }
    }

    private static void applyFailure(Document document, String reason, Integer pageCount) {
        if (pageCount != null) {
            document.setPageCount(pageCount);
        }
        document.setStatus(DocumentStatus.FAILED);
        document.setFailureReason(reason);
        document.setProcessedAt(Instant.now());
    }

    private void deleteStoredFileQuietly(String storedFilename) {
        try {
            fileStorageService.delete(storedFilename);
        } catch (IOException ex) {
            log.error("Failed to delete stored file after document failure storedFilename={}", storedFilename, ex);
        }
    }
}
