package dev.esgenius.dto;

import java.time.Instant;

/**
 * Document metadata for list views. Does not include extracted text.
 */
public record DocumentSummaryResponse(
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
        Instant processedAt) {
}
