package dev.esgenius.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_export")
public class ReportExport {

    public static final String FORMAT_PDF = "PDF";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private ComplianceAnalysis analysis;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String format;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    protected ReportExport() {
    }

    public ReportExport(ComplianceAnalysis analysis, UUID userId, String format, long sizeBytes) {
        this(analysis, userId, format, sizeBytes, Instant.now());
    }

    public ReportExport(
            ComplianceAnalysis analysis, UUID userId, String format, long sizeBytes, Instant generatedAt) {
        this.analysis = analysis;
        this.userId = userId;
        this.format = format;
        this.sizeBytes = sizeBytes;
        this.generatedAt = generatedAt;
    }

    public Long getId() {
        return id;
    }

    public ComplianceAnalysis getAnalysis() {
        return analysis;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFormat() {
        return format;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
