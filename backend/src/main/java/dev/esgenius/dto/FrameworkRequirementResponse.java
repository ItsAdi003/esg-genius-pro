package dev.esgenius.dto;

import dev.esgenius.entity.FrameworkRequirement;

/**
 * Response DTO for a single framework requirement.
 * Does NOT contain compliance status, evidence, or AI analysis fields.
 */
public record FrameworkRequirementResponse(
        Long id,
        String requirementCode,
        String title,
        String category,
        String description,
        String frameworkText,
        boolean mandatory,
        String version
) {
    public static FrameworkRequirementResponse from(FrameworkRequirement entity) {
        return new FrameworkRequirementResponse(
                entity.getId(),
                entity.getRequirementCode(),
                entity.getTitle(),
                entity.getCategory().name(),
                entity.getDescription(),
                entity.getFrameworkText(),
                entity.isMandatory(),
                entity.getVersion()
        );
    }
}
