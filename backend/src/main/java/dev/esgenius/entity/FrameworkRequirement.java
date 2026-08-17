package dev.esgenius.entity;

import jakarta.persistence.*;

/**
 * Immutable reference data representing a single disclosure requirement
 * within an ESG framework. Does NOT contain compliance status, evidence,
 * AI analysis, gaps or recommendations — those belong to AssessmentResult.
 */
@Entity
@Table(name = "framework_requirement",
       uniqueConstraints = @UniqueConstraint(columnNames = {"framework_id", "requirement_code"}))
public class FrameworkRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "framework_id", nullable = false)
    private Framework framework;

    @Column(name = "requirement_code", length = 20, nullable = false)
    private String requirementCode;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EsgCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "framework_text", columnDefinition = "TEXT")
    private String frameworkText;

    @Column(nullable = false)
    private boolean mandatory = true;

    @Column(length = 50)
    private String version;

    protected FrameworkRequirement() {}

    public FrameworkRequirement(Framework framework, String requirementCode, String title,
                                 EsgCategory category, String description, String frameworkText,
                                 boolean mandatory, String version) {
        this.framework = framework;
        this.requirementCode = requirementCode;
        this.title = title;
        this.category = category;
        this.description = description;
        this.frameworkText = frameworkText;
        this.mandatory = mandatory;
        this.version = version;
    }

    public Long getId() { return id; }

    public Framework getFramework() { return framework; }

    public String getRequirementCode() { return requirementCode; }

    public String getTitle() { return title; }

    public EsgCategory getCategory() { return category; }

    public String getDescription() { return description; }

    public String getFrameworkText() { return frameworkText; }

    public boolean isMandatory() { return mandatory; }

    public String getVersion() { return version; }
}
