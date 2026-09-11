package dev.esgenius.service;

import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.RequirementAssessmentResponse;
import dev.esgenius.dto.StartAnalysisRequest;
import dev.esgenius.entity.*;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.service.compliance.ClassificationFailureCategory;
import dev.esgenius.service.compliance.ComplianceClassificationException;
import dev.esgenius.service.compliance.ComplianceClassificationProvider;
import dev.esgenius.service.compliance.ComplianceClassificationRequest;
import dev.esgenius.service.compliance.EvidenceContextExpander;
import dev.esgenius.service.compliance.ComplianceClassificationRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ComplianceAnalysisClassificationTest {

    @Autowired
    private ComplianceAnalysisService complianceAnalysisService;

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

    @MockBean
    private ComplianceClassificationProvider classificationProvider;

    private Organization organization;

    @BeforeEach
    void setUp() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();
        organization = organizationRepository.findByTicker("INFY").orElseThrow();
        reset(classificationProvider);
    }

    @Test
    void coveredClassificationPersistsAllFields() {
        when(classificationProvider.classify(any())).thenReturn(new ComplianceClassificationResult(
                AssessmentStatus.COVERED, 0.95, "Fully disclosed.", null, null));

        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        RequirementAssessmentResponse env003 = findAssessment(response, "ENV-003");
        assertThat(env003.assessmentStatus()).isEqualTo("COVERED");
        assertThat(env003.confidence()).isEqualTo(0.95);
        assertThat(env003.explanation()).isEqualTo("Fully disclosed.");
        assertThat(env003.retrievalScore()).isGreaterThan(0.0);
        assertThat(env003.evidenceText()).containsIgnoringCase("Scope 1");
        assertThat(env003.evidenceChunks()).isNotEmpty();
        verify(classificationProvider, atLeastOnce()).classify(any());
    }

    @Test
    void partiallyCoveredClassificationPersistsGapAndRecommendation() {
        when(classificationProvider.classify(any())).thenReturn(new ComplianceClassificationResult(
                AssessmentStatus.PARTIALLY_COVERED, 0.7, "Partial disclosure.",
                "Missing baseline year.", "Add baseline year."));

        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        RequirementAssessmentResponse assessment = response.assessments().get(0);
        assertThat(assessment.assessmentStatus()).isEqualTo("PARTIALLY_COVERED");
        assertThat(assessment.gap()).isEqualTo("Missing baseline year.");
        assertThat(assessment.recommendation()).isEqualTo("Add baseline year.");
    }

    @Test
    void notCoveredClassificationPersists() {
        when(classificationProvider.classify(any())).thenReturn(new ComplianceClassificationResult(
                AssessmentStatus.NOT_COVERED, 0.8, "Not found.", "No disclosure.", "Add disclosure."));

        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        assertThat(response.assessments())
                .allMatch(a -> a.assessmentStatus().equals("NOT_COVERED"));
    }

    @Test
    void humanReviewRequiredClassificationPersists() {
        when(classificationProvider.classify(any())).thenReturn(new ComplianceClassificationResult(
                AssessmentStatus.HUMAN_REVIEW_REQUIRED, 0.4, "Ambiguous evidence.",
                "Conflicting data.", "Manual review needed."));

        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        assertThat(response.assessments().stream().filter(a -> a.retrievalScore() > 0.0).toList())
                .isNotEmpty()
                .allMatch(a -> a.assessmentStatus().equals("HUMAN_REVIEW_REQUIRED"));
    }

    @Test
    void noEvidenceFoundSkipsGeminiAndProducesDeterministicResult() {
        Document document = createReadyDocument("Unrelated corporate boilerplate with no ESG content whatsoever.");

        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.requirementCount()).isEqualTo(14);
        assertThat(response.assessments())
                .allMatch(a -> a.assessmentStatus().equals("NOT_COVERED"));
        assertThat(response.assessments())
                .allMatch(a -> a.retrievalScore() == 0.0);
        assertThat(response.assessments())
                .allMatch(a -> a.explanation()
                        .equals("No relevant disclosure for this requirement was retrieved from the submitted document."));
        verify(classificationProvider, never()).classify(any());
    }

    @Test
    void providerFailureResultsInHumanReviewRequiredWithoutFailingAnalysis() {
        AtomicInteger callCount = new AtomicInteger();
        when(classificationProvider.classify(any())).thenAnswer(invocation -> {
            if (callCount.incrementAndGet() == 2) {
                throw new ComplianceClassificationException(
                        ClassificationFailureCategory.HTTP_RATE_LIMIT,
                        "Gemini API returned HTTP 429",
                        true,
                        429,
                        3,
                        "Rate limit exceeded");
            }
            return new ComplianceClassificationResult(
                    AssessmentStatus.COVERED, 0.9, "OK", null, null);
        });

        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.assessments()).hasSize(14);

        long humanReviewCount = response.assessments().stream()
                .filter(a -> "HUMAN_REVIEW_REQUIRED".equals(a.assessmentStatus()))
                .count();
        assertThat(humanReviewCount).isEqualTo(1);

        long coveredCount = response.assessments().stream()
                .filter(a -> "COVERED".equals(a.assessmentStatus()))
                .count();
        assertThat(coveredCount).isGreaterThan(0);
    }

    @Test
    void classificationReceivesExpandedEvidenceContext() {
        when(classificationProvider.classify(any())).thenAnswer(invocation -> {
            ComplianceClassificationRequest request = invocation.getArgument(0);
            String combined = String.join(" ", request.evidencePassages());
            assertThat(combined).contains("Scope 1");
            assertThat(combined.length()).isLessThanOrEqualTo(
                    EvidenceContextExpander.MAX_EXPANDED_PASSAGE_CHARS * 3);
            return new ComplianceClassificationResult(
                    AssessmentStatus.COVERED, 0.9, "OK", null, null);
        });

        String chunkText = String.join("\n",
                "Scope 1 boundary includes owned facilities.",
                "Total Scope 1 emissions were 15,000 metric tonnes CO2e for the reporting year.");
        Document document = createReadyDocument(chunkText);

        complianceAnalysisService.startAnalysis(document.getId(), new StartAnalysisRequest(null, "BRSR"));

        verify(classificationProvider, atLeastOnce()).classify(any());
    }

    @Test
    void evidenceAndRetrievalScoreRemainUnchangedAfterClassification() {
        when(classificationProvider.classify(any())).thenAnswer(invocation -> {
            ComplianceClassificationRequest request = invocation.getArgument(0);
            return new ComplianceClassificationResult(
                    AssessmentStatus.COVERED, 0.9,
                    "Classified " + request.requirementCode(), null, null);
        });

        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysisResponse created = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));
        ComplianceAnalysisResponse fetched = complianceAnalysisService.getAnalysis(created.id());

        RequirementAssessmentResponse env003 = findAssessment(fetched, "ENV-003");
        assertThat(env003.evidenceText()).containsIgnoringCase("Scope 1");
        assertThat(env003.retrievalScore()).isGreaterThan(0.0);
        assertThat(env003.evidenceChunks()).isNotEmpty();
    }

    @Test
    void allFourteenBrsrRequirementsStillProcess() {
        when(classificationProvider.classify(any())).thenReturn(new ComplianceClassificationResult(
                AssessmentStatus.COVERED, 0.85, "OK", null, null));

        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        assertThat(response.requirementCount()).isEqualTo(14);
        assertThat(response.assessments())
                .extracting(RequirementAssessmentResponse::requirementCode)
                .contains("ENV-003", "ENV-004", "SOC-001", "GOV-001");
    }

    private RequirementAssessmentResponse findAssessment(ComplianceAnalysisResponse response, String code) {
        return response.assessments().stream()
                .filter(a -> code.equals(a.requirementCode()))
                .findFirst()
                .orElseThrow();
    }

    private Document createReadyDocument(String extractedText) {
        Document document = new Document(
                organization,
                "esg-sample.pdf",
                "classify-test-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "classify-test-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(extractedText);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        return documentRepository.save(document);
    }
}
