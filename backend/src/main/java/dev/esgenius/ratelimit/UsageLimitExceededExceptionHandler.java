package dev.esgenius.ratelimit;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Maps usage-limit failures to HTTP 429. Ordered ahead of the generic
 * {@code Exception} handler so the response stays a client error.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class UsageLimitExceededExceptionHandler {

    @ExceptionHandler(UsageLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUsageLimitExceeded(UsageLimitExceededException ex) {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        Map<String, Object> body = Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.status(status)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getRetryAfterSeconds()))
                .body(body);
    }
}
