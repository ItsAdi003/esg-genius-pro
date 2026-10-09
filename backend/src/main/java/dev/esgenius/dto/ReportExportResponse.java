package dev.esgenius.dto;

import java.time.Instant;

public record ReportExportResponse(
        Long id,
        Long analysisId,
        Long documentId,
        String documentName,
        String frameworkCode,
        String format,
        Long sizeBytes,
        Instant generatedAt) {
}
