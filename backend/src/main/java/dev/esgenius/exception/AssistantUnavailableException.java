package dev.esgenius.exception;

import org.springframework.http.HttpStatus;

/**
 * The assistant retrieved evidence but could not produce a grounded answer.
 */
public class AssistantUnavailableException extends RuntimeException {

    private final HttpStatus httpStatus;

    public AssistantUnavailableException(String message) {
        this(HttpStatus.BAD_GATEWAY, message);
    }

    public AssistantUnavailableException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus == null ? HttpStatus.BAD_GATEWAY : httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
