package dev.esgenius.exception;

/**
 * Thrown when a client request is invalid.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
