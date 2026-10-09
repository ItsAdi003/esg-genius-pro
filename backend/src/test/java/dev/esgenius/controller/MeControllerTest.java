package dev.esgenius.controller;

import com.jayway.jsonpath.JsonPath;
import dev.esgenius.config.AuthenticatedUser;
import dev.esgenius.ratelimit.UsageLimiter;
import dev.esgenius.service.Caller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.limits.uploads-per-user-per-day=20",
        "app.limits.analyses-per-user-per-day=5",
        "app.limits.assistant-asks-per-user-per-hour=30",
        "app.auth.admin-user-ids=cccccccc-cccc-cccc-cccc-ccccccccccc3"
})
class MeControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-ccccccccccc3");
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(ADMIN_ID, "admin@example.com");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsageLimiter usageLimiter;

    @Test
    void noIdentityIsAdminWithNullEmailAndNullLimits() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        "{\"email\":null,\"admin\":true,\"limits\":{"
                                + "\"uploadsPerDay\":null,\"analysesPerDay\":null,\"assistantAsksPerHour\":null}}",
                        true))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.limits.globalAnalysesPerDay").doesNotExist());
    }

    @Test
    void identifiedUserSeesEmailAndRemainingLimits() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "a@b.com");
        Caller caller = new Caller(userId, false);
        usageLimiter.consumeUpload(caller);
        usageLimiter.consumeUpload(caller);
        usageLimiter.consumeUpload(caller);
        usageLimiter.consumeAnalysis(caller);
        usageLimiter.consumeAssistantAsk(caller);
        usageLimiter.consumeAssistantAsk(caller);

        String body = mockMvc.perform(asUser(get("/api/v1/me"), user))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("a@b.com"))
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.limits.uploadsPerDay.limit").value(20))
                .andExpect(jsonPath("$.limits.uploadsPerDay.used").value(3))
                .andExpect(jsonPath("$.limits.analysesPerDay.limit").value(5))
                .andExpect(jsonPath("$.limits.analysesPerDay.used").value(1))
                .andExpect(jsonPath("$.limits.assistantAsksPerHour.limit").value(30))
                .andExpect(jsonPath("$.limits.assistantAsksPerHour.used").value(2))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.limits.globalAnalysesPerDay").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain(userId.toString());
        assertThat((Integer) JsonPath.read(body, "$.limits.uploadsPerDay.resetsInSeconds")).isBetween(86_000, 86_400);
        assertThat((Integer) JsonPath.read(body, "$.limits.analysesPerDay.resetsInSeconds")).isBetween(86_000, 86_400);
        assertThat((Integer) JsonPath.read(body, "$.limits.assistantAsksPerHour.resetsInSeconds")).isBetween(3_500, 3_600);
    }

    @Test
    void adminSeesNullLimits() throws Exception {
        Caller adminCaller = new Caller(ADMIN_ID, true);
        usageLimiter.consumeUpload(adminCaller);
        usageLimiter.consumeAnalysis(adminCaller);
        usageLimiter.consumeAssistantAsk(adminCaller);

        mockMvc.perform(asUser(get("/api/v1/me"), ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.admin").value(true))
                .andExpect(jsonPath("$.limits.uploadsPerDay").value(nullValue()))
                .andExpect(jsonPath("$.limits.analysesPerDay").value(nullValue()))
                .andExpect(jsonPath("$.limits.assistantAsksPerHour").value(nullValue()))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void getMeDoesNotConsumeBudget() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "user@example.com");
        Caller caller = new Caller(userId, false);
        usageLimiter.consumeUpload(caller);
        usageLimiter.consumeUpload(caller);
        usageLimiter.consumeUpload(caller);

        mockMvc.perform(asUser(get("/api/v1/me"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limits.uploadsPerDay.used").value(3));
        mockMvc.perform(asUser(get("/api/v1/me"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limits.uploadsPerDay.used").value(3));

        assertThat(usageLimiter.snapshot(caller).uploadsPerDay().used()).isEqualTo(3);
    }

    private static MockHttpServletRequestBuilder asUser(
            MockHttpServletRequestBuilder request, AuthenticatedUser user) {
        return request.requestAttr(AuthenticatedUser.REQUEST_ATTRIBUTE, user);
    }
}
