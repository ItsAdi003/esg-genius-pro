package dev.esgenius.service;

import dev.esgenius.entity.AnalysisStatus;
import dev.esgenius.entity.ComplianceAnalysis;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Framework;
import dev.esgenius.entity.Organization;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ComplianceAnalysisStartupRecoveryTest {

    @Autowired
    private ComplianceAnalysisStartupRecovery startupRecovery;

    @Autowired
    private ComplianceAnalysisRepository analysisRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FrameworkRepository frameworkRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void setUp() {
        analysisRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Test
    void startupRecoveryMarksInterruptedInProgressAnalysesAsFailed() {
        Organization organization = organizationRepository.findByTicker("INFY").orElseThrow();
        Framework framework = frameworkRepository.findByCode("BRSR").orElseThrow();
        Document document = new Document(
                organization,
                "esg-sample.pdf",
                "recovery-test-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "recovery-test-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText("sample");
        document.setProcessedAt(Instant.now());
        documentRepository.save(document);

        ComplianceAnalysis interrupted = new ComplianceAnalysis(document, framework);
        analysisRepository.save(interrupted);

        startupRecovery.markInterruptedAnalysesAsFailed();

        ComplianceAnalysis recovered = analysisRepository.findById(interrupted.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(recovered.getFailureReason())
                .isEqualTo(ComplianceAnalysisStartupRecovery.RESTART_FAILURE_REASON);
        assertThat(recovered.getCompletedAt()).isNotNull();
    }

    @Test
    void startupRecoveryLeavesCompletedAndFailedAnalysesUnchanged() {
        Organization organization = organizationRepository.findByTicker("INFY").orElseThrow();
        Framework framework = frameworkRepository.findByCode("BRSR").orElseThrow();
        Document document = new Document(
                organization,
                "esg-sample.pdf",
                "recovery-keep-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "recovery-keep-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText("sample");
        document.setProcessedAt(Instant.now());
        documentRepository.save(document);

        ComplianceAnalysis completed = new ComplianceAnalysis(document, framework);
        completed.setStatus(AnalysisStatus.COMPLETED);
        completed.setCompletedAt(Instant.now());
        analysisRepository.save(completed);

        ComplianceAnalysis failed = new ComplianceAnalysis(document, framework);
        failed.setStatus(AnalysisStatus.FAILED);
        failed.setFailureReason("original failure");
        failed.setCompletedAt(Instant.now());
        analysisRepository.save(failed);

        startupRecovery.markInterruptedAnalysesAsFailed();

        ComplianceAnalysis stillCompleted = analysisRepository.findById(completed.getId()).orElseThrow();
        assertThat(stillCompleted.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(stillCompleted.getFailureReason()).isNull();

        ComplianceAnalysis stillFailed = analysisRepository.findById(failed.getId()).orElseThrow();
        assertThat(stillFailed.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(stillFailed.getFailureReason()).isEqualTo("original failure");
    }
}
