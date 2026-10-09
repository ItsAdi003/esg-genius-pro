package dev.esgenius.service.assistant.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.esgenius.config.GeminiProperties;
import dev.esgenius.service.assistant.AssistantAnswerException;
import dev.esgenius.service.assistant.AssistantAnswerFailureCategory;
import dev.esgenius.service.assistant.AssistantAnswerPromptBuilder;
import dev.esgenius.service.assistant.AssistantAnswerProvider;
import dev.esgenius.service.assistant.AssistantAnswerRequest;
import dev.esgenius.service.assistant.AssistantAnswerResult;
import dev.esgenius.service.assistant.AssistantAnswerResultParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

public class GeminiAssistantAnswerProvider implements AssistantAnswerProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiAssistantAnswerProvider.class);
    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final Set<Integer> RETRYABLE_HTTP_STATUSES = Set.of(429, 500, 502, 503, 504);
    private static final Set<String> BLOCKED_FINISH_REASONS = Set.of(
            "SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "MALFORMED_FUNCTION_CALL");

    private final GeminiProperties properties;
    private final RestClient restClient;
    private final AssistantAnswerPromptBuilder promptBuilder;
    private final AssistantAnswerResultParser resultParser;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public GeminiAssistantAnswerProvider(
            GeminiProperties properties,
            RestClient geminiRestClient,
            AssistantAnswerPromptBuilder promptBuilder,
            AssistantAnswerResultParser resultParser,
            ObjectMapper objectMapper) {
        this(properties, geminiRestClient, promptBuilder, resultParser, objectMapper, Clock.systemUTC());
    }

    GeminiAssistantAnswerProvider(
            GeminiProperties properties,
            RestClient geminiRestClient,
            AssistantAnswerPromptBuilder promptBuilder,
            AssistantAnswerResultParser resultParser,
            ObjectMapper objectMapper,
            Clock clock) {
        this.properties = properties;
        this.restClient = geminiRestClient;
        this.promptBuilder = promptBuilder;
        this.resultParser = resultParser;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public AssistantAnswerResult answer(AssistantAnswerRequest request) {
        if (!properties.isConfigured()) {
            throw new AssistantAnswerException(
                    AssistantAnswerFailureCategory.CONFIGURATION_ERROR,
                    "Gemini API key is not configured. Set GEMINI_API_KEY to enable the document assistant.",
                    false);
        }

        if (request.evidencePassages() == null || request.evidencePassages().isEmpty()) {
            throw new AssistantAnswerException(
                    AssistantAnswerFailureCategory.VALIDATION_FAILURE,
                    "An assistant answer requires retrieved evidence passages; none were provided.",
                    false);
        }

        String prompt = promptBuilder.buildPrompt(request);
        String requestBody = buildRequestBody(prompt);
        int maxAttempts = properties.getMaxRetries() + 1;
        Instant startedAt = clock.instant();

        AssistantAnswerException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeRequest(request, requestBody, attempt);
            } catch (AssistantAnswerException ex) {
                lastFailure = ex.withAttempt(attempt);
                logAnswerFailure(lastFailure);

                if (!ex.isRetryable() || attempt >= maxAttempts) {
                    throw lastFailure;
                }

                Duration backoff = backoffBeforeRetry(attempt, lastFailure);
                if (retryWouldExceedDeadline(startedAt, backoff)) {
                    AssistantAnswerException deadlineFailure = deadlineExceeded(attempt);
                    logAnswerFailure(deadlineFailure);
                    throw deadlineFailure;
                }

                sleepBeforeRetry(attempt, backoff, lastFailure);
            }
        }

        throw lastFailure != null ? lastFailure : new AssistantAnswerException(
                AssistantAnswerFailureCategory.OTHER,
                "Gemini assistant answer failed after retries",
                false);
    }

    private AssistantAnswerResult executeRequest(
            AssistantAnswerRequest request, String requestBody, int attempt) {
        try {
            String responseBody = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.getModel())
                    .header(API_KEY_HEADER, properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, response) -> {
                        int status = response.getStatusCode().value();
                        String errorBody = readErrorBody(response);
                        String safeMessage = extractSafeErrorMessage(response.getHeaders(), status, errorBody);
                        throw buildHttpException(status, safeMessage, attempt, errorBody);
                    })
                    .body(String.class);

            String jsonText = extractResponseText(responseBody, attempt);
            return resultParser.parse(jsonText, request.evidencePassages().size());
        } catch (AssistantAnswerException ex) {
            throw ex.withAttempt(attempt);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            String errorBody = ex.getResponseBodyAsString();
            String safeMessage = extractSafeErrorMessage(ex.getResponseHeaders(), status, errorBody);
            AssistantAnswerException httpException = buildHttpException(status, safeMessage, attempt, errorBody);
            throw new AssistantAnswerException(
                    httpException.getCategory(),
                    httpException.getMessage(),
                    httpException.isRetryable(),
                    httpException.getHttpStatus(),
                    attempt,
                    httpException.getSafeDetail(),
                    ex,
                    httpException.getRetryAfter());
        } catch (ResourceAccessException ex) {
            throw new AssistantAnswerException(
                    AssistantAnswerFailureCategory.NETWORK_TIMEOUT,
                    "Gemini API request timed out or failed to connect",
                    true,
                    null,
                    attempt,
                    ex.getMessage(),
                    ex);
        } catch (Exception ex) {
            throw new AssistantAnswerException(
                    AssistantAnswerFailureCategory.OTHER,
                    "Gemini API request failed",
                    false,
                    null,
                    attempt,
                    ex.getMessage(),
                    ex);
        }
    }

    private AssistantAnswerException buildHttpException(
            int status, String safeMessage, int attempt, String errorBody) {
        AssistantAnswerFailureCategory category;
        boolean retryable = RETRYABLE_HTTP_STATUSES.contains(status);
        Duration retryAfter = null;
        if (status == 429 && GeminiHttpErrorClassifier.isPerDayQuota(errorBody)) {
            category = AssistantAnswerFailureCategory.QUOTA_EXHAUSTED;
            retryable = false;
        } else if (status == 429) {
            category = AssistantAnswerFailureCategory.HTTP_RATE_LIMIT;
            retryAfter = GeminiHttpErrorClassifier.parseRetryDelay(errorBody);
        } else if (status >= 500) {
            category = AssistantAnswerFailureCategory.HTTP_SERVER_ERROR;
        } else if (status == 401 || status == 403) {
            category = AssistantAnswerFailureCategory.CONFIGURATION_ERROR;
        } else if (status >= 400) {
            category = AssistantAnswerFailureCategory.HTTP_CLIENT_ERROR;
        } else {
            category = AssistantAnswerFailureCategory.OTHER;
        }

        return new AssistantAnswerException(
                category,
                "Gemini API returned HTTP " + status,
                retryable,
                status,
                attempt,
                safeMessage,
                null,
                retryAfter);
    }

    private void logAnswerFailure(AssistantAnswerException ex) {
        log.warn(
                "Gemini assistant answer failed category={} httpStatus={} attempt={} retryable={} detail={}",
                ex.getCategory(),
                ex.getHttpStatus(),
                ex.getAttempt(),
                ex.isRetryable(),
                ex.getSafeDetail());
    }

    private Duration backoffBeforeRetry(int attempt, AssistantAnswerException failure) {
        long backoffMs = Math.min(
                properties.getMaxRetryBackoff().toMillis(),
                properties.getInitialRetryBackoff().toMillis() * (1L << (attempt - 1)));
        Duration retryAfter = failure.getRetryAfter();
        if (retryAfter != null && !retryAfter.isZero() && !retryAfter.isNegative()) {
            backoffMs = Math.min(properties.getMaxRetryBackoff().toMillis(), retryAfter.toMillis());
        }
        return Duration.ofMillis(Math.max(0, backoffMs));
    }

    private boolean retryWouldExceedDeadline(Instant startedAt, Duration backoff) {
        // The next call can block for a full read timeout, so count that before starting it.
        Duration readTimeout = properties.getReadTimeout() != null ? properties.getReadTimeout() : Duration.ZERO;
        Duration projected = Duration.between(startedAt, clock.instant()).plus(backoff).plus(readTimeout);
        return projected.compareTo(properties.getAssistantTotalDeadline()) > 0;
    }

    private AssistantAnswerException deadlineExceeded(int attempt) {
        return new AssistantAnswerException(
                AssistantAnswerFailureCategory.HTTP_SERVER_ERROR,
                "Gemini API returned HTTP 502",
                false,
                502,
                attempt,
                "Assistant request exceeded the total time limit");
    }

    private void sleepBeforeRetry(int attempt, Duration backoff, AssistantAnswerException failure) {
        try {
            Thread.sleep(backoff.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssistantAnswerException(
                    AssistantAnswerFailureCategory.OTHER,
                    "Gemini assistant answer retry interrupted",
                    false,
                    failure.getHttpStatus(),
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
            throw new AssistantAnswerException(
                    AssistantAnswerFailureCategory.OTHER,
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

        ObjectNode propertiesNode = objectMapper.createObjectNode();

        ObjectNode answer = objectMapper.createObjectNode();
        answer.put("type", "string");
        propertiesNode.set("answer", answer);

        ObjectNode passageIndices = objectMapper.createObjectNode();
        passageIndices.put("type", "array");
        ObjectNode items = objectMapper.createObjectNode();
        items.put("type", "integer");
        passageIndices.set("items", items);
        propertiesNode.set("passageIndices", passageIndices);

        schema.set("properties", propertiesNode);

        ArrayNode required = objectMapper.createArrayNode();
        required.add("answer");
        required.add("passageIndices");
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
                throw new AssistantAnswerException(
                        AssistantAnswerFailureCategory.BLOCKED_RESPONSE,
                        blockReason != null
                                ? "Gemini blocked the assistant response: " + blockReason
                                : "Gemini response contained no candidates",
                        false,
                        null,
                        attempt,
                        blockReason);
            }

            JsonNode firstCandidate = candidates.get(0);
            String finishReason = firstCandidate.path("finishReason").asText(null);
            if (finishReason != null && BLOCKED_FINISH_REASONS.contains(finishReason)) {
                throw new AssistantAnswerException(
                        AssistantAnswerFailureCategory.BLOCKED_RESPONSE,
                        "Gemini response blocked with finishReason=" + finishReason,
                        false,
                        null,
                        attempt,
                        finishReason);
            }

            JsonNode parts = firstCandidate.path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new AssistantAnswerException(
                        AssistantAnswerFailureCategory.MALFORMED_RESPONSE,
                        "Gemini response contained no content parts",
                        false,
                        null,
                        attempt,
                        finishReason);
            }

            String text = parts.get(0).path("text").asText(null);
            if (text == null || text.isBlank()) {
                throw new AssistantAnswerException(
                        AssistantAnswerFailureCategory.MALFORMED_RESPONSE,
                        "Gemini response text was empty",
                        false,
                        null,
                        attempt,
                        finishReason);
            }
            return text;
        } catch (AssistantAnswerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AssistantAnswerException(
                    AssistantAnswerFailureCategory.MALFORMED_RESPONSE,
                    "Failed to parse Gemini API response envelope",
                    false,
                    null,
                    attempt,
                    ex.getMessage(),
                    ex);
        }
    }

    String extractSafeErrorMessage(HttpHeaders headers, int status, String errorBody) {
        return GeminiHttpErrorClassifier.safeDetail(headers, status, errorBody);
    }

    private static String readErrorBody(ClientHttpResponse response) {
        try {
            byte[] bytes = response.getBody().readAllBytes();
            if (bytes.length == 0) {
                return "";
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }
}
