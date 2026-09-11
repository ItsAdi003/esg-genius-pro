package dev.esgenius.entity;

import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.RequirementAssessmentResponse;
import dev.esgenius.dto.StartAnalysisRequest;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentPageRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRequirementRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.service.ComplianceAnalysisService;
import dev.esgenius.support.ComplianceTestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AssessmentStatusBackwardCompatibilityTest {

    @Autowired
    private ComplianceAnalysisService complianceAnalysisService;

    @Autowired
    private ComplianceAnalysisRepository analysisRepository;

    @Autowired
    private RequirementAssessmentRepository assessmentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentPageRepository documentPageRepository;

    @Autowired
    private FrameworkRepository frameworkRepository;

    @Autowired
    private FrameworkRequirementRepository requirementRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization organization;
    private Framework brsrFramework;

    @BeforeEach
    void setUp() {
        cleanData();
        organization = organizationRepository.findByTicker("INFY").orElseThrow();
        brsrFramework = frameworkRepository.findByCode("BRSR").orElseThrow();
    }

    @AfterEach
    void tearDown() {
        cleanData();
    }

    private void cleanData() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentPageRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Test
    void legacyPersistedStatusesRemainReadable() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        ComplianceAnalysis analysis = new ComplianceAnalysis(document, brsrFramework);
        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setCompletedAt(Instant.now());
        analysisRepository.save(analysis);

        FrameworkRequirement withEvidence = requirementRepository.findByFramework(brsrFramework).stream()
                .filter(r -> "ENV-003".equals(r.getRequirementCode()))
                .findFirst()
                .orElseThrow();
        FrameworkRequirement withoutEvidence = requirementRepository.findByFramework(brsrFramework).stream()
                .filter(r -> "SOC-002".equals(r.getRequirementCode()))
                .findFirst()
                .orElseThrow();

        RequirementAssessment evidenceAssessment = new RequirementAssessment(analysis, withEvidence);
        evidenceAssessment.setAssessmentStatus(AssessmentStatus.EVIDENCE_RETRIEVED);
        evidenceAssessment.setRetrievalScore(47.5);
        evidenceAssessment.setEvidenceText("Legacy Scope 1 evidence text.");
        analysis.addAssessment(evidenceAssessment);

        RequirementAssessment noEvidenceAssessment = new RequirementAssessment(analysis, withoutEvidence);
        noEvidenceAssessment.setAssessmentStatus(AssessmentStatus.NO_EVIDENCE_FOUND);
        noEvidenceAssessment.setRetrievalScore(0.0);
        analysis.addAssessment(noEvidenceAssessment);

        analysisRepository.save(analysis);

        ComplianceAnalysisResponse response = complianceAnalysisService.getAnalysis(analysis.getId());

        RequirementAssessmentResponse legacyEvidence = response.assessments().stream()
                .filter(a -> "ENV-003".equals(a.requirementCode()))
                .findFirst()
                .orElseThrow();
        RequirementAssessmentResponse legacyNoEvidence = response.assessments().stream()
                .filter(a -> "SOC-002".equals(a.requirementCode()))
                .findFirst()
                .orElseThrow();

        assertThat(legacyEvidence.assessmentStatus()).isEqualTo("EVIDENCE_RETRIEVED");
        assertThat(legacyEvidence.evidenceText()).isEqualTo("Legacy Scope 1 evidence text.");
        assertThat(legacyNoEvidence.assessmentStatus()).isEqualTo("NO_EVIDENCE_FOUND");
        assertThat(legacyNoEvidence.retrievalScore()).isEqualTo(0.0);
    }

    @Test
    void newAnalysesPersistOnlyFinalClassificationStatuses() {
        Document document = createReadyDocument(ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        Set<String> statuses = response.assessments().stream()
                .map(RequirementAssessmentResponse::assessmentStatus)
                .collect(Collectors.toSet());

        assertThat(statuses).doesNotContain("EVIDENCE_RETRIEVED", "NO_EVIDENCE_FOUND");
        assertThat(statuses).isSubsetOf(Set.of(
                "COVERED", "PARTIALLY_COVERED", "NOT_COVERED", "HUMAN_REVIEW_REQUIRED"));
    }

    @Test
    void enumValueOfSupportsLegacyDatabaseStrings() {
        assertThat(AssessmentStatus.valueOf("EVIDENCE_RETRIEVED")).isEqualTo(AssessmentStatus.EVIDENCE_RETRIEVED);
        assertThat(AssessmentStatus.valueOf("NO_EVIDENCE_FOUND")).isEqualTo(AssessmentStatus.NO_EVIDENCE_FOUND);
        assertThat(AssessmentStatus.EVIDENCE_RETRIEVED.isLegacyRetrievalStatus()).isTrue();
        assertThat(AssessmentStatus.COVERED.isClassificationStatus()).isTrue();
    }

    private Document createReadyDocument(String extractedText) {
        Document document = new Document(
                organization,
                "legacy-compat.pdf",
                "legacy-compat-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "legacy-compat-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(extractedText);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        return documentRepository.save(document);
    }
}
