package dev.esgenius.service.esgscore;

import dev.esgenius.entity.EsgPillar;

import java.util.Arrays;
import java.util.Optional;

/**
 * The key issues of the MSCI-style score, each fed by one section of the BRSR format.
 * MSCI scores two to seven environmental and social key issues per industry plus governance; ESGenius
 * maps the BRSR sections onto ten issues so that every one of the 140 questions contributes.
 */
public enum BrsrKeyIssue {
    GOVERNANCE_SYSTEMS("A,B", "Corporate Governance and Management Systems", EsgPillar.GOVERNANCE),
    BUSINESS_ETHICS("P1", "Business Ethics and Transparency", EsgPillar.GOVERNANCE),
    PUBLIC_POLICY("P7", "Responsible Policy Advocacy", EsgPillar.GOVERNANCE),
    SUSTAINABLE_PRODUCTS("P2", "Sustainable and Safe Products and Services", EsgPillar.ENVIRONMENTAL),
    ENVIRONMENTAL_FOOTPRINT("P6", "Environmental Footprint", EsgPillar.ENVIRONMENTAL),
    HUMAN_CAPITAL("P3", "Employee Wellbeing and Human Capital", EsgPillar.SOCIAL),
    STAKEHOLDER_ENGAGEMENT("P4", "Stakeholder Engagement", EsgPillar.SOCIAL),
    HUMAN_RIGHTS("P5", "Human Rights", EsgPillar.SOCIAL),
    COMMUNITY_DEVELOPMENT("P8", "Inclusive Growth and Community Development", EsgPillar.SOCIAL),
    CUSTOMER_RESPONSIBILITY("P9", "Consumer Value and Responsibility", EsgPillar.SOCIAL);

    private final String sections;
    private final String label;
    private final EsgPillar pillar;

    BrsrKeyIssue(String sections, String label, EsgPillar pillar) {
        this.sections = sections;
        this.label = label;
        this.pillar = pillar;
    }

    public String getLabel() {
        return label;
    }

    public EsgPillar getPillar() {
        return pillar;
    }

    /** BRSR sections feeding this issue, comma separated (for example "A,B"). */
    public String getSections() {
        return sections;
    }

    public static Optional<BrsrKeyIssue> forSection(String section) {
        if (section == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(issue -> Arrays.asList(issue.sections.split(",")).contains(section))
                .findFirst();
    }
}
