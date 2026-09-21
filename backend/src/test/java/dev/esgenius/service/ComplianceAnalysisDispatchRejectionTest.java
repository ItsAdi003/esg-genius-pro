package dev.esgenius.service;

import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.StartAnalysisRequest;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Organization;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.support.ComplianceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(ComplianceAnalysisDispatchRejectionTest.RejectingExecutorConfig.class)
class ComplianceAnalysisDispatchRejectionTest {

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

    private Organization organization;

    @BeforeEach
    void setUp() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();
        organization = organizationRepository.findByTicker("INFY").orElseThrow();
    }

    @Test
    void rejectedExecutorTaskMarksAnalysisFailed() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        ComplianceAnalysisResponse created = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        assertThat(created.status()).isEqualTo("IN_PROGRESS");

        ComplianceAnalysisResponse fetched = complianceAnalysisService.getAnalysis(created.id());
        assertThat(fetched.status()).isEqualTo("FAILED");
        assertThat(fetched.failureReason())
                .isEqualTo(ComplianceAnalysisPersistenceService.DISPATCH_FAILURE_REASON);
        assertThat(fetched.completedAt()).isNotNull();
    }

    @TestConfiguration
    static class RejectingExecutorConfig {

        @Bean(name = "complianceAnalysisExecutor")
        TaskExecutor complianceAnalysisExecutor() {
            return task -> {
                throw new TaskRejectedException("executor queue is full");
            };
        }
    }

    private Document createReadyDocument(String extractedText) {
        Document document = new Document(
                organization,
                "esg-sample.pdf",
                "reject-test-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "reject-test-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(extractedText);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        return documentRepository.save(document);
    }
}
