package dev.esgenius.controller;

import dev.esgenius.entity.*;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.support.ComplianceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComplianceAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FrameworkRepository frameworkRepository;

    @Autowired
    private ComplianceAnalysisRepository analysisRepository;

    @Autowired
    private RequirementAssessmentRepository assessmentRepository;

    private Long documentId;
    private Long frameworkId;

    @BeforeEach
    void setUp() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();

        Organization organization = organizationRepository.findByTicker("INFY").orElseThrow();
        frameworkId = frameworkRepository.findByCode("BRSR").orElseThrow().getId();

        Document document = new Document(
                organization,
                "esg-sample.pdf",
                "controller-test-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "controller-test-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        documentId = documentRepository.save(document).getId();
    }

    @Test
    void startAnalysisReturnsCreatedResponse() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{documentId}/analyses", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameworkCode\":\"BRSR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.documentId", is(documentId.intValue())))
                .andExpect(jsonPath("$.frameworkCode", is("BRSR")))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.requirementCount", is(14)))
                .andExpect(jsonPath("$.assessments", hasSize(14)))
                .andExpect(jsonPath("$.assessments[?(@.requirementCode=='ENV-003')].assessmentStatus")
                        .value(hasItem("COVERED")))
                .andExpect(jsonPath("$.assessments[?(@.requirementCode=='ENV-003')].evidenceText")
                        .value(hasItem(containsString("Scope 1"))))
                .andExpect(jsonPath("$.assessments[0].confidence", notNullValue()))
                .andExpect(jsonPath("$.assessments[0].explanation", notNullValue()));
    }

    @Test
    void getAnalysisReturnsPersistedResults() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/documents/{documentId}/analyses", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameworkId\":" + frameworkId + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number analysisId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(get("/api/v1/analyses/{analysisId}", analysisId.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(analysisId.intValue())))
                .andExpect(jsonPath("$.assessments", hasSize(14)))
                .andExpect(jsonPath("$.assessments[0].requirementId", notNullValue()))
                .andExpect(jsonPath("$.assessments[0].requirementCode", notNullValue()))
                .andExpect(jsonPath("$.assessments[0].requirementTitle", notNullValue()))
                .andExpect(jsonPath("$.assessments[0].category", notNullValue()))
                .andExpect(jsonPath("$.assessments[0].retrievalScore", notNullValue()))
                .andExpect(jsonPath("$.assessments[0].confidence", notNullValue()))
                .andExpect(jsonPath("$.assessments[0].explanation", notNullValue()));
    }

    @Test
    void startAnalysisRejectsNonReadyDocument() throws Exception {
        Document document = documentRepository.findById(documentId).orElseThrow();
        document.setStatus(DocumentStatus.FAILED);
        documentRepository.save(document);

        mockMvc.perform(post("/api/v1/documents/{documentId}/analyses", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameworkCode\":\"BRSR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("READY")));
    }

    @Test
    void startAnalysisReturns404ForMissingDocument() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{documentId}/analyses", 99999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameworkCode\":\"BRSR\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Document not found: 99999")));
    }

    @Test
    void startAnalysisReturns404ForMissingFramework() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{documentId}/analyses", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameworkId\":99999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Framework not found: 99999")));
    }
}
