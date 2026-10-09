package dev.esgenius.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.StartAnalysisRequest;
import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Organization;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.service.compliance.ClassificationFailureCategory;
import dev.esgenius.service.compliance.ClassificationFailureHandler;
import dev.esgenius.service.compliance.ComplianceClassificationException;
import dev.esgenius.service.compliance.ComplianceClassificationProvider;
import dev.esgenius.service.compliance.ComplianceClassificationRequest;
import dev.esgenius.service.compliance.ComplianceClassificationResult;
import dev.esgenius.support.FullBrsrEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.ai.gemini.max-concurrent-classifications=4")
class ComplianceAnalysisConcurrencyParallelTest {

    private static final int CALL_DELAY_MS = 200;

    @Autowired
    private ComplianceAnalysisService complianceAnalysisService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ComplianceAnalysisRepository analysisRepository;

    @Autowired
    private RequirementAssessmentRepository assessmentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private ComplianceClassificationProvider classificationProvider;

    private Organization organization;

    @BeforeEach
    void setUp() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();
        organization = organizationRepository.findByTicker("INFY").orElseThrow();
    }

    @Test
    void fourWorkersClassifyFourteenRequirementsFasterThanSequential() {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        when(classificationProvider.classify(any())).thenAnswer(invocation -> {
            int current = inFlight.incrementAndGet();
            maxInFlight.updateAndGet(seen -> Math.max(seen, current));
            try {
                Thread.sleep(CALL_DELAY_MS);
                return covered();
            } finally {
                inFlight.decrementAndGet();
            }
        });

        long startedAt = System.nanoTime();
        ComplianceAnalysisResponse response = analyse(FullBrsrEvidence.text());
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;

        assertThat(maxInFlight.get()).isGreaterThan(1).isLessThanOrEqualTo(4);
        assertThat(elapsedMs).isLessThan(CALL_DELAY_MS * 8L);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.assessments()).hasSize(14);
        assertThat(response.assessments())
                .allMatch(assessment -> "COVERED".equals(assessment.assessmentStatus()));
    }

    @Test
    void quotaFailureStopsNewCallsAndCompletesRemainingAsHumanReview() throws Exception {
        AtomicInteger entered = new AtomicInteger();
        AtomicInteger callsAfterQuota = new AtomicInteger();
        CountDownLatch fourStarted = new CountDownLatch(4);
        CountDownLatch quotaMarked = new CountDownLatch(1);

        when(classificationProvider.classify(any())).thenAnswer(invocation -> {
            ComplianceClassificationRequest request = invocation.getArgument(0);
            if (request.runContext() != null && request.runContext().isQuotaExhausted()) {
                callsAfterQuota.incrementAndGet();
            }
            int n = entered.incrementAndGet();
            fourStarted.countDown();
            if (!fourStarted.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for four in-flight classifications");
            }
            if (n == 1) {
                request.runContext().markQuotaExhausted();
                quotaMarked.countDown();
                throw new ComplianceClassificationException(
                        ClassificationFailureCategory.QUOTA_EXHAUSTED,
                        "Gemini API returned HTTP 429",
                        false,
                        429,
                        1,
                        "Daily quota exhausted");
            }
            if (!quotaMarked.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for quota mark");
            }
            return covered();
        });

        ComplianceAnalysisResponse response = analyse(FullBrsrEvidence.text());

        assertThat(entered.get()).isEqualTo(4);
        assertThat(callsAfterQuota.get()).isZero();
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.failureReason()).isNull();
        assertThat(response.assessments()).hasSize(14);
        assertThat(response.assessments())
                .filteredOn(assessment -> "COVERED".equals(assessment.assessmentStatus()))
                .hasSize(3);
        assertThat(response.assessments())
                .filteredOn(assessment -> ClassificationFailureHandler.QUOTA_EXHAUSTED_EXPLANATION
                        .equals(assessment.explanation()))
                .hasSize(11);
    }

    @Test
    void unexpectedWorkerFailureMarksOnlyThatRequirement() {
        Logger logger = (Logger) LoggerFactory.getLogger(ComplianceAnalysisService.class);
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        try {
            AtomicInteger calls = new AtomicInteger();
            when(classificationProvider.classify(any())).thenAnswer(invocation -> {
                if (calls.incrementAndGet() == 1) {
                    throw new RuntimeException("boom");
                }
                return covered();
            });

            ComplianceAnalysisResponse response = analyse(FullBrsrEvidence.text());

            assertThat(response.status()).isEqualTo("COMPLETED");
            assertThat(response.failureReason()).isNull();
            assertThat(response.assessments()).hasSize(14);
            assertThat(response.assessments())
                    .filteredOn(assessment -> "COVERED".equals(assessment.assessmentStatus()))
                    .hasSize(13);
            assertThat(response.assessments())
                    .filteredOn(assessment -> ClassificationFailureHandler.GENERIC_FAILURE_EXPLANATION
                            .equals(assessment.explanation()))
                    .hasSize(1);
            assertThat(logAppender.list)
                    .anyMatch(event -> event.getFormattedMessage().contains("Unexpected classification failure")
                            && event.getThrowableProxy() != null
                            && event.getThrowableProxy().getMessage().contains("boom"));
        } finally {
            logger.detachAppender(logAppender);
        }
    }

    private ComplianceAnalysisResponse analyse(String extractedText) {
        Document document = createReadyDocument(extractedText);
        ComplianceAnalysisResponse created = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));
        return complianceAnalysisService.getAnalysis(created.id());
    }

    private static ComplianceClassificationResult covered() {
        return new ComplianceClassificationResult(AssessmentStatus.COVERED, 0.9, "OK", null, null);
    }

    private Document createReadyDocument(String extractedText) {
        Document document = new Document(
                organization,
                "esg-sample.pdf",
                "concurrency-parallel-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "concurrency-parallel-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(extractedText);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        return documentRepository.save(document);
    }
}
