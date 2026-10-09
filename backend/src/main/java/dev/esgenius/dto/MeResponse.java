package dev.esgenius.dto;

/**
 * Current caller and remaining per-user usage. A limit value of {@code null}
 * means that window is not limited.
 */
public record MeResponse(String email, boolean admin, Limits limits) {

    public record Limits(Limit uploadsPerDay, Limit analysesPerDay, Limit assistantAsksPerHour) {
    }

    public record Limit(int limit, int used, long resetsInSeconds) {
    }
}
