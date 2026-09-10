package dev.esgenius.dto;

import java.time.Instant;

/**
 * Full document metadata including extracted text for detail views.
 */
public record DocumentDetailResponse(
        Long id,
        Long organizationId,
        String organizationName,
        String originalFilename,
        String documentType,
        Integer reportingYear,
        String status,
        Long fileSize,
        Integer pageCount,
        Instant uploadedAt,
        Instant processedAt,
        String extractedText,
        String failureReason) {
}
