package dev.esgenius.config;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;
import java.util.UUID;

/**
 * Caller identity taken from a successful Supabase {@code /auth/v1/user} response.
 */
public record AuthenticatedUser(UUID userId, String email) {

    public static final String REQUEST_ATTRIBUTE = "authenticatedUser";

    public static Optional<AuthenticatedUser> from(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        Object value = request.getAttribute(REQUEST_ATTRIBUTE);
        if (value instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
