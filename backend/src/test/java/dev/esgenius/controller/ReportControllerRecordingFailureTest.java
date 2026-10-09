package dev.esgenius.controller;

import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Organization;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.ReportExportRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.service.Caller;
import dev.esgenius.service.DocumentAccessPolicy;
import dev.esgenius.service.ReportExportService;
import dev.esgenius.support.ComplianceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
@Import(ReportControllerRecordingFailureTest.FailingReportExportConfig.class)
class ReportControllerRecordingFailureTest {

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

    @Autowired
    private ReportExportRepository reportExportRepository;

    private Long documentId;

    @BeforeEach
    void setUp() {
        reportExportRepository.deleteAll();
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();

        Organization organization = organizationRepository.findByTicker("INFY").orElseThrow();
        Document document = new Document(
                organization,
                "esg-sample.pdf",
                "report-record-fail-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "report-record-fail-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        documentId = documentRepository.save(document).getId();
    }

    @Test
    void recordingFailureDoesNotBreakDownload() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/documents/{documentId}/analyses", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameworkCode\":\"BRSR\"}"))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number analysisId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(get("/api/v1/analyses/{analysisId}/report.pdf", analysisId.longValue()))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")));

        assertThat(reportExportRepository.count()).isZero();
    }

    @Test
    void failedGenerationDoesNotRecordExport() throws Exception {
        mockMvc.perform(get("/api/v1/analyses/{analysisId}/report.pdf", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Analysis not found: 99999")));

        assertThat(reportExportRepository.count()).isZero();
    }

    @TestConfiguration
    static class FailingReportExportConfig {
        @Bean
        @Primary
        ReportExportService failingReportExportService(
                ReportExportRepository reportExportRepository,
                ComplianceAnalysisRepository analysisRepository,
                DocumentAccessPolicy documentAccessPolicy) {
            return new ReportExportService(
                    reportExportRepository, analysisRepository, documentAccessPolicy) {
                @Override
                public void recordPdfExport(Long analysisId, Caller caller, long sizeBytes) {
                    throw new IllegalStateException("database unavailable");
                }
            };
        }
    }
}
