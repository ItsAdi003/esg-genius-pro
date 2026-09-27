package dev.esgenius.service.assistant.gemini;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.esgenius.config.GeminiProperties;
import dev.esgenius.service.assistant.AssistantAnswerException;
import dev.esgenius.service.assistant.AssistantAnswerFailureCategory;
import dev.esgenius.service.assistant.AssistantAnswerPromptBuilder;
import dev.esgenius.service.assistant.AssistantAnswerRequest;
import dev.esgenius.service.assistant.AssistantAnswerResult;
import dev.esgenius.service.assistant.AssistantAnswerResultParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class GeminiAssistantAnswerProviderTest {

    private GeminiProperties properties;
    private MockRestServiceServer mockServer;
    private GeminiAssistantAnswerProvider provider;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        properties = new GeminiProperties();
        properties.setApiKey("test-api-key");
        properties.setModel("gemini-3.5-flash");
        properties.setBaseUrl("https://generativelanguage.googleapis.com");
        properties.setConnectTimeout(Duration.ofSeconds(5));
        properties.setReadTimeout(Duration.ofSeconds(5));
        properties.setMaxRetries(2);
        properties.setInitialRetryBackoff(Duration.ofMillis(1));
        properties.setMaxRetryBackoff(Duration.ofMillis(5));
        properties.setThinkingLevel("low");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        ObjectMapper objectMapper = new ObjectMapper();
        provider = new GeminiAssistantAnswerProvider(
                properties,
                restClient,
                new AssistantAnswerPromptBuilder(),
                new AssistantAnswerResultParser(objectMapper),
                objectMapper);

        Logger logger = (Logger) LoggerFactory.getLogger(GeminiAssistantAnswerProvider.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @Test
    void answerSendsStructuredRequestAndMapsPassageNumbers() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString(
                        "/v1beta/models/gemini-3.5-flash:generateContent")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("responseMimeType"),
                        org.hamcrest.Matchers.containsString("thinkingLevel"),
                        org.hamcrest.Matchers.containsString("LOW"),
                        org.hamcrest.Matchers.containsString("passageIndices"),
                        org.hamcrest.Matchers.containsString("\"answer\""),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("test-api-key")))))
                .andRespond(withSuccess(successResponse("Withdrawal was 12500 kilolitres.", "[1]"),
                        MediaType.APPLICATION_JSON));

        AssistantAnswerResult result = provider.answer(sampleRequest());

        assertThat(result.answer()).isEqualTo("Withdrawal was 12500 kilolitres.");
        assertThat(result.passageIndices()).containsExactly(0);
        mockServer.verify();
    }

    @Test
    void retriesHttp429AndSucceeds() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withSuccess(successResponse("Retried answer.", "[1]"), MediaType.APPLICATION_JSON));

        AssistantAnswerResult result = provider.answer(sampleRequest());

        assertThat(result.answer()).isEqualTo("Retried answer.");
        mockServer.verify();
    }

    @Test
    void retriesHttp503UntilExhausted() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        AssistantAnswerException ex = assertThrows(AssistantAnswerException.class, () ->
                provider.answer(sampleRequest()));

        assertThat(ex.getCategory()).isEqualTo(AssistantAnswerFailureCategory.HTTP_SERVER_ERROR);
        assertThat(ex.getHttpStatus()).isEqualTo(503);
        assertThat(ex.getAttempt()).isEqualTo(3);
        assertThat(ex.isRetryable()).isTrue();
        mockServer.verify();
    }

    @Test
    void doesNotRetryHttp401() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withUnauthorizedRequest());

        AssistantAnswerException ex = assertThrows(AssistantAnswerException.class, () ->
                provider.answer(sampleRequest()));

        assertThat(ex.getCategory()).isEqualTo(AssistantAnswerFailureCategory.CONFIGURATION_ERROR);
        assertThat(ex.getHttpStatus()).isEqualTo(401);
        assertThat(ex.getAttempt()).isEqualTo(1);
        assertThat(ex.isRetryable()).isFalse();
        mockServer.verify();
    }

    @Test
    void doesNotRetryInvalidPassageIndex() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withSuccess(successResponse("Out of range.", "[9]"), MediaType.APPLICATION_JSON));

        AssistantAnswerException ex = assertThrows(AssistantAnswerException.class, () ->
                provider.answer(sampleRequest()));

        assertThat(ex.getCategory()).isEqualTo(AssistantAnswerFailureCategory.VALIDATION_FAILURE);
        assertThat(ex.getAttempt()).isEqualTo(1);
        assertThat(ex.isRetryable()).isFalse();
        mockServer.verify();
    }

    @Test
    void loggingDoesNotExposeApiKey() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withUnauthorizedRequest());

        assertThrows(AssistantAnswerException.class, () -> provider.answer(sampleRequest()));

        String combinedLogs = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", String::concat);
        assertThat(combinedLogs).doesNotContain("test-api-key");
        assertThat(combinedLogs).contains("category=");
    }

    @Test
    void rejectsMissingApiKeyWithoutCallingGemini() {
        properties.setApiKey("");

        AssistantAnswerException ex = assertThrows(AssistantAnswerException.class, () ->
                provider.answer(sampleRequest()));

        assertThat(ex.getCategory()).isEqualTo(AssistantAnswerFailureCategory.CONFIGURATION_ERROR);
        mockServer.verify();
    }

    @Test
    void rejectsEmptyEvidencePassagesWithoutCallingGemini() {
        AssistantAnswerException ex = assertThrows(AssistantAnswerException.class, () ->
                provider.answer(new AssistantAnswerRequest("groundwater withdrawal", List.of())));

        assertThat(ex.getCategory()).isEqualTo(AssistantAnswerFailureCategory.VALIDATION_FAILURE);
        mockServer.verify();
    }

    @Test
    void extractResponseTextRejectsBlockedFinishReason() {
        AssistantAnswerException ex = assertThrows(AssistantAnswerException.class, () ->
                provider.extractResponseText("""
                        {"candidates":[{"finishReason":"SAFETY","content":{"parts":[{"text":"{}"}]}}]}
                        """, 1));

        assertThat(ex.getCategory()).isEqualTo(AssistantAnswerFailureCategory.BLOCKED_RESPONSE);
    }

    @Test
    void extractResponseTextRejectsEmptyCandidates() {
        AssistantAnswerException ex = assertThrows(AssistantAnswerException.class, () ->
                provider.extractResponseText("{\"candidates\":[]}", 1));

        assertThat(ex.getCategory()).isEqualTo(AssistantAnswerFailureCategory.BLOCKED_RESPONSE);
    }

    private AssistantAnswerRequest sampleRequest() {
        return new AssistantAnswerRequest(
                "groundwater withdrawal kilolitres",
                List.of("Groundwater withdrawal during the reporting period was 12500 kilolitres."));
    }

    private String successResponse(String answer, String passageIndices) {
        return """
                {
                  "candidates": [{
                    "finishReason": "STOP",
                    "content": {
                      "parts": [{
                        "text": "{\\"answer\\":\\"%s\\",\\"passageIndices\\":%s}"
                      }]
                    }
                  }]
                }
                """.formatted(answer, passageIndices);
    }
}
