package dev.esgenius.service;

import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.RequirementAssessmentResponse;
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
import dev.esgenius.service.compliance.ComplianceClassificationProvider;
import dev.esgenius.service.compliance.ComplianceClassificationResult;
import dev.esgenius.support.FullBrsrEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.ai.gemini.max-concurrent-classifications=1")
class ComplianceAnalysisConcurrencyTest {

    private static final int CALL_DELAY_MS = 40;

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
    void defaultConcurrencyRunsClassificationsOneAtATime() throws Exception {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        AtomicInteger calls = new AtomicInteger();
        when(classificationProvider.classify(any())).thenAnswer(invocation -> {
            calls.incrementAndGet();
            int current = inFlight.incrementAndGet();
            maxInFlight.updateAndGet(seen -> Math.max(seen, current));
            try {
                Thread.sleep(CALL_DELAY_MS);
                return new ComplianceClassificationResult(
                        AssessmentStatus.COVERED, 0.9, "OK", null, null);
            } finally {
                inFlight.decrementAndGet();
            }
        });

        Document document = createReadyDocument(FullBrsrEvidence.text());
        ComplianceAnalysisResponse created = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));
        ComplianceAnalysisResponse response = complianceAnalysisService.getAnalysis(created.id());

        assertThat(maxInFlight.get()).isEqualTo(1);
        assertThat(calls.get()).isEqualTo(14);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.assessments()).hasSize(14);
        assertThat(response.assessments())
                .allMatch(assessment -> "COVERED".equals(assessment.assessmentStatus()))
                .allMatch(assessment -> "OK".equals(assessment.explanation()))
                .extracting(RequirementAssessmentResponse::requirementCode)
                .contains("ENV-003", "ENV-004", "SOC-001", "GOV-001");
    }

    private Document createReadyDocument(String extractedText) {
        Document document = new Document(
                organization,
                "esg-sample.pdf",
                "concurrency-test-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "concurrency-test-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(extractedText);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        return documentRepository.save(document);
    }
}
