package dev.esgenius.service.assistant.gemini;

import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GeminiHttpErrorClassifier {

    private static final Pattern RETRY_DELAY = Pattern.compile(
            "\"retryDelay\"\\s*:\\s*\"(\\d+(?:\\.\\d+)?)s\"",
            Pattern.CASE_INSENSITIVE);

    private GeminiHttpErrorClassifier() {
    }

    static boolean isPerDayQuota(String responseBody) {
        return responseBody != null && responseBody.contains("PerDay");
    }

    static Duration parseRetryDelay(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        Matcher matcher = RETRY_DELAY.matcher(responseBody);
        if (!matcher.find()) {
            return null;
        }
        double seconds = Double.parseDouble(matcher.group(1));
        long millis = Math.round(seconds * 1000.0);
        if (millis <= 0) {
            return null;
        }
        return Duration.ofMillis(millis);
    }

    static String safeDetail(HttpHeaders headers, int status, String responseBody) {
        if (status == 429 && isPerDayQuota(responseBody)) {
            return "Daily quota exhausted";
        }
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
