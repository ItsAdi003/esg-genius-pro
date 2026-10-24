package dev.esgenius.dto;

public record ApplicabilityResponse(Long requirementId, String assessmentStatus, String notApplicableReason) {
}
