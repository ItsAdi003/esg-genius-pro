package dev.esgenius.exception;

/**
 * The assistant retrieved evidence but could not produce a grounded answer.
 */
public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException(String message) {
        super(message);
    }
}
