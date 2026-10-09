package dev.esgenius.controller;

import dev.esgenius.config.AuthenticatedUser;
import dev.esgenius.entity.ComplianceAnalysis;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Organization;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.ReportExportRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.service.DocumentAccessPolicy;
import dev.esgenius.support.TestPdfFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.auth.admin-user-ids=cccccccc-cccc-cccc-cccc-ccccccccccc3")
class DocumentOwnershipAccessTest {

    private static final UUID USER_A_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID USER_B_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2");
    private static final UUID ADMIN_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-ccccccccccc3");

    private static final AuthenticatedUser USER_A = new AuthenticatedUser(USER_A_ID, "a@example.com");
    private static final AuthenticatedUser USER_B = new AuthenticatedUser(USER_B_ID, "b@example.com");
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(ADMIN_ID, "admin@example.com");

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

    @Autowired
    private ReportExportRepository reportExportRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long organizationId;

    @BeforeEach
    void setUp() {
        reportExportRepository.deleteAll();
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();
        organizationId = organizationRepository.findByTicker("INFY").orElseThrow().getId();
    }

    @Test
    void ownerSeesUploadAndOtherUserReceivesNotFound() throws Exception {
        Long documentId = uploadAs(USER_A);

        assertThat(documentRepository.findById(documentId).orElseThrow().getOwnerUserId()).isEqualTo(USER_A_ID);

        mockMvc.perform(asUser(get("/api/v1/documents").param("organizationId", organizationId.toString()), USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(documentId.intValue())))
                .andExpect(jsonPath("$[0].shared", is(false)))
                .andExpect(jsonPath("$[0].canModify", is(true)));

        mockMvc.perform(asUser(get("/api/v1/documents").param("organizationId", organizationId.toString()), USER_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        expectDocumentNotFound(asUser(get("/api/v1/documents/{documentId}", documentId), USER_B));
        expectDocumentNotFound(asUser(delete("/api/v1/documents/{documentId}", documentId), USER_B));
        expectDocumentNotFound(asUser(get("/api/v1/documents/{documentId}/pages", documentId), USER_B));
        expectDocumentNotFound(asUser(get("/api/v1/documents/{documentId}/analyses", documentId), USER_B));
        expectDocumentNotFound(asUser(post("/api/v1/documents/{documentId}/analyses", documentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"frameworkCode\":\"BRSR\"}"), USER_B));
        expectDocumentNotFound(asUser(post("/api/v1/documents/{documentId}/assistant/ask", documentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"carbon emissions\"}"), USER_B));

        ComplianceAnalysis analysis = analysisRepository.save(new ComplianceAnalysis(
                documentRepository.findById(documentId).orElseThrow(),
                frameworkRepository.findByCode("BRSR").orElseThrow()));

        mockMvc.perform(asUser(get("/api/v1/analyses/{analysisId}/report.pdf", analysis.getId()), USER_B))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Analysis not found: " + analysis.getId())));
        assertThat(reportExportRepository.findAll()).isEmpty();

        mockMvc.perform(asUser(get("/api/v1/documents/{documentId}", documentId), USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shared", is(false)))
                .andExpect(jsonPath("$.canModify", is(true)));
    }

    @Test
    void sharedDocumentIsViewableButNotModifiableUnlessAdmin() throws Exception {
        Document shared = saveDocument(null, "shared-sample");
        Document sharedToDelete = saveDocument(null, "shared-delete");

        mockMvc.perform(asUser(get("/api/v1/documents/{documentId}", shared.getId()), USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shared", is(true)))
                .andExpect(jsonPath("$.canModify", is(false)));
        mockMvc.perform(asUser(get("/api/v1/documents/{documentId}", shared.getId()), USER_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shared", is(true)))
                .andExpect(jsonPath("$.canModify", is(false)));

        mockMvc.perform(asUser(get("/api/v1/documents").param("organizationId", organizationId.toString()), USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(asUser(get("/api/v1/documents").param("organizationId", organizationId.toString()), USER_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        expectSharedForbidden(asUser(delete("/api/v1/documents/{documentId}", shared.getId()), USER_A));
        expectSharedForbidden(asUser(delete("/api/v1/documents/{documentId}", shared.getId()), USER_B));
        expectSharedForbidden(asUser(post("/api/v1/documents/{documentId}/analyses", shared.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"frameworkCode\":\"BRSR\"}"), USER_A));
        expectSharedForbidden(asUser(post("/api/v1/documents/{documentId}/analyses", shared.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"frameworkCode\":\"BRSR\"}"), USER_B));

        mockMvc.perform(asUser(get("/api/v1/documents/{documentId}", shared.getId()), ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shared", is(true)))
                .andExpect(jsonPath("$.canModify", is(true)));
        mockMvc.perform(asUser(post("/api/v1/documents/{documentId}/analyses", shared.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameworkCode\":\"BRSR\"}"), ADMIN))
                .andExpect(status().isAccepted());
        mockMvc.perform(asUser(delete("/api/v1/documents/{documentId}", sharedToDelete.getId()), ADMIN))
                .andExpect(status().isNoContent());
        assertThat(documentRepository.findById(sharedToDelete.getId())).isEmpty();
    }

    @Test
    void absentIdentityWithAuthDisabledActsAsAdmin() throws Exception {
        Document shared = saveDocument(null, "local-dev-shared");

        mockMvc.perform(get("/api/v1/documents/{documentId}", shared.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shared", is(true)))
                .andExpect(jsonPath("$.canModify", is(true)));

        mockMvc.perform(post("/api/v1/documents/{documentId}/analyses", shared.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameworkCode\":\"BRSR\"}"))
                .andExpect(status().isAccepted());

        Document sharedToDelete = saveDocument(null, "local-dev-delete");
        mockMvc.perform(delete("/api/v1/documents/{documentId}", sharedToDelete.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void flywayV10KeepsExistingRowsOwnerNull() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '10'",
                Integer.class);
        assertThat(applied).isEqualTo(1);

        String nullable = jdbcTemplate.queryForObject(
                """
                SELECT is_nullable FROM information_schema.columns
                WHERE lower(table_name) = 'document' AND lower(column_name) = 'owner_user_id'
                """,
                String.class);
        assertThat(nullable).isEqualToIgnoringCase("YES");

        Integer indexCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.indexes
                WHERE lower(table_name) = 'document' AND lower(index_name) = 'idx_document_owner_user_id'
                """,
                Integer.class);
        assertThat(indexCount).isEqualTo(1);

        String storedFilename = "legacy-null-owner-" + System.nanoTime() + ".pdf";
        jdbcTemplate.update(
                """
                INSERT INTO document (
                    organization_id, original_filename, stored_filename, document_type,
                    status, file_size, storage_path, uploaded_at)
                VALUES (?, 'legacy.pdf', ?, 'OTHER', 'UPLOADED', 1, ?, CURRENT_TIMESTAMP)
                """,
                organizationId, storedFilename, storedFilename);

        UUID owner = jdbcTemplate.query(
                "SELECT owner_user_id FROM document WHERE stored_filename = ?",
                rs -> {
                    assertThat(rs.next()).isTrue();
                    return rs.getObject("owner_user_id", UUID.class);
                },
                storedFilename);
        assertThat(owner).isNull();
    }

    private Long uploadAs(AuthenticatedUser user) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "owned.pdf",
                "application/pdf",
                TestPdfFixtures.createSamplePdfBytes());

        String body = mockMvc.perform(asUser(multipart("/api/v1/documents")
                        .file(file)
                        .param("organizationId", organizationId.toString())
                        .param("documentType", "BRSR")
                        .param("reportingYear", "2025"), user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shared", is(false)))
                .andExpect(jsonPath("$.canModify", is(true)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        return id.longValue();
    }

    private Document saveDocument(UUID ownerUserId, String name) {
        Organization organization = organizationRepository.findByTicker("INFY").orElseThrow();
        String stored = name + "-" + System.nanoTime() + ".pdf";
        Document document = new Document(
                organization,
                name + ".pdf",
                stored,
                DocumentType.BRSR,
                2025,
                128L,
                stored);
        document.setOwnerUserId(ownerUserId);
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText("Scope 1 emissions from owned facilities were disclosed.");
        document.setPageCount(1);
        document.setProcessedAt(Instant.now());
        return documentRepository.save(document);
    }

    private static MockHttpServletRequestBuilder asUser(
            MockHttpServletRequestBuilder request, AuthenticatedUser user) {
        return request.requestAttr(AuthenticatedUser.REQUEST_ATTRIBUTE, user);
    }

    private void expectDocumentNotFound(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Document not found:")));
    }

    private void expectSharedForbidden(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is(DocumentAccessPolicy.SHARED_DOCUMENT_MODIFY_MESSAGE)));
    }
}
