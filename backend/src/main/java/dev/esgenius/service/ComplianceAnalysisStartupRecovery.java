package dev.esgenius.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ComplianceAnalysisStartupRecovery {

    private static final Logger log = LoggerFactory.getLogger(ComplianceAnalysisStartupRecovery.class);
    static final String RESTART_FAILURE_REASON =
            "Analysis was interrupted when the application restarted.";

    private final ComplianceAnalysisPersistenceService persistenceService;

    public ComplianceAnalysisStartupRecovery(ComplianceAnalysisPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedAnalysesOnStartup() {
        try {
            int recovered = persistenceService.markInterruptedAnalysesAsFailed(RESTART_FAILURE_REASON);
            if (recovered > 0) {
                log.warn(
                        "Marked {} in-progress compliance analyses as failed after application restart",
                        recovered);
            }
        } catch (RuntimeException ex) {
            log.error("Failed to recover interrupted compliance analyses; application will continue", ex);
        }
    }

    public void markInterruptedAnalysesAsFailed() {
        persistenceService.markInterruptedAnalysesAsFailed(RESTART_FAILURE_REASON);
    }
}
