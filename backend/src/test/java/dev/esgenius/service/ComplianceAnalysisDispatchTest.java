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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(ComplianceAnalysisDispatchTest.DeferredExecutorConfig.class)
class ComplianceAnalysisDispatchTest {

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

    @Autowired
    @Qualifier("complianceAnalysisExecutor")
    private TaskExecutor complianceAnalysisExecutor;

    private Organization organization;

    @BeforeEach
    void setUp() {
        DeferredExecutorConfig.clearPendingTasks();
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();
        organization = organizationRepository.findByTicker("INFY").orElseThrow();
    }

    @Test
    void startAnalysisReturnsWithoutWaitingForClassificationCompletion() {
        assertThat(complianceAnalysisExecutor).isInstanceOf(DeferredExecutorConfig.DeferredTaskExecutor.class);

        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        ComplianceAnalysisResponse created = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        assertThat(created.status()).isEqualTo("IN_PROGRESS");
        assertThat(created.assessments()).isEmpty();
        assertThat(DeferredExecutorConfig.pendingTaskCount()).isEqualTo(1);

        ComplianceAnalysisResponse stillRunning = complianceAnalysisService.getAnalysis(created.id());
        assertThat(stillRunning.status()).isEqualTo("IN_PROGRESS");
        assertThat(stillRunning.assessments()).isEmpty();
    }

    @TestConfiguration
    static class DeferredExecutorConfig {

        private static final List<Runnable> PENDING_TASKS = new ArrayList<>();

        @Bean(name = "complianceAnalysisExecutor")
        TaskExecutor complianceAnalysisExecutor() {
            return new DeferredTaskExecutor();
        }

        static void clearPendingTasks() {
            PENDING_TASKS.clear();
        }

        static int pendingTaskCount() {
            return PENDING_TASKS.size();
        }

        static final class DeferredTaskExecutor implements TaskExecutor {
            @Override
            public void execute(Runnable task) {
                PENDING_TASKS.add(task);
            }
        }
    }

    private Document createReadyDocument(String extractedText) {
        Document document = new Document(
                organization,
                "esg-sample.pdf",
                "dispatch-test-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "dispatch-test-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(extractedText);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        return documentRepository.save(document);
    }
}
