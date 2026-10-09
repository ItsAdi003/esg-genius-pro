package dev.esgenius.controller;

import dev.esgenius.config.AuthenticatedUser;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Organization;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.service.DocumentAccessPolicy;
import dev.esgenius.support.TestPdfFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.limits.uploads-per-user-per-day=1",
        "app.limits.analyses-per-user-per-day=1",
        "app.limits.assistant-asks-per-user-per-hour=1",
        "app.limits.global-analyses-per-day=0"
})
class UsageLimitMvcTest {

    private static final String DOCUMENT_TEXT =
            "Groundwater withdrawal during the reporting period was 12500 kilolitres.";

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

    private Long organizationId;

    @BeforeEach
    void setUp() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();
        organizationId = organizationRepository.findByTicker("INFY").orElseThrow().getId();
    }

    @Test
    void uploadOverThePerUserLimitReturns429WithRetryAfter() throws Exception {
        AuthenticatedUser user = user("11111111-1111-1111-1111-111111111111");

        upload(user).andExpect(status().isCreated());
        assertTooManyRequests(
                upload(user),
                "You have reached the limit of 1 uploads per day. Try again in ",
                86_000,
                86_400);
    }

    @Test
    void requestsWithNoIdentityAreNotLimited() throws Exception {
        upload(null).andExpect(status().isCreated());
        upload(null).andExpect(status().isCreated());
    }

    @Test
    void ownershipDeniedAnalysisDoesNotConsumeBudget() throws Exception {
        AuthenticatedUser owner = user("22222222-2222-2222-2222-222222222222");
        AuthenticatedUser other = user("33333333-3333-3333-3333-333333333333");
        Long ownedId = saveDocument(owner.userId(), DOCUMENT_TEXT).getId();
        Long secondOwnedId = saveDocument(owner.userId(), DOCUMENT_TEXT).getId();
        Long sharedId = saveDocument(null, DOCUMENT_TEXT).getId();

        mockMvc.perform(asUser(analysis(ownedId), other))
                .andExpect(status().isNotFound());
        mockMvc.perform(asUser(analysis(sharedId), owner))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is(DocumentAccessPolicy.SHARED_DOCUMENT_MODIFY_MESSAGE)));

        mockMvc.perform(asUser(analysis(ownedId), owner))
                .andExpect(status().isAccepted());
        assertThat(analysisRepository.count()).isEqualTo(1);

        assertTooManyRequests(
                mockMvc.perform(asUser(analysis(secondOwnedId), owner)),
                "You have reached the limit of 1 analyses per day. Try again in ",
                86_000,
                86_400);
        assertThat(analysisRepository.count()).isEqualTo(1);
    }

    @Test
    void ownershipDeniedAssistantAskDoesNotConsumeBudget() throws Exception {
        AuthenticatedUser owner = user("44444444-4444-4444-4444-444444444444");
        AuthenticatedUser other = user("55555555-5555-5555-5555-555555555555");
        Long documentId = saveDocument(owner.userId(), DOCUMENT_TEXT).getId();

        mockMvc.perform(asUser(ask(documentId, "groundwater withdrawal"), other))
                .andExpect(status().isNotFound());
        mockMvc.perform(asUser(ask(documentId, "   "), owner))
                .andExpect(status().isBadRequest());

        mockMvc.perform(asUser(ask(documentId, "groundwater withdrawal"), owner))
                .andExpect(status().isOk());
        assertTooManyRequests(
                mockMvc.perform(asUser(ask(documentId, "groundwater withdrawal"), owner)),
                "You have reached the limit of 1 assistant questions per hour. Try again in ",
                3_500,
                3_600);
    }

    private void assertTooManyRequests(
            ResultActions result, String messagePrefix, long minRetryAfter, long maxRetryAfter) throws Exception {
        String retryAfter = result.andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.error", is("Too Many Requests")))
                .andExpect(jsonPath("$.message", containsString(messagePrefix)))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.RETRY_AFTER);

        long seconds = Long.parseLong(retryAfter);
        assertThat(seconds).isBetween(minRetryAfter, maxRetryAfter);
        assertThat(result.andReturn().getResponse().getContentAsString()).doesNotContain("UsageLimiter");
    }

    private ResultActions upload(AuthenticatedUser user) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "limited.pdf",
                "application/pdf",
                TestPdfFixtures.createSamplePdfBytes());
        MockHttpServletRequestBuilder request = multipart("/api/v1/documents")
                .file(file)
                .param("organizationId", organizationId.toString())
                .param("documentType", "BRSR")
                .param("reportingYear", "2025");
        if (user != null) {
            request = asUser(request, user);
        }
        return mockMvc.perform(request);
    }

    private static MockHttpServletRequestBuilder analysis(Long documentId) {
        return post("/api/v1/documents/{documentId}/analyses", documentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"frameworkCode\":\"BRSR\"}");
    }

    private static MockHttpServletRequestBuilder ask(Long documentId, String question) {
        String escaped = question.replace("\\", "\\\\").replace("\"", "\\\"");
        return post("/api/v1/documents/{documentId}/assistant/ask", documentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"" + escaped + "\"}");
    }

    private Document saveDocument(UUID ownerUserId, String text) {
        Organization organization = organizationRepository.findByTicker("INFY").orElseThrow();
        String stored = "limit-" + UUID.randomUUID() + ".pdf";
        Document document = new Document(
                organization,
                "limit.pdf",
                stored,
                DocumentType.BRSR,
                2025,
                128L,
                stored);
        document.setOwnerUserId(ownerUserId);
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(text);
        document.setPageCount(1);
        document.setProcessedAt(Instant.now());
        return documentRepository.save(document);
    }

    private static AuthenticatedUser user(String id) {
        return new AuthenticatedUser(UUID.fromString(id), id + "@example.com");
    }

    private static MockHttpServletRequestBuilder asUser(
            MockHttpServletRequestBuilder request, AuthenticatedUser user) {
        return request.requestAttr(AuthenticatedUser.REQUEST_ATTRIBUTE, user);
    }
}
