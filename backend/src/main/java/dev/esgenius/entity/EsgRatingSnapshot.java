package dev.esgenius.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;

/**
 * ESG Rating Snapshot: Captures a company's ESG state at a specific point in
 * time.
 * 
 * PROTOTYPE DATA: Scores and ratings are illustrative prototype values
 * used to validate the ESGenius rating architecture. They are NOT official
 * MSCI or third-party ESG ratings.
 */
@Entity
@Table(name = "esg_rating_snapshot")
public class EsgRatingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /**
     * Overall ESG score (0.0–10.0 scale).
     * This is prototype/demo data.
     */
    @Column(nullable = false)
    private Double overallScore;

    /**
     * Environmental pillar score (0.0–10.0 scale).
     */
    @Column(nullable = false)
    private Double environmentalScore;

    /**
     * Social pillar score (0.0–10.0 scale).
     */
    @Column(nullable = false)
    private Double socialScore;

    /**
     * Governance pillar score (0.0–10.0 scale).
     */
    @Column(nullable = false)
    private Double governanceScore;

    /**
     * ESGenius prototype rating band (AAA–CCC).
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EsgRatingBand ratingBand;

    /**
     * Assessment date (e.g., when the snapshot was created).
     */
    @Column(nullable = false)
    private LocalDate assessmentDate;

    /**
     * Previous overall score for comparison/trend tracking.
     */
    private Double previousOverallScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EsgRatingSnapshot() {
    }

    public EsgRatingSnapshot(Organization organization, Double overallScore,
            Double environmentalScore, Double socialScore,
            Double governanceScore, EsgRatingBand ratingBand,
            LocalDate assessmentDate, Double previousOverallScore) {
        this.organization = organization;
        this.overallScore = overallScore;
        this.environmentalScore = environmentalScore;
        this.socialScore = socialScore;
        this.governanceScore = governanceScore;
        this.ratingBand = ratingBand;
        this.assessmentDate = assessmentDate;
        this.previousOverallScore = previousOverallScore;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public Double getEnvironmentalScore() {
        return environmentalScore;
    }

    public Double getSocialScore() {
        return socialScore;
    }

    public Double getGovernanceScore() {
        return governanceScore;
    }

    public EsgRatingBand getRatingBand() {
        return ratingBand;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public Double getPreviousOverallScore() {
        return previousOverallScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
