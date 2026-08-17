package dev.esgenius.dto;

/**
 * Response body for the health check endpoint.
 */
public record HealthResponse(String status, String service) {
}
