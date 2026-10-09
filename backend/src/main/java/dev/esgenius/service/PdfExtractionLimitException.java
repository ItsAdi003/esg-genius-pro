package dev.esgenius.service;

import java.io.IOException;

/**
 * PDF was rejected by a configured extraction limit. The message is safe to show to users.
 */
public class PdfExtractionLimitException extends IOException {

    public PdfExtractionLimitException(String message) {
        super(message);
    }
}
