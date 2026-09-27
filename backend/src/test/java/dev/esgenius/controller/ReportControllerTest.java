package dev.esgenius.controller;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ComplianceAnalysisRepository analysisRepository;

    @Autowired
    private RequirementAssessmentRepository assessmentRepository;

    private Long documentId;

    @BeforeEach
    void setUp() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();

        Organization organization = organizationRepository.findByTicker("INFY").orElseThrow();
        Document document = new Document(
                organization,
                "esg-sample.pdf",
                "report-controller-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "report-controller-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        documentId = documentRepository.save(document).getId();
    }

    @Test
    void downloadReportPdfReturnsAttachmentForExistingAnalysis() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/documents/{documentId}/analyses", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameworkCode\":\"BRSR\"}"))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number analysisId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        MvcResult result = mockMvc.perform(get("/api/v1/analyses/{analysisId}/report.pdf", analysisId.longValue()))
                .andExpect(status().isOk())
                .andExpect(header().string(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")))
                .andExpect(header().string(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        containsString("gap-assessment-" + analysisId.intValue() + ".pdf")))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body.length).isGreaterThan(4);
        assertThat(new String(body, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void downloadReportPdfReturns404ForMissingAnalysis() throws Exception {
        mockMvc.perform(get("/api/v1/analyses/{analysisId}/report.pdf", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Analysis not found: 99999")));
    }
}
