package dev.esgenius.controller;

import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.service.LocalFileStorageService;
import dev.esgenius.support.TestPdfFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private LocalFileStorageService fileStorageService;

    private Long organizationId;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        organizationId = organizationRepository.findByTicker("INFY").orElseThrow().getId();
    }

    @Test
    void uploadDocumentReturnsCreatedMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "brsr-report.pdf",
                "application/pdf",
                TestPdfFixtures.createSamplePdfBytes());

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("organizationId", organizationId.toString())
                        .param("documentType", "BRSR")
                        .param("reportingYear", "2025"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.organizationId", is(organizationId.intValue())))
                .andExpect(jsonPath("$.originalFilename", is("brsr-report.pdf")))
                .andExpect(jsonPath("$.documentType", is("BRSR")))
                .andExpect(jsonPath("$.status", is("READY")))
                .andExpect(jsonPath("$.pageCount", is(1)))
                .andExpect(jsonPath("$.extractedText", containsString(TestPdfFixtures.LINE_ONE)))
                .andExpect(jsonPath("$.extractedText", containsString(TestPdfFixtures.LINE_TWO)))
                .andExpect(jsonPath("$.storagePath").doesNotExist());
    }

    @Test
    void listDocumentsExcludesExtractedText() throws Exception {
        uploadSampleDocument();

        mockMvc.perform(get("/api/v1/documents").param("organizationId", organizationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].originalFilename", is("sample.pdf")))
                .andExpect(jsonPath("$[0].extractedText").doesNotExist())
                .andExpect(jsonPath("$[0].failureReason").doesNotExist());
    }

    @Test
    void getDocumentReturnsDetailWithExtractedText() throws Exception {
        Long documentId = uploadSampleDocument();

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(documentId.intValue())))
                .andExpect(jsonPath("$.extractedText", containsString(TestPdfFixtures.LINE_TWO)))
                .andExpect(jsonPath("$.storagePath").doesNotExist());
    }

    @Test
    void uploadRejectsEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("organizationId", organizationId.toString())
                        .param("documentType", "OTHER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Uploaded file must not be empty")));
    }

    @Test
    void uploadRejectsNonPdfFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "plain text".getBytes());

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("organizationId", organizationId.toString())
                        .param("documentType", "OTHER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Only PDF files are supported")));
    }

    @Test
    void uploadRejectsMissingOrganization() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", TestPdfFixtures.createSamplePdfBytes());

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("organizationId", "99999")
                        .param("documentType", "BRSR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Organization not found: 99999")));
    }

    @Test
    void deleteDocumentRemovesRecordAndStoredFile() throws Exception {
        Long documentId = uploadSampleDocument();
        String storedFilename = documentRepository.findById(documentId).orElseThrow().getStoredFilename();

        mockMvc.perform(delete("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/documents/{documentId}", documentId))
                .andExpect(status().isNotFound());

        org.junit.jupiter.api.Assertions.assertFalse(fileStorageService.exists(storedFilename));
    }

    private Long uploadSampleDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                TestPdfFixtures.createSamplePdfBytes());

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("organizationId", organizationId.toString())
                        .param("documentType", "ANNUAL_REPORT"))
                .andExpect(status().isCreated());

        return documentRepository.findAll().get(0).getId();
    }
}
