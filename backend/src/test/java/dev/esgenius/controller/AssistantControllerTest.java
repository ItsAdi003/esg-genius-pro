package dev.esgenius.controller;

import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentPage;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Organization;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentPageRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.service.DocumentAssistantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssistantControllerTest {

    private static final String GROUNDWATER =
            "Groundwater withdrawal during the reporting period was 12500 kilolitres across all manufacturing plants and offices.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentPageRepository documentPageRepository;

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
                "assistant-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "assistant-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(GROUNDWATER);
        document.setPageCount(1);
        document.setProcessedAt(Instant.now());
        document = documentRepository.save(document);
        documentPageRepository.save(new DocumentPage(document, 4, GROUNDWATER));
        documentId = document.getId();
    }

    @Test
    void askReturnsGroundedAnswerWithPageCitation() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{documentId}/assistant/ask", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"groundwater withdrawal kilolitres\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded", is(true)))
                .andExpect(jsonPath("$.answer", containsString("supplied passage")))
                .andExpect(jsonPath("$.citations", hasSize(1)))
                .andExpect(jsonPath("$.citations[0].pageNumber", is(4)))
                .andExpect(jsonPath("$.citations[0].chunkIndex", is(0)))
                .andExpect(jsonPath("$.citations[0].snippet", containsString("Groundwater withdrawal")));
    }

    @Test
    void askReturnsNotFoundWhenDocumentHasNoMatchingEvidence() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{documentId}/assistant/ask", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"whistleblower helpline complaints\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded", is(false)))
                .andExpect(jsonPath("$.answer", is(DocumentAssistantService.NOT_FOUND_ANSWER)))
                .andExpect(jsonPath("$.citations", hasSize(0)));
    }

    @Test
    void askRejectsBlankQuestion() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{documentId}/assistant/ask", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("blank")));
    }

    @Test
    void askRejectsDocumentThatIsNotReady() throws Exception {
        Document document = documentRepository.findById(documentId).orElseThrow();
        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        mockMvc.perform(post("/api/v1/documents/{documentId}/assistant/ask", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"groundwater withdrawal\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("READY")));
    }

    @Test
    void askReturns404WhenDocumentIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{documentId}/assistant/ask", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"groundwater withdrawal\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void legacyDocumentWithoutPagesReturnsNullPageNumber() throws Exception {
        documentPageRepository.deleteAll();

        mockMvc.perform(post("/api/v1/documents/{documentId}/assistant/ask", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"groundwater withdrawal kilolitres\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded", is(true)))
                .andExpect(jsonPath("$.citations[0].pageNumber", nullValue()))
                .andExpect(jsonPath("$.citations[0].chunkIndex", is(0)));
    }
}
