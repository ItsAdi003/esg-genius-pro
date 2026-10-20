package dev.esgenius.service.compliance.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.esgenius.config.GeminiProperties;
import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.service.compliance.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Gemini batch classification: one HTTP call answers several requirements. Never calls Google. */
class GeminiBatchClassificationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer mockServer;
    private GeminiComplianceClassificationProvider provider;
    private AnalysisRunContext runContext;

    @BeforeEach
    void setUp() {
        GeminiProperties properties = new GeminiProperties();
        properties.setApiKey("test-api-key");
        properties.setModel("gemini-3.5-flash");
        properties.setBaseUrl("https://generativelanguage.googleapis.com");
        properties.setMaxRetries(2);
        properties.setInitialRetryBackoff(Duration.ofMillis(1));
        properties.setMaxRetryBackoff(Duration.ofMillis(5));

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new GeminiComplianceClassificationProvider(
                properties,
                builder.build(),
                new ComplianceClassificationPromptBuilder(),
                new ComplianceClassificationResultParser(objectMapper),
                objectMapper);
        runContext = new AnalysisRunContext();
    }

    @Test
    void severalRequirementsAreAnsweredByOneHttpCall() {
        mockServer.expect(requestTo(containsString("generateContent")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(allOf(
                        containsString("\"type\":\"array\""),
                        containsString("requirementCode"),
                        containsString("P6-E01"),
                        containsString("P6-E03"),
                        containsString("P6-E06"))))
                .andRespond(withSuccess(
                        envelope(entry("P6-E01", "COVERED"), entry("P6-E03", "PARTIALLY_COVERED"),
                                entry("P6-E06", "NOT_COVERED")),
                        MediaType.APPLICATION_JSON));

        Map<String, ComplianceClassificationResult> results = provider.classifyBatch(List.of(
                request("P6-E01"), request("P6-E03"), request("P6-E06")));

        assertThat(results).containsOnlyKeys("P6-E01", "P6-E03", "P6-E06");
        assertThat(results.get("P6-E01").status()).isEqualTo(AssessmentStatus.COVERED);
        assertThat(results.get("P6-E03").status()).isEqualTo(AssessmentStatus.PARTIALLY_COVERED);
        assertThat(results.get("P6-E06").status()).isEqualTo(AssessmentStatus.NOT_COVERED);
        mockServer.verify();
    }

    @Test
    void requirementsTheModelOmittedAreAbsentFromTheResult() {
        mockServer.expect(requestTo(containsString("generateContent")))
                .andRespond(withSuccess(envelope(entry("P3-E10", "COVERED")), MediaType.APPLICATION_JSON));

        Map<String, ComplianceClassificationResult> results = provider.classifyBatch(List.of(
                request("P3-E10"), request("P3-E11")));

        assertThat(results).containsOnlyKeys("P3-E10");
        mockServer.verify();
    }

    @Test
    void aSingleRequirementBatchUsesTheSingleRequirementCall() {
        mockServer.expect(requestTo(containsString("generateContent")))
                .andExpect(content().string(allOf(containsString("\"type\":\"object\""))))
                .andRespond(withSuccess(singleEnvelope("COVERED"), MediaType.APPLICATION_JSON));

        Map<String, ComplianceClassificationResult> results = provider.classifyBatch(List.of(request("P1-E04")));

        assertThat(results.get("P1-E04").status()).isEqualTo(AssessmentStatus.COVERED);
        mockServer.verify();
    }

    @Test
    void emptyBatchMakesNoCall() {
        assertThat(provider.classifyBatch(List.of())).isEmpty();
        mockServer.verify();
    }

    @Test
    void retriesAWholeBatchOnRateLimitThenSucceeds() {
        mockServer.expect(requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mockServer.expect(requestTo(containsString("generateContent")))
                .andRespond(withSuccess(
                        envelope(entry("P1-E04", "COVERED"), entry("P1-E05", "COVERED")), MediaType.APPLICATION_JSON));

        Map<String, ComplianceClassificationResult> results = provider.classifyBatch(List.of(
                request("P1-E04"), request("P1-E05")));

        assertThat(results).hasSize(2);
        mockServer.verify();
    }

    @Test
    void dailyQuotaFailsTheBatchAndMarksTheRunContext() {
        mockServer.expect(requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"code":429,"message":"Resource exhausted.","status":"RESOURCE_EXHAUSTED",
                                "details":[{"@type":"type.googleapis.com/google.rpc.QuotaFailure",
                                "violations":[{"quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier"}]}]}}
                                """));

        ComplianceClassificationException failure = assertThrows(ComplianceClassificationException.class,
                () -> provider.classifyBatch(List.of(request("A-01"), request("A-02"))));

        assertThat(failure.getCategory()).isEqualTo(ClassificationFailureCategory.QUOTA_EXHAUSTED);
        assertThat(runContext.isQuotaExhausted()).isTrue();
        mockServer.verify();
    }

    @Test
    void doesNotCallGeminiOnceQuotaIsAlreadyExhausted() {
        runContext.markQuotaExhausted();

        ComplianceClassificationException failure = assertThrows(ComplianceClassificationException.class,
                () -> provider.classifyBatch(List.of(request("A-01"), request("A-02"))));

        assertThat(failure.getCategory()).isEqualTo(ClassificationFailureCategory.QUOTA_EXHAUSTED);
        mockServer.verify();
    }

    @Test
    void rejectsABatchWithARequirementThatHasNoEvidence() {
        ComplianceClassificationRequest empty = new ComplianceClassificationRequest(
                "A-02", "Title", "Description", "Text", List.of(), runContext);

        ComplianceClassificationException failure = assertThrows(ComplianceClassificationException.class,
                () -> provider.classifyBatch(List.of(request("A-01"), empty)));

        assertThat(failure.getCategory()).isEqualTo(ClassificationFailureCategory.VALIDATION_FAILURE);
        assertThat(failure.getMessage()).contains("A-02");
        mockServer.verify();
    }

    @Test
    void anAnswerThatIsNotAnArrayFailsTheBatch() {
        mockServer.expect(requestTo(containsString("generateContent")))
                .andRespond(withSuccess(singleEnvelope("COVERED"), MediaType.APPLICATION_JSON));

        ComplianceClassificationException failure = assertThrows(ComplianceClassificationException.class,
                () -> provider.classifyBatch(List.of(request("A-01"), request("A-02"))));

        assertThat(failure.getCategory()).isEqualTo(ClassificationFailureCategory.PARSE_FAILURE);
        mockServer.verify();
    }

    private ComplianceClassificationRequest request(String code) {
        return new ComplianceClassificationRequest(
                code, "Title " + code, "Description " + code, "Framework text " + code,
                List.of("Evidence passage for " + code + "."), runContext);
    }

    private ObjectNode entry(String code, String status) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("requirementCode", code);
        node.put("status", status);
        node.put("confidence", 0.9);
        node.put("explanation", "Explanation for " + code);
        node.putNull("gap");
        node.putNull("recommendation");
        return node;
    }

    private String envelope(ObjectNode... entries) {
        ArrayNode array = objectMapper.createArrayNode();
        for (ObjectNode entry : entries) {
            array.add(entry);
        }
        return wrap(array.toString());
    }

    private String singleEnvelope(String status) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("status", status);
        node.put("confidence", 0.9);
        node.put("explanation", "Single");
        node.putNull("gap");
        node.putNull("recommendation");
        return wrap(node.toString());
    }

    private String wrap(String jsonText) {
        ObjectNode part = objectMapper.createObjectNode();
        part.put("text", jsonText);
        ObjectNode content = objectMapper.createObjectNode();
        content.set("parts", objectMapper.createArrayNode().add(part));
        ObjectNode candidate = objectMapper.createObjectNode();
        candidate.put("finishReason", "STOP");
        candidate.set("content", content);
        ObjectNode root = objectMapper.createObjectNode();
        root.set("candidates", objectMapper.createArrayNode().add(candidate));
        return root.toString();
    }
}
