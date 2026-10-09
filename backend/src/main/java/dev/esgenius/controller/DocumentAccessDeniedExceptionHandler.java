package dev.esgenius.controller;

import dev.esgenius.service.DocumentAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class DocumentAccessDeniedExceptionHandler {

    @ExceptionHandler(DocumentAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentAccessDenied(DocumentAccessDeniedException ex) {
        Map<String, Object> body = Map.of(
                "status", HttpStatus.FORBIDDEN.value(),
                "error", HttpStatus.FORBIDDEN.getReasonPhrase(),
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}
