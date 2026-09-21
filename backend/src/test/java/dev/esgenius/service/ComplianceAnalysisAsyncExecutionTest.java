package dev.esgenius.service;

import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.StartAnalysisRequest;
import dev.esgenius.entity.*;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.service.compliance.ComplianceClassificationProvider;
import dev.esgenius.service.compliance.ComplianceClassificationResult;
import dev.esgenius.support.ComplianceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ComplianceAnalysisAsyncExecutionTest {

    @Autowired
    private ComplianceAnalysisService complianceAnalysisService;

    @Autowired
    private ComplianceAnalysisProcessor complianceAnalysisProcessor;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FrameworkRepository frameworkRepository;

    @Autowired
    private ComplianceAnalysisRepository analysisRepository;

    @Autowired
    private RequirementAssessmentRepository assessmentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ComplianceAnalysisPersistenceService persistenceService;

    @MockBean
    private ComplianceClassificationProvider classificationProvider;

    private Organization organization;

    @BeforeEach
    void setUp() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();
        organization = organizationRepository.findByTicker("INFY").orElseThrow();
        when(classificationProvider.classify(any())).thenReturn(new ComplianceClassificationResult(
                AssessmentStatus.COVERED, 0.85, "OK", null, null));
    }

    @Test
    void startAnalysisCreatesInProgressRecord() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        ComplianceAnalysisResponse created = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        assertThat(created.status()).isEqualTo("IN_PROGRESS");
        assertThat(created.assessments()).isEmpty();
    }

    @Test
    void executeAnalysisCompletesPersistedAnalysis() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysis analysis = new ComplianceAnalysis(document, frameworkRepository.findByCode("BRSR").orElseThrow());
        analysisRepository.save(analysis);

        complianceAnalysisService.executeAnalysis(analysis.getId());

        ComplianceAnalysisResponse completed = complianceAnalysisService.getAnalysis(analysis.getId());
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.assessments()).hasSize(14);
        assertThat(completed.completedAt()).isNotNull();
    }

    @Test
    void executeAnalysisMarksFailedOnUnhandledProcessingError() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysis analysis = new ComplianceAnalysis(document, frameworkRepository.findByCode("BRSR").orElseThrow());
        analysisRepository.save(analysis);

        when(classificationProvider.classify(any())).thenThrow(new IllegalStateException("simulated pipeline failure"));

        complianceAnalysisProcessor.processAnalysis(analysis.getId());

        ComplianceAnalysisResponse failed = complianceAnalysisService.getAnalysis(analysis.getId());
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.failureReason()).contains("simulated pipeline failure");
        assertThat(failed.completedAt()).isNotNull();
    }

    @Test
    void persistedAssessmentsSurviveLaterWorkerFailure() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysis analysis = new ComplianceAnalysis(
                document, frameworkRepository.findByCode("BRSR").orElseThrow());
        analysisRepository.save(analysis);

        AtomicInteger classifyCalls = new AtomicInteger();
        when(classificationProvider.classify(any())).thenAnswer(invocation -> {
            if (classifyCalls.incrementAndGet() >= 2) {
                throw new IllegalStateException("failure after partial persistence");
            }
            return new ComplianceClassificationResult(AssessmentStatus.COVERED, 0.85, "OK", null, null);
        });

        complianceAnalysisProcessor.processAnalysis(analysis.getId());

        ComplianceAnalysisResponse failed = complianceAnalysisService.getAnalysis(analysis.getId());
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.failureReason()).contains("failure after partial persistence");
        assertThat(failed.assessments()).isNotEmpty();
        assertThat(assessmentRepository.findByAnalysisOrderByFrameworkRequirement_RequirementCodeAsc(
                analysisRepository.findById(analysis.getId()).orElseThrow())).isNotEmpty();
    }

    @Test
    void getAnalysisExposesInProgressStatus() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysis analysis = new ComplianceAnalysis(document, frameworkRepository.findByCode("BRSR").orElseThrow());
        analysisRepository.save(analysis);

        ComplianceAnalysisResponse fetched = complianceAnalysisService.getAnalysis(analysis.getId());

        assertThat(fetched.status()).isEqualTo("IN_PROGRESS");
        assertThat(fetched.assessments()).isEmpty();
    }

    @Test
    void completedAnalysesRemainReadableAfterAsyncProcessing() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        ComplianceAnalysisResponse created = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));
        ComplianceAnalysisResponse completed = complianceAnalysisService.getAnalysis(created.id());

        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.assessments()).hasSize(14);

        ComplianceAnalysisResponse fetchedAgain = complianceAnalysisService.getAnalysis(created.id());
        assertThat(fetchedAgain.status()).isEqualTo("COMPLETED");
        assertThat(fetchedAgain.assessments()).hasSize(14);
    }

    @Test
    void startAnalysisRejectsDuplicateInProgressAnalysis() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        Framework framework = frameworkRepository.findByCode("BRSR").orElseThrow();

        ComplianceAnalysis inProgress = new ComplianceAnalysis(document, framework);
        analysisRepository.save(inProgress);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                complianceAnalysisService.startAnalysis(
                        document.getId(), new StartAnalysisRequest(null, "BRSR")));

        assertThat(ex.getMessage()).contains("already in progress");
    }

    @Test
    void markAnalysisFailedPersistsSanitizedReason() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysis analysis = new ComplianceAnalysis(document, frameworkRepository.findByCode("BRSR").orElseThrow());
        analysisRepository.save(analysis);

        persistenceService.markAnalysisFailed(
                analysis.getId(), new IllegalStateException("Bearer secret-token leaked"));

        ComplianceAnalysisResponse failed = complianceAnalysisService.getAnalysis(analysis.getId());
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.failureReason()).isEqualTo(ComplianceAnalysisPersistenceService.GENERIC_FAILURE_REASON);
    }

    private Document createReadyDocument(String extractedText) {
        Document document = new Document(
                organization,
                "esg-sample.pdf",
                "async-test-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "async-test-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(extractedText);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        return documentRepository.save(document);
    }
}
