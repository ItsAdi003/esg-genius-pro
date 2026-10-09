package dev.esgenius.service;

import java.util.UUID;

/**
 * Resolved caller for document ownership checks.
 * {@code userId} is null when the request has no authenticated identity.
 */
public record Caller(UUID userId, boolean admin) {

    public static Caller unidentifiedAdmin() {
        return new Caller(null, true);
    }

    public static Caller anonymous() {
        return new Caller(null, false);
    }
}
