package dev.esgenius.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "requirement_assessment",
       uniqueConstraints = @UniqueConstraint(columnNames = {"analysis_id", "framework_requirement_id"}))
public class RequirementAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private ComplianceAnalysis analysis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "framework_requirement_id", nullable = false)
    private FrameworkRequirement frameworkRequirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_status", nullable = false, length = 30)
    private AssessmentStatus assessmentStatus;

    @Column
    private Double confidence;

    @Column(name = "evidence_text", columnDefinition = "TEXT")
    private String evidenceText;

    @Column(name = "evidence_chunks", columnDefinition = "TEXT")
    private String evidenceChunks;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String gap;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "retrieval_score")
    private Double retrievalScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RequirementAssessment() {
    }

    public RequirementAssessment(ComplianceAnalysis analysis, FrameworkRequirement frameworkRequirement) {
        this.analysis = analysis;
        this.frameworkRequirement = frameworkRequirement;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ComplianceAnalysis getAnalysis() {
        return analysis;
    }

    public FrameworkRequirement getFrameworkRequirement() {
        return frameworkRequirement;
    }

    public AssessmentStatus getAssessmentStatus() {
        return assessmentStatus;
    }

    public void setAssessmentStatus(AssessmentStatus assessmentStatus) {
        this.assessmentStatus = assessmentStatus;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getEvidenceText() {
        return evidenceText;
    }

    public void setEvidenceText(String evidenceText) {
        this.evidenceText = evidenceText;
    }

    public String getEvidenceChunks() {
        return evidenceChunks;
    }

    public void setEvidenceChunks(String evidenceChunks) {
        this.evidenceChunks = evidenceChunks;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getGap() {
        return gap;
    }

    public void setGap(String gap) {
        this.gap = gap;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public Double getRetrievalScore() {
        return retrievalScore;
    }

    public void setRetrievalScore(Double retrievalScore) {
        this.retrievalScore = retrievalScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
