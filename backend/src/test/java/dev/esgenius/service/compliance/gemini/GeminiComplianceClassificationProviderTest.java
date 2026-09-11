package dev.esgenius.service.compliance.gemini;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.esgenius.config.GeminiProperties;
import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.service.compliance.*;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiComplianceClassificationProviderTest {

    private GeminiProperties properties;
    private MockRestServiceServer mockServer;
    private GeminiComplianceClassificationProvider provider;
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
        provider = new GeminiComplianceClassificationProvider(
                properties,
                restClient,
                new ComplianceClassificationPromptBuilder(),
                new ComplianceClassificationResultParser(objectMapper),
                objectMapper);

        Logger logger = (Logger) LoggerFactory.getLogger(GeminiComplianceClassificationProvider.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @Test
    void classifySendsStructuredRequestWithThinkingLevel() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/v1beta/models/gemini-3.5-flash:generateContent")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("responseMimeType"),
                        org.hamcrest.Matchers.containsString("thinkingLevel"),
                        org.hamcrest.Matchers.containsString("LOW"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("test-api-key")))))
                .andRespond(withSuccess(successResponse("COVERED", 0.9, "Disclosed."), MediaType.APPLICATION_JSON));

        ComplianceClassificationResult result = provider.classify(sampleRequest());

        assertThat(result.status()).isEqualTo(AssessmentStatus.COVERED);
        mockServer.verify();
    }

    @Test
    void retriesHttp429AndSucceeds() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withSuccess(successResponse("COVERED", 0.88, "Retried."), MediaType.APPLICATION_JSON));

        ComplianceClassificationResult result = provider.classify(sampleRequest());

        assertThat(result.status()).isEqualTo(AssessmentStatus.COVERED);
        assertThat(result.confidence()).isEqualTo(0.88);
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

        ComplianceClassificationException ex = assertThrows(ComplianceClassificationException.class, () ->
                provider.classify(sampleRequest()));

        assertThat(ex.getCategory()).isEqualTo(ClassificationFailureCategory.HTTP_SERVER_ERROR);
        assertThat(ex.getHttpStatus()).isEqualTo(503);
        assertThat(ex.getAttempt()).isEqualTo(3);
        assertThat(ex.isRetryable()).isTrue();
        mockServer.verify();
    }

    @Test
    void doesNotRetryHttp401() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withUnauthorizedRequest());

        ComplianceClassificationException ex = assertThrows(ComplianceClassificationException.class, () ->
                provider.classify(sampleRequest()));

        assertThat(ex.getCategory()).isEqualTo(ClassificationFailureCategory.CONFIGURATION_ERROR);
        assertThat(ex.getHttpStatus()).isEqualTo(401);
        assertThat(ex.getAttempt()).isEqualTo(1);
        assertThat(ex.isRetryable()).isFalse();
        mockServer.verify();
    }

    @Test
    void doesNotRetryValidationFailures() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withSuccess(successResponse("INVALID", 0.5, "x"), MediaType.APPLICATION_JSON));

        ComplianceClassificationException ex = assertThrows(ComplianceClassificationException.class, () ->
                provider.classify(sampleRequest()));

        assertThat(ex.getCategory()).isEqualTo(ClassificationFailureCategory.VALIDATION_FAILURE);
        assertThat(ex.getAttempt()).isEqualTo(1);
        mockServer.verify();
    }

    @Test
    void loggingDoesNotExposeApiKey() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("generateContent")))
                .andRespond(withUnauthorizedRequest());

        assertThrows(ComplianceClassificationException.class, () -> provider.classify(sampleRequest()));

        String combinedLogs = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", String::concat);
        assertThat(combinedLogs).doesNotContain("test-api-key");
        assertThat(combinedLogs).contains("category=");
    }

    @Test
    void rejectsMissingApiKey() {
        properties.setApiKey("");

        ComplianceClassificationException ex = assertThrows(ComplianceClassificationException.class, () ->
                provider.classify(sampleRequest()));

        assertThat(ex.getCategory()).isEqualTo(ClassificationFailureCategory.CONFIGURATION_ERROR);
    }

    @Test
    void rejectsEmptyEvidencePassages() {
        ComplianceClassificationException ex = assertThrows(ComplianceClassificationException.class, () ->
                provider.classify(new ComplianceClassificationRequest(
                        "ENV-003", "Title", "Desc", "Text", List.of())));

        assertThat(ex.getCategory()).isEqualTo(ClassificationFailureCategory.VALIDATION_FAILURE);
    }

    @Test
    void extractResponseTextRejectsEmptyCandidates() {
        ComplianceClassificationException ex = assertThrows(ComplianceClassificationException.class, () ->
                provider.extractResponseText("{\"candidates\":[]}", 1));

        assertThat(ex.getCategory()).isEqualTo(ClassificationFailureCategory.BLOCKED_RESPONSE);
    }

    private ComplianceClassificationRequest sampleRequest() {
        return new ComplianceClassificationRequest(
                "ENV-003", "Scope 1 emissions", "Disclose Scope 1", "Framework text",
                List.of("Scope 1 emissions totalled 1000 tCO2e."));
    }

    private String successResponse(String status, double confidence, String explanation) {
        return """
                {
                  "candidates": [{
                    "finishReason": "STOP",
                    "content": {
                      "parts": [{
                        "text": "{\\"status\\":\\"%s\\",\\"confidence\\":%s,\\"explanation\\":\\"%s\\",\\"gap\\":null,\\"recommendation\\":null}"
                      }]
                    }
                  }]
                }
                """.formatted(status, confidence, explanation);
    }
}
