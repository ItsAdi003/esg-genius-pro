package dev.esgenius.dto;

import dev.esgenius.entity.Framework;

/**
 * Response DTO for framework data exposed through the REST API.
 */
public record FrameworkResponse(
        Long id,
        String code,
        String name,
        String fullName,
        String region,
        String version,
        String status,
        int requirementCount
) {
    public static FrameworkResponse from(Framework entity, int requirementCount) {
        return new FrameworkResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getFullName(),
                entity.getRegion(),
                entity.getVersion(),
                entity.getStatus().name(),
                requirementCount
        );
    }
}
