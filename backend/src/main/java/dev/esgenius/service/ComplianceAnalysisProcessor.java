package dev.esgenius.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionException;

@Component
public class ComplianceAnalysisProcessor {

    private static final Logger log = LoggerFactory.getLogger(ComplianceAnalysisProcessor.class);

    private final TaskExecutor complianceAnalysisExecutor;
    private final ComplianceAnalysisService complianceAnalysisService;
    private final ComplianceAnalysisPersistenceService persistenceService;

    public ComplianceAnalysisProcessor(
            @Qualifier("complianceAnalysisExecutor") TaskExecutor complianceAnalysisExecutor,
            ComplianceAnalysisService complianceAnalysisService,
            ComplianceAnalysisPersistenceService persistenceService) {
        this.complianceAnalysisExecutor = complianceAnalysisExecutor;
        this.complianceAnalysisService = complianceAnalysisService;
        this.persistenceService = persistenceService;
    }

    public void processAnalysis(Long analysisId) {
        try {
            complianceAnalysisExecutor.execute(() -> {
                try {
                    complianceAnalysisService.executeAnalysis(analysisId);
                } catch (Exception ex) {
                    log.error("Compliance analysis {} failed", analysisId, ex);
                    persistenceService.markAnalysisFailed(analysisId, ex);
                }
            });
        } catch (RejectedExecutionException ex) {
            log.error("Compliance analysis {} was not accepted by the executor", analysisId, ex);
            persistenceService.markAnalysisFailed(
                    analysisId,
                    new IllegalStateException(ComplianceAnalysisPersistenceService.DISPATCH_FAILURE_REASON, ex));
        }
    }
}
