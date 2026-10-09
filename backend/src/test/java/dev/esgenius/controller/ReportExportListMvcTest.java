package dev.esgenius.controller;

import dev.esgenius.config.AuthenticatedUser;
import dev.esgenius.entity.AnalysisStatus;
import dev.esgenius.entity.ComplianceAnalysis;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Framework;
import dev.esgenius.entity.Organization;
import dev.esgenius.entity.ReportExport;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.ReportExportRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.auth.admin-user-ids=cccccccc-cccc-cccc-cccc-ccccccccccc3")
class ReportExportListMvcTest {

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

    private Organization organization;
    private Framework framework;

    @BeforeEach
    void setUp() {
        reportExportRepository.deleteAll();
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();
        organization = organizationRepository.findByTicker("INFY").orElseThrow();
        framework = frameworkRepository.findByCode("BRSR").orElseThrow();
    }

    @Test
    void listIsolatesExportsBetweenUsersAndAdminSeesAll() throws Exception {
        ComplianceAnalysis analysisA = saveAnalysis(saveDocument(USER_A_ID, "a-report.pdf"));
        ComplianceAnalysis analysisB = saveAnalysis(saveDocument(USER_B_ID, "b-report.pdf"));
        ReportExport exportA = saveExport(analysisA, USER_A_ID, Instant.parse("2026-10-09T10:15:00Z"), 3800L);
        ReportExport exportB = saveExport(analysisB, USER_B_ID, Instant.parse("2026-10-09T10:16:00Z"), 4100L);

        mockMvc.perform(asUser(get("/api/v1/reports"), USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(exportA.getId().intValue())))
                .andExpect(jsonPath("$[0].analysisId", is(analysisA.getId().intValue())))
                .andExpect(jsonPath("$[0].documentId", is(analysisA.getDocument().getId().intValue())))
                .andExpect(jsonPath("$[0].documentName", is("a-report.pdf")))
                .andExpect(jsonPath("$[0].frameworkCode", is("BRSR")))
                .andExpect(jsonPath("$[0].format", is("PDF")))
                .andExpect(jsonPath("$[0].sizeBytes", is(3800)))
                .andExpect(jsonPath("$[0].generatedAt", is("2026-10-09T10:15:00Z")));

        mockMvc.perform(asUser(get("/api/v1/reports"), USER_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(exportB.getId().intValue())))
                .andExpect(jsonPath("$[0].documentName", is("b-report.pdf")));

        mockMvc.perform(asUser(get("/api/v1/reports"), ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(exportB.getId().intValue())))
                .andExpect(jsonPath("$[1].id", is(exportA.getId().intValue())));

        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void listOmitsExportWhenCallerCanNoLongerViewDocument() throws Exception {
        Document document = saveDocument(USER_A_ID, "owned.pdf");
        ComplianceAnalysis analysis = saveAnalysis(document);
        saveExport(analysis, USER_A_ID, Instant.parse("2026-10-09T11:00:00Z"), 100L);

        document.setOwnerUserId(USER_B_ID);
        documentRepository.save(document);

        mockMvc.perform(asUser(get("/api/v1/reports"), USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(asUser(get("/api/v1/reports"), ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].documentName", is("owned.pdf")));
    }

    @Test
    void listReturnsNewestFirstAndCapsAtFifty() throws Exception {
        ComplianceAnalysis analysis = saveAnalysis(saveDocument(USER_A_ID, "cap.pdf"));
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        Long newestId = null;
        Long secondNewestId = null;
        for (int i = 0; i < 51; i++) {
            ReportExport saved = saveExport(analysis, USER_A_ID, base.plusSeconds(i), 10L + i);
            if (i == 50) {
                newestId = saved.getId();
            }
            if (i == 49) {
                secondNewestId = saved.getId();
            }
        }

        mockMvc.perform(asUser(get("/api/v1/reports"), USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(50)))
                .andExpect(jsonPath("$[0].id", is(newestId.intValue())))
                .andExpect(jsonPath("$[1].id", is(secondNewestId.intValue())))
                .andExpect(jsonPath("$[0].sizeBytes", is(60)));
    }

    @Test
    void deletingDocumentCascadesReportExports() throws Exception {
        Document document = saveDocument(USER_A_ID, "to-delete.pdf");
        ComplianceAnalysis analysis = saveAnalysis(document);
        ReportExport export = saveExport(analysis, USER_A_ID, Instant.parse("2026-10-09T12:00:00Z"), 200L);
        Long exportId = export.getId();
        Long documentId = document.getId();

        mockMvc.perform(asUser(delete("/api/v1/documents/{documentId}", documentId), USER_A))
                .andExpect(status().isNoContent());

        assertThat(documentRepository.findById(documentId)).isEmpty();
        assertThat(analysisRepository.findById(analysis.getId())).isEmpty();
        assertThat(reportExportRepository.findById(exportId)).isEmpty();
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM report_export WHERE id = ?", Integer.class, exportId);
        assertThat(remaining).isZero();
    }

    @Test
    void flywayV11CreatesReportExportTableOnH2() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '11'",
                Integer.class);
        assertThat(applied).isEqualTo(1);

        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE lower(table_name) = 'report_export'
                """,
                Integer.class);
        assertThat(tableCount).isEqualTo(1);

        Integer indexCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.indexes
                WHERE lower(table_name) = 'report_export'
                  AND lower(index_name) = 'idx_report_export_user_id_generated_at'
                """,
                Integer.class);
        assertThat(indexCount).isEqualTo(1);
    }

    private Document saveDocument(UUID ownerUserId, String originalFilename) {
        String stored = originalFilename.replace(".pdf", "") + "-" + System.nanoTime() + ".pdf";
        Document document = new Document(
                organization,
                originalFilename,
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

    private ComplianceAnalysis saveAnalysis(Document document) {
        ComplianceAnalysis analysis = new ComplianceAnalysis(document, framework);
        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setCompletedAt(Instant.now());
        return analysisRepository.save(analysis);
    }

    private ReportExport saveExport(ComplianceAnalysis analysis, UUID userId, Instant generatedAt, long sizeBytes) {
        return reportExportRepository.save(new ReportExport(
                analysis, userId, ReportExport.FORMAT_PDF, sizeBytes, generatedAt));
    }

    private static MockHttpServletRequestBuilder asUser(
            MockHttpServletRequestBuilder request, AuthenticatedUser user) {
        return request.requestAttr(AuthenticatedUser.REQUEST_ATTRIBUTE, user);
    }
}
