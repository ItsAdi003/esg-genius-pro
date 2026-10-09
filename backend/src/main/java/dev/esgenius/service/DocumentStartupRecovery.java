package dev.esgenius.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Documents are processed on an in-memory queue, so any document still UPLOADED or PROCESSING
 * when the application starts was interrupted by a crash or redeploy. Mark them FAILED so the
 * UI stops showing a spinner for work that will never finish.
 */
@Component
public class DocumentStartupRecovery {

    private static final Logger log = LoggerFactory.getLogger(DocumentStartupRecovery.class);

    private final DocumentProcessingService processingService;

    public DocumentStartupRecovery(DocumentProcessingService processingService) {
        this.processingService = processingService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedDocumentsOnStartup() {
        try {
            int recovered = processingService.markInterruptedDocumentsAsFailed();
            if (recovered > 0) {
                log.warn("Marked {} unfinished documents as failed after application restart", recovered);
            }
        } catch (RuntimeException ex) {
            log.error("Failed to recover interrupted documents; application will continue", ex);
        }
    }
}
