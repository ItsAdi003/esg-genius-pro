package dev.esgenius.service;

/**
 * Caller may see the document but may not delete it or start an analysis.
 */
public class DocumentAccessDeniedException extends RuntimeException {

    public DocumentAccessDeniedException(String message) {
        super(message);
    }
}
