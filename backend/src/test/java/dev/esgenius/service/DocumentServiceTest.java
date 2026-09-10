package dev.esgenius.service;

import dev.esgenius.dto.DocumentDetailResponse;
import dev.esgenius.dto.DocumentSummaryResponse;
import dev.esgenius.entity.Organization;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.support.TestPdfFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DocumentServiceTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocalFileStorageService fileStorageService;

    private Organization organization;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        organization = organizationRepository.findByTicker("INFY").orElseThrow();
    }

    @Test
    void uploadDocumentExtractsTextAndPersistsMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "brsr-report.pdf",
                "application/pdf",
                TestPdfFixtures.createSamplePdfBytes());

        DocumentDetailResponse response = documentService.uploadDocument(
                file, organization.getId(), "BRSR", 2025);

        assertNotNull(response.id());
        assertEquals(organization.getId(), response.organizationId());
        assertEquals("Infosys Limited", response.organizationName());
        assertEquals("brsr-report.pdf", response.originalFilename());
        assertEquals("BRSR", response.documentType());
        assertEquals(2025, response.reportingYear());
        assertEquals("READY", response.status());
        assertEquals(1, response.pageCount());
        assertNotNull(response.uploadedAt());
        assertNotNull(response.processedAt());
        assertTrue(response.extractedText().contains(TestPdfFixtures.LINE_ONE));
        assertTrue(response.extractedText().contains(TestPdfFixtures.LINE_TWO));
        assertNull(response.failureReason());

        String storedFilename = documentRepository.findById(response.id()).orElseThrow().getStoredFilename();
        assertTrue(fileStorageService.exists(storedFilename));
    }

    @Test
    void listDocumentsReturnsMetadataWithoutExtractedText() throws Exception {
        uploadSampleDocument();

        List<DocumentSummaryResponse> documents = documentService.listDocuments(organization.getId());

        assertEquals(1, documents.size());
        DocumentSummaryResponse summary = documents.get(0);
        assertEquals("READY", summary.status());
        assertNotNull(summary.originalFilename());
        assertNotNull(summary.uploadedAt());
    }

    @Test
    void getDocumentReturnsExtractedText() throws Exception {
        DocumentDetailResponse uploaded = uploadSampleDocument();

        DocumentDetailResponse detail = documentService.getDocument(uploaded.id());

        assertEquals(uploaded.id(), detail.id());
        assertTrue(detail.extractedText().contains(TestPdfFixtures.LINE_ONE));
    }

    @Test
    void deleteDocumentRemovesMetadataAndStoredFile() throws Exception {
        DocumentDetailResponse uploaded = uploadSampleDocument();
        String storedFilename = documentRepository.findById(uploaded.id()).orElseThrow().getStoredFilename();
        assertTrue(fileStorageService.exists(storedFilename));

        documentService.deleteDocument(uploaded.id());

        assertFalse(documentRepository.existsById(uploaded.id()));
        assertFalse(fileStorageService.exists(storedFilename));
    }

    @Test
    void uploadRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(BadRequestException.class,
                () -> documentService.uploadDocument(file, organization.getId(), "BRSR", 2025));
    }

    @Test
    void uploadRejectsNonPdfExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", TestPdfFixtures.createSamplePdfBytes());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> documentService.uploadDocument(file, organization.getId(), "OTHER", null));
        assertTrue(ex.getMessage().contains("Only PDF files are supported"));
    }

    @Test
    void uploadRejectsMissingOrganization() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", TestPdfFixtures.createSamplePdfBytes());

        assertThrows(ResourceNotFoundException.class,
                () -> documentService.uploadDocument(file, 99999L, "BRSR", 2025));
    }

    private DocumentDetailResponse uploadSampleDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                TestPdfFixtures.createSamplePdfBytes());
        return documentService.uploadDocument(file, organization.getId(), "SUSTAINABILITY_REPORT", 2024);
    }
}
