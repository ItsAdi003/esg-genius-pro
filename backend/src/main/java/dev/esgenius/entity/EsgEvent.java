package dev.esgenius.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;

/**
 * ESG Event: Records ESG-related news, developments, or controversies affecting
 * a company's rating.
 * 
 * PROTOTYPE DATA: Events marked with isPrototype=true are illustrative examples
 * used for architecture validation and NOT real current news.
 */
@Entity
@Table(name = "esg_event")
public class EsgEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /**
     * Event title.
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * Detailed description.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * ESG pillar affected.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EsgPillar pillar;

    /**
     * Optional: specific key issue related to this event.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "key_issue_id")
    private EsgKeyIssue keyIssue;

    /**
     * Event severity.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EsgEventSeverity severity;

    /**
     * Event date.
     */
    @Column(nullable = false)
    private LocalDate eventDate;

    /**
     * Prototype impact on overall score (e.g., +0.2 or -0.3).
     * Prototype/demo data only.
     */
    private Double scoreImpact;

    /**
     * News/event source name.
     */
    private String sourceName;

    /**
     * URL to the source (if applicable).
     */
    private String sourceUrl;

    /**
     * Flag: true if this is a prototype/demo event, not real current news.
     */
    @Column(nullable = false)
    private Boolean isPrototype = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EsgEvent() {
    }

    public EsgEvent(Organization organization, String title, String description,
            EsgPillar pillar, EsgEventSeverity severity, LocalDate eventDate,
            Double scoreImpact, Boolean isPrototype) {
        this.organization = organization;
        this.title = title;
        this.description = description;
        this.pillar = pillar;
        this.severity = severity;
        this.eventDate = eventDate;
        this.scoreImpact = scoreImpact;
        this.isPrototype = isPrototype != null ? isPrototype : true;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public EsgPillar getPillar() {
        return pillar;
    }

    public EsgKeyIssue getKeyIssue() {
        return keyIssue;
    }

    public void setKeyIssue(EsgKeyIssue keyIssue) {
        this.keyIssue = keyIssue;
    }

    public EsgEventSeverity getSeverity() {
        return severity;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public Double getScoreImpact() {
        return scoreImpact;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Boolean getIsPrototype() {
        return isPrototype;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
