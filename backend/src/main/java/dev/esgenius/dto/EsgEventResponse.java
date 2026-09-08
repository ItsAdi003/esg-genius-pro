package dev.esgenius.dto;

/**
 * ESG event or controversy response.
 */
public record EsgEventResponse(
        Long id,
        String title,
        String description,
        String pillar,
        String severity,
        String eventDate,
        Double scoreImpact,
        Boolean isPrototype) {
}
