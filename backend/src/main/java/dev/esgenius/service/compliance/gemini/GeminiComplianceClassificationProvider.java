package dev.esgenius.service.compliance.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.esgenius.config.GeminiProperties;
import dev.esgenius.service.compliance.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

public class GeminiComplianceClassificationProvider implements ComplianceClassificationProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiComplianceClassificationProvider.class);
    private static final Set<Integer> RETRYABLE_HTTP_STATUSES = Set.of(429, 500, 502, 503, 504);
    private static final Set<String> BLOCKED_FINISH_REASONS = Set.of(
            "SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "MALFORMED_FUNCTION_CALL");

    private final GeminiProperties properties;
    private final RestClient restClient;
    private final ComplianceClassificationPromptBuilder promptBuilder;
    private final ComplianceClassificationResultParser resultParser;
    private final ObjectMapper objectMapper;

    public GeminiComplianceClassificationProvider(
            GeminiProperties properties,
            RestClient geminiRestClient,
            ComplianceClassificationPromptBuilder promptBuilder,
            ComplianceClassificationResultParser resultParser,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = geminiRestClient;
        this.promptBuilder = promptBuilder;
        this.resultParser = resultParser;
        this.objectMapper = objectMapper;
    }

    @Override
    public ComplianceClassificationResult classify(ComplianceClassificationRequest request) {
        if (!properties.isConfigured()) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.CONFIGURATION_ERROR,
                    "Gemini API key is not configured. Set GEMINI_API_KEY to enable compliance classification.",
                    false);
        }

        if (request.evidencePassages() == null || request.evidencePassages().isEmpty()) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.VALIDATION_FAILURE,
                    "Classification requires retrieved evidence passages; none were provided.",
                    false);
        }

        String prompt = promptBuilder.buildPrompt(request);
        String requestBody = buildRequestBody(prompt);
        int maxAttempts = properties.getMaxRetries() + 1;

        ComplianceClassificationException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeRequest(request.requirementCode(), requestBody, attempt);
            } catch (ComplianceClassificationException ex) {
                lastFailure = ex.withAttempt(attempt);
                logClassificationFailure(request.requirementCode(), lastFailure);

                if (!ex.isRetryable() || attempt >= maxAttempts) {
                    throw lastFailure;
                }

                sleepBeforeRetry(attempt, ex.getHttpStatus());
            }
        }

        throw lastFailure != null ? lastFailure : new ComplianceClassificationException(
                ClassificationFailureCategory.OTHER,
                "Gemini classification failed after retries",
                false);
    }

    private ComplianceClassificationResult executeRequest(
            String requirementCode, String requestBody, int attempt) {
        try {
            String responseBody = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={apiKey}",
                            properties.getModel(), properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, response) -> {
                        int status = response.getStatusCode().value();
                        String safeMessage = extractSafeErrorMessage(response.getHeaders(), status);
                        throw buildHttpException(status, safeMessage, attempt);
                    })
                    .body(String.class);

            String jsonText = extractResponseText(responseBody, attempt);
            return resultParser.parse(jsonText);
        } catch (ComplianceClassificationException ex) {
            throw ex.withAttempt(attempt);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            String safeMessage = extractSafeErrorMessage(ex.getResponseHeaders(), status);
            ComplianceClassificationException httpException = buildHttpException(status, safeMessage, attempt);
            throw new ComplianceClassificationException(
                    httpException.getCategory(),
                    httpException.getMessage(),
                    httpException.isRetryable(),
                    httpException.getHttpStatus(),
                    attempt,
                    httpException.getSafeDetail(),
                    ex);
        } catch (ResourceAccessException ex) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.NETWORK_TIMEOUT,
                    "Gemini API request timed out or failed to connect",
                    true,
                    null,
                    attempt,
                    ex.getMessage(),
                    ex);
        } catch (Exception ex) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.OTHER,
                    "Gemini API request failed",
                    false,
                    null,
                    attempt,
                    ex.getMessage(),
                    ex);
        }
    }

    private ComplianceClassificationException buildHttpException(
            int status, String safeMessage, int attempt) {
        ClassificationFailureCategory category;
        boolean retryable = RETRYABLE_HTTP_STATUSES.contains(status);
        if (status == 429) {
            category = ClassificationFailureCategory.HTTP_RATE_LIMIT;
        } else if (status >= 500) {
            category = ClassificationFailureCategory.HTTP_SERVER_ERROR;
        } else if (status == 401 || status == 403) {
            category = ClassificationFailureCategory.CONFIGURATION_ERROR;
        } else if (status >= 400) {
            category = ClassificationFailureCategory.HTTP_CLIENT_ERROR;
        } else {
            category = ClassificationFailureCategory.OTHER;
        }

        return new ComplianceClassificationException(
                category,
                "Gemini API returned HTTP " + status,
                retryable,
                status,
                attempt,
                safeMessage);
    }

    private void logClassificationFailure(String requirementCode, ComplianceClassificationException ex) {
        log.warn(
                "Gemini classification failed requirementCode={} category={} httpStatus={} attempt={} retryable={} detail={}",
                requirementCode,
                ex.getCategory(),
                ex.getHttpStatus(),
                ex.getAttempt(),
                ex.isRetryable(),
                ex.getSafeDetail());
    }

    private void sleepBeforeRetry(int attempt, Integer httpStatus) {
        long backoffMs = Math.min(
                properties.getMaxRetryBackoff().toMillis(),
                properties.getInitialRetryBackoff().toMillis() * (1L << (attempt - 1)));
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.OTHER,
                    "Gemini classification retry interrupted",
                    false,
                    httpStatus,
                    attempt,
                    interrupted.getMessage(),
                    interrupted);
        }
    }

    String buildRequestBody(String prompt) {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", "user");
        ArrayNode parts = objectMapper.createArrayNode();
        parts.addObject().put("text", prompt);
        content.set("parts", parts);

        ArrayNode contents = objectMapper.createArrayNode();
        contents.add(content);
        root.set("contents", contents);

        ObjectNode generationConfig = objectMapper.createObjectNode();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseSchema", buildResponseSchema());

        ObjectNode thinkingConfig = objectMapper.createObjectNode();
        thinkingConfig.put("thinkingLevel", properties.getThinkingLevel().toUpperCase(Locale.ROOT));
        generationConfig.set("thinkingConfig", thinkingConfig);

        root.set("generationConfig", generationConfig);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.OTHER,
                    "Failed to build Gemini request body",
                    false,
                    null,
                    1,
                    ex.getMessage(),
                    ex);
        }
    }

    private ObjectNode buildResponseSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode status = objectMapper.createObjectNode();
        status.put("type", "string");
        ArrayNode statusEnum = objectMapper.createArrayNode();
        statusEnum.add("COVERED");
        statusEnum.add("PARTIALLY_COVERED");
        statusEnum.add("NOT_COVERED");
        statusEnum.add("HUMAN_REVIEW_REQUIRED");
        status.set("enum", statusEnum);
        properties.set("status", status);

        ObjectNode confidence = objectMapper.createObjectNode();
        confidence.put("type", "number");
        properties.set("confidence", confidence);

        ObjectNode explanation = objectMapper.createObjectNode();
        explanation.put("type", "string");
        properties.set("explanation", explanation);

        ObjectNode gap = objectMapper.createObjectNode();
        gap.put("type", "string");
        properties.set("gap", gap);

        ObjectNode recommendation = objectMapper.createObjectNode();
        recommendation.put("type", "string");
        properties.set("recommendation", recommendation);

        schema.set("properties", properties);

        ArrayNode required = objectMapper.createArrayNode();
        required.add("status");
        required.add("confidence");
        required.add("explanation");
        required.add("gap");
        required.add("recommendation");
        schema.set("required", required);

        return schema;
    }

    String extractResponseText(String responseBody, int attempt) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                JsonNode promptFeedback = root.path("promptFeedback");
                String blockReason = promptFeedback.path("blockReason").asText(null);
                throw new ComplianceClassificationException(
                        ClassificationFailureCategory.BLOCKED_RESPONSE,
                        blockReason != null
                                ? "Gemini blocked the classification response: " + blockReason
                                : "Gemini response contained no candidates",
                        false,
                        null,
                        attempt,
                        blockReason);
            }

            JsonNode firstCandidate = candidates.get(0);
            String finishReason = firstCandidate.path("finishReason").asText(null);
            if (finishReason != null && BLOCKED_FINISH_REASONS.contains(finishReason)) {
                throw new ComplianceClassificationException(
                        ClassificationFailureCategory.BLOCKED_RESPONSE,
                        "Gemini response blocked with finishReason=" + finishReason,
                        false,
                        null,
                        attempt,
                        finishReason);
            }

            JsonNode parts = firstCandidate.path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new ComplianceClassificationException(
                        ClassificationFailureCategory.MALFORMED_RESPONSE,
                        "Gemini response contained no content parts",
                        false,
                        null,
                        attempt,
                        finishReason);
            }

            String text = parts.get(0).path("text").asText(null);
            if (text == null || text.isBlank()) {
                throw new ComplianceClassificationException(
                        ClassificationFailureCategory.MALFORMED_RESPONSE,
                        "Gemini response text was empty",
                        false,
                        null,
                        attempt,
                        finishReason);
            }
            return text;
        } catch (ComplianceClassificationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.MALFORMED_RESPONSE,
                    "Failed to parse Gemini API response envelope",
                    false,
                    null,
                    attempt,
                    ex.getMessage(),
                    ex);
        }
    }

    String extractSafeErrorMessage(HttpHeaders headers, int status) {
        if (headers != null) {
            String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
            if (retryAfter != null && !retryAfter.isBlank()) {
                return "Retry-After=" + retryAfter;
            }
        }
        if (status == 429) {
            return "Rate limit exceeded";
        }
        if (status == 401 || status == 403) {
            return "Gemini API authentication or permission error";
        }
        if (status >= 500) {
            return "Gemini API server error";
        }
        return "Gemini API client error";
    }
}
