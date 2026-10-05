package dev.esgenius.controller;

import dev.esgenius.entity.AnalysisStatus;
import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.entity.ComplianceAnalysis;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Framework;
import dev.esgenius.entity.FrameworkRequirement;
import dev.esgenius.entity.FrameworkStatus;
import dev.esgenius.entity.Organization;
import dev.esgenius.entity.RequirementAssessment;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.FrameworkRequirementRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.support.SqlStatementCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards statement counts on read endpoints whose queries were reduced.
 * Caps sit one statement above the optimized count, and still below the
 * pre-fix counts (analysis 16, company ESG 10, compare 19, document detail 2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=dev.esgenius.support.SqlStatementCounter"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReadEndpointQueryCountTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FrameworkRepository frameworkRepository;

    @Autowired
    private FrameworkRequirementRepository requirementRepository;

    @Autowired
    private ComplianceAnalysisRepository analysisRepository;

    @Autowired
    private RequirementAssessmentRepository assessmentRepository;

    private Long organizationId;
    private Long frameworkId;
    private Long documentId;
    private Long analysisId;
    private Long companyAId;
    private Long companyBId;

    @BeforeEach
    void setUp() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();

        Organization organization = organizationRepository.findByTicker("INFY").orElseThrow();
        organizationId = organization.getId();
        Organization tcs = organizationRepository.findByTicker("TCS").orElseThrow();
        companyAId = organizationId;
        companyBId = tcs.getId();

        Framework framework = frameworkRepository.findByCode("BRSR").orElseThrow();
        frameworkId = framework.getId();

        Document document = new Document(
                organization,
                "seed-measure.pdf",
                "seed-measure-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                2048L,
                "seed-measure.pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText("measured document text");
        document.setPageCount(2);
        document.setProcessedAt(Instant.now());
        documentId = documentRepository.save(document).getId();

        ComplianceAnalysis analysis = new ComplianceAnalysis(document, framework);
        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setCompletedAt(Instant.now());
        analysis = analysisRepository.save(analysis);
        analysisId = analysis.getId();

        List<FrameworkRequirement> requirements = requirementRepository.findByFramework(framework);
        for (FrameworkRequirement requirement : requirements) {
            RequirementAssessment assessment = new RequirementAssessment(analysis, requirement);
            assessment.setAssessmentStatus(AssessmentStatus.COVERED);
            assessment.setConfidence(0.9);
            assessment.setExplanation("covered");
            assessment.setRetrievalScore(0.8);
            assessment.setEvidenceText("evidence for " + requirement.getRequirementCode());
            assessment.setEvidenceChunks("[{\"chunkIndex\":0,\"pageNumber\":1,\"text\":\"evidence\",\"score\":0.8}]");
            assessmentRepository.save(assessment);
        }
    }

    @Test
    @Order(1)
    void fixedReadEndpointsStayWithinStatementBudget() throws Exception {
        mockMvc.perform(get("/api/v1/frameworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BRSR"))
                .andExpect(jsonPath("$[0].requirementCount").value(14));
        assertThat(countOf("GET /api/v1/frameworks", "/api/v1/frameworks")).isLessThanOrEqualTo(3);
        assertThat(countOf("GET /api/v1/frameworks/{id}/requirements",
                "/api/v1/frameworks/" + frameworkId + "/requirements")).isLessThanOrEqualTo(2);
        assertThat(countOf("GET /api/v1/documents?organizationId=",
                "/api/v1/documents?organizationId=" + organizationId)).isLessThanOrEqualTo(2);

        assertThat(countOf("GET /api/v1/documents/{id}", "/api/v1/documents/" + documentId))
                .isLessThanOrEqualTo(1);
        mockMvc.perform(get("/api/v1/documents/" + documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("Infosys Limited"))
                .andExpect(jsonPath("$.extractedText").value("measured document text"));

        assertThat(countOf("GET /api/v1/documents/{id}/analyses",
                "/api/v1/documents/" + documentId + "/analyses")).isLessThanOrEqualTo(3);
        assertThat(countOf("GET /api/v1/analyses/{id}", "/api/v1/analyses/" + analysisId))
                .isLessThanOrEqualTo(4);
        mockMvc.perform(get("/api/v1/analyses/" + analysisId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirementCount").value(14))
                .andExpect(jsonPath("$.assessments.length()").value(14))
                .andExpect(jsonPath("$.assessments[0].evidenceText").exists());

        assertThat(countOf("GET /api/v1/companies", "/api/v1/companies")).isLessThanOrEqualTo(1);
        assertThat(countOf("GET /api/v1/companies/{id}/esg", "/api/v1/companies/" + companyAId + "/esg"))
                .isLessThanOrEqualTo(6);
        mockMvc.perform(get("/api/v1/companies/" + companyAId + "/esg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.materialIssues.length()").value(5))
                .andExpect(jsonPath("$.ticker").value("INFY"));

        assertThat(countOf(
                "GET /api/v1/companies/compare",
                "/api/v1/companies/compare?companyA=" + companyAId + "&companyB=" + companyBId))
                .isLessThanOrEqualTo(13);
        mockMvc.perform(get("/api/v1/companies/compare")
                        .param("companyA", companyAId.toString())
                        .param("companyB", companyBId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyA.ticker").value("INFY"))
                .andExpect(jsonPath("$.companyB.ticker").value("TCS"))
                .andExpect(jsonPath("$.comparisonInsight").value(org.hamcrest.Matchers.containsString("Infosys Limited")));
    }

    @Test
    @Order(2)
    void frameworkListCountDoesNotScalePerFramework() throws Exception {
        frameworkRepository.save(new Framework(
                "ZZ1", "Extra One", "Extra Framework One", "IN", "1", FrameworkStatus.PLANNED));
        frameworkRepository.save(new Framework(
                "ZZ2", "Extra Two", "Extra Framework Two", "IN", "1", FrameworkStatus.PLANNED));
        frameworkRepository.save(new Framework(
                "ZZ3", "Extra Three", "Extra Framework Three", "IN", "1", FrameworkStatus.PLANNED));

        // 4 frameworks. Per-framework requirement loads would be 5 statements.
        assertThat(countOf("GET /api/v1/frameworks (4 frameworks)", "/api/v1/frameworks"))
                .isLessThanOrEqualTo(3);
        mockMvc.perform(get("/api/v1/frameworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='BRSR')].requirementCount").value(org.hamcrest.Matchers.hasItem(14)));
    }

    private int countOf(String label, String url) throws Exception {
        SqlStatementCounter.reset();
        mockMvc.perform(get(url)).andExpect(status().isOk());
        int statements = SqlStatementCounter.count();
        System.out.println("QUERY_COUNT " + label + " = " + statements);
        return statements;
    }
}
