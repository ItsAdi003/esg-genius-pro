package dev.esgenius.entity;

import jakarta.persistence.*;

/**
 * ESG Key Issue: A material ESG topic relevant to company performance
 * assessment.
 * Examples: Carbon Emissions, Human Capital, Data Privacy & Security, Corporate
 * Governance, Business Ethics.
 */
@Entity
@Table(name = "esg_key_issue")
public class EsgKeyIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Short code for the issue (e.g., "CARBON_EMISSIONS").
     */
    @Column(nullable = false, length = 100)
    private String code;

    /**
     * Display name (e.g., "Carbon Emissions").
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * ESG pillar: Environmental, Social, or Governance.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EsgPillar pillar;

    /**
     * Detailed description of the issue.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    protected EsgKeyIssue() {
    }

    public EsgKeyIssue(String code, String name, EsgPillar pillar, String description) {
        this.code = code;
        this.name = name;
        this.pillar = pillar;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public EsgPillar getPillar() {
        return pillar;
    }

    public String getDescription() {
        return description;
    }
}
