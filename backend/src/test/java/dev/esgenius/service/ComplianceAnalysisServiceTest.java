package dev.esgenius.service;

import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.ComplianceAnalysisSummaryResponse;
import dev.esgenius.dto.RequirementAssessmentResponse;
import dev.esgenius.dto.StartAnalysisRequest;
import dev.esgenius.entity.*;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.FrameworkRequirementRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.support.ComplianceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ComplianceAnalysisServiceTest {

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

    @Autowired
    private FrameworkRequirementRepository requirementRepository;

    private Organization organization;
    private Framework brsrFramework;

    @BeforeEach
    void setUp() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();

        organization = organizationRepository.findByTicker("INFY").orElseThrow();
        brsrFramework = frameworkRepository.findByCode("BRSR").orElseThrow();
    }

    @Test
    void startAnalysisProcessesAllSeededRequirements() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.frameworkCode()).isEqualTo("BRSR");
        assertThat(response.requirementCount()).isEqualTo(14);
        assertThat(response.assessments()).hasSize(14);
        assertThat(response.assessments())
                .extracting(RequirementAssessmentResponse::requirementCode)
                .contains("ENV-003", "ENV-004", "SOC-001", "GOV-001");
    }

    @Test
    void emissionsRequirementRetrievesEmissionsEvidence() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(brsrFramework.getId(), null));

        RequirementAssessmentResponse scope1 = response.assessments().stream()
                .filter(a -> "ENV-003".equals(a.requirementCode()))
                .findFirst()
                .orElseThrow();

        assertThat(scope1.assessmentStatus()).isEqualTo("COVERED");
        assertThat(scope1.confidence()).isNotNull();
        assertThat(scope1.explanation()).isNotBlank();
        assertThat(scope1.retrievalScore()).isGreaterThan(0.0);
        assertThat(scope1.evidenceText()).containsIgnoringCase("Scope 1");
        assertThat(scope1.evidenceChunks()).isNotEmpty();
    }

    @Test
    void rejectsNonReadyDocument() {
        Document document = createDocumentWithStatus(DocumentStatus.PROCESSING, ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                complianceAnalysisService.startAnalysis(
                        document.getId(), new StartAnalysisRequest(null, "BRSR")));

        assertThat(ex.getMessage()).contains("READY");
    }

    @Test
    void missingDocumentReturns404() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                complianceAnalysisService.startAnalysis(
                        99999L, new StartAnalysisRequest(null, "BRSR")));

        assertThat(ex.getMessage()).contains("Document not found");
    }

    @Test
    void missingFrameworkReturns404() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                complianceAnalysisService.startAnalysis(
                        document.getId(), new StartAnalysisRequest(99999L, null)));

        assertThat(ex.getMessage()).contains("Framework not found");
    }

    @Test
    void listAnalysesForDocumentReturnsNewestFirst() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        ComplianceAnalysis older = new ComplianceAnalysis(document, brsrFramework);
        older.setStatus(AnalysisStatus.COMPLETED);
        ReflectionTestUtils.setField(older, "startedAt", Instant.now().minus(2, ChronoUnit.HOURS));
        older.setCompletedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        analysisRepository.save(older);

        ComplianceAnalysis newer = new ComplianceAnalysis(document, brsrFramework);
        newer.setStatus(AnalysisStatus.COMPLETED);
        ReflectionTestUtils.setField(newer, "startedAt", Instant.now().minus(1, ChronoUnit.HOURS));
        newer.setCompletedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        analysisRepository.save(newer);

        List<ComplianceAnalysisSummaryResponse> summaries =
                complianceAnalysisService.listAnalysesForDocument(document.getId());

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).id()).isEqualTo(newer.getId());
        assertThat(summaries.get(1).id()).isEqualTo(older.getId());
    }

    @Test
    void listAnalysesForDocumentReturnsEmptyListWhenNoAnalyses() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        assertThat(complianceAnalysisService.listAnalysesForDocument(document.getId())).isEmpty();
    }

    @Test
    void listAnalysesForDocumentThrowsWhenDocumentMissing() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                complianceAnalysisService.listAnalysesForDocument(99999L));

        assertThat(ex.getMessage()).contains("Document not found");
    }

    @Test
    void listAnalysesSummaryMapsMetadataAndStatusCounts() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysis analysis = new ComplianceAnalysis(document, brsrFramework);
        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setCompletedAt(Instant.now());
        analysisRepository.save(analysis);

        FrameworkRequirement covered = requirementRepository.findByFramework(brsrFramework).stream()
                .filter(r -> "ENV-003".equals(r.getRequirementCode()))
                .findFirst()
                .orElseThrow();
        FrameworkRequirement notCovered = requirementRepository.findByFramework(brsrFramework).stream()
                .filter(r -> "SOC-002".equals(r.getRequirementCode()))
                .findFirst()
                .orElseThrow();
        FrameworkRequirement humanReview = requirementRepository.findByFramework(brsrFramework).stream()
                .filter(r -> "GOV-001".equals(r.getRequirementCode()))
                .findFirst()
                .orElseThrow();
        FrameworkRequirement legacyEvidence = requirementRepository.findByFramework(brsrFramework).stream()
                .filter(r -> "ENV-004".equals(r.getRequirementCode()))
                .findFirst()
                .orElseThrow();
        FrameworkRequirement legacyNoEvidence = requirementRepository.findByFramework(brsrFramework).stream()
                .filter(r -> "SOC-001".equals(r.getRequirementCode()))
                .findFirst()
                .orElseThrow();
        FrameworkRequirement partial = requirementRepository.findByFramework(brsrFramework).stream()
                .filter(r -> "ENV-001".equals(r.getRequirementCode()))
                .findFirst()
                .orElseThrow();

        saveAssessment(analysis, covered, AssessmentStatus.COVERED);
        saveAssessment(analysis, partial, AssessmentStatus.PARTIALLY_COVERED);
        saveAssessment(analysis, notCovered, AssessmentStatus.NOT_COVERED);
        saveAssessment(analysis, humanReview, AssessmentStatus.HUMAN_REVIEW_REQUIRED);
        saveAssessment(analysis, legacyEvidence, AssessmentStatus.EVIDENCE_RETRIEVED);
        saveAssessment(analysis, legacyNoEvidence, AssessmentStatus.NO_EVIDENCE_FOUND);

        ComplianceAnalysisSummaryResponse summary = complianceAnalysisService
                .listAnalysesForDocument(document.getId())
                .get(0);

        assertThat(summary.id()).isEqualTo(analysis.getId());
        assertThat(summary.documentId()).isEqualTo(document.getId());
        assertThat(summary.frameworkCode()).isEqualTo("BRSR");
        assertThat(summary.frameworkName()).isNotBlank();
        assertThat(summary.status()).isEqualTo("COMPLETED");
        assertThat(summary.requirementCount()).isEqualTo(6);
        assertThat(summary.coveredCount()).isEqualTo(1);
        assertThat(summary.partiallyCoveredCount()).isEqualTo(1);
        assertThat(summary.notCoveredCount()).isEqualTo(1);
        assertThat(summary.humanReviewRequiredCount()).isEqualTo(1);
        assertThat(summary.evidenceRetrievedCount()).isEqualTo(1);
        assertThat(summary.noEvidenceFoundCount()).isEqualTo(1);
    }

    @Test
    void getAnalysisReturnsPersistedRetrievalResults() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        ComplianceAnalysisResponse created = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        ComplianceAnalysisResponse fetched = complianceAnalysisService.getAnalysis(created.id());

        assertThat(fetched.id()).isEqualTo(created.id());
        assertThat(fetched.assessments()).hasSize(14);
        assertThat(fetched.assessments().get(0).requirementTitle()).isNotBlank();
        assertThat(fetched.assessments().get(0).category()).isIn("ENVIRONMENTAL", "SOCIAL", "GOVERNANCE");
    }

    private void saveAssessment(
            ComplianceAnalysis analysis, FrameworkRequirement requirement, AssessmentStatus status) {
        RequirementAssessment assessment = new RequirementAssessment(analysis, requirement);
        assessment.setAssessmentStatus(status);
        assessmentRepository.save(assessment);
    }

    private Document createReadyDocument(String extractedText) {
        return createDocumentWithStatus(DocumentStatus.READY, extractedText);
    }

    private Document createDocumentWithStatus(DocumentStatus status, String extractedText) {
        Document document = new Document(
                organization,
                "esg-sample.pdf",
                "test-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "test-" + System.nanoTime() + ".pdf");
        document.setStatus(status);
        document.setExtractedText(extractedText);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        return documentRepository.save(document);
    }
}
