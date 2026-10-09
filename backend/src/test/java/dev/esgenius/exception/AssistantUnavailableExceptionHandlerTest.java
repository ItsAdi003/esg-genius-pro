package dev.esgenius.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantUnavailableExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void genericAssistantFailureRemainsBadGateway() {
        ResponseEntity<Map<String, Object>> response = handler.handleAssistantUnavailable(
                new AssistantUnavailableException(
                        "The assistant could not generate an answer from the uploaded document. Please try again."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(502);
        assertThat(response.getBody().get("error")).isEqualTo("Bad Gateway");
        assertThat(response.getBody().get("message").toString()).doesNotContain("stack");
    }

    @Test
    void quotaExhaustionReturnsServiceUnavailableWithoutInternals() {
        ResponseEntity<Map<String, Object>> response = handler.handleAssistantUnavailable(
                new AssistantUnavailableException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "The AI service has reached its usage limit. Please try again later."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(503);
        assertThat(response.getBody().get("error")).isEqualTo("Service Unavailable");
        assertThat(response.getBody().get("message"))
                .isEqualTo("The AI service has reached its usage limit. Please try again later.");
        assertThat(response.getBody().get("message").toString()).doesNotContain("quotaId");
        assertThat(response.getBody().get("message").toString()).doesNotContain("RESOURCE_EXHAUSTED");
    }
}
