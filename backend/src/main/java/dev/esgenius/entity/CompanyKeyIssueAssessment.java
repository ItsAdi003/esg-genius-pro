package dev.esgenius.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Company Key Issue Assessment: Links an organization to a material ESG issue
 * with a score and risk level.
 * 
 * PROTOTYPE DATA: Scores and risk levels are illustrative prototype values.
 */
@Entity
@Table(name = "company_key_issue_assessment")
public class CompanyKeyIssueAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "key_issue_id")
    private EsgKeyIssue keyIssue;

    /**
     * Score for this key issue (0.0–10.0 scale).
     * Prototype/demo data.
     */
    @Column(nullable = false)
    private Double score;

    /**
     * Risk level: Low, Moderate, or High.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    /**
     * Assessment date.
     */
    @Column(nullable = false)
    private LocalDate assessmentDate;

    protected CompanyKeyIssueAssessment() {
    }

    public CompanyKeyIssueAssessment(Organization organization, EsgKeyIssue keyIssue,
            Double score, RiskLevel riskLevel, LocalDate assessmentDate) {
        this.organization = organization;
        this.keyIssue = keyIssue;
        this.score = score;
        this.riskLevel = riskLevel;
        this.assessmentDate = assessmentDate;
    }

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public EsgKeyIssue getKeyIssue() {
        return keyIssue;
    }

    public Double getScore() {
        return score;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }
}
