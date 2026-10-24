package dev.esgenius.entity;

/**
 * Sector a reviewer assigns to an analysis. It selects the Environmental / Social / Governance pillar
 * weights used for the MSCI-style score and defines the peer group for the industry-adjusted score.
 *
 * MSCI sets weights per GICS sub-industry from proprietary data. These sector-level pillar weights are
 * ESGenius prototype values that follow MSCI's published rules (weights reflect each industry's
 * contribution to environmental and social externalities; Governance gets at least 33%).
 */
public enum EsgIndustry {
    ENERGY("Energy", 0.45, 0.22, 0.33),
    MATERIALS("Materials", 0.45, 0.22, 0.33),
    UTILITIES("Utilities", 0.45, 0.22, 0.33),
    INDUSTRIALS("Industrials", 0.35, 0.32, 0.33),
    CONSUMER_DISCRETIONARY("Consumer Discretionary", 0.30, 0.37, 0.33),
    CONSUMER_STAPLES("Consumer Staples", 0.35, 0.32, 0.33),
    HEALTH_CARE("Health Care", 0.15, 0.52, 0.33),
    FINANCIALS("Financials", 0.12, 0.40, 0.48),
    INFORMATION_TECHNOLOGY("Information Technology", 0.15, 0.52, 0.33),
    COMMUNICATION_SERVICES("Communication Services", 0.10, 0.55, 0.35),
    REAL_ESTATE("Real Estate", 0.40, 0.27, 0.33);

    private final String label;
    private final double environmentalWeight;
    private final double socialWeight;
    private final double governanceWeight;

    EsgIndustry(String label, double environmentalWeight, double socialWeight, double governanceWeight) {
        this.label = label;
        this.environmentalWeight = environmentalWeight;
        this.socialWeight = socialWeight;
        this.governanceWeight = governanceWeight;
    }

    public String getLabel() {
        return label;
    }

    public double getEnvironmentalWeight() {
        return environmentalWeight;
    }

    public double getSocialWeight() {
        return socialWeight;
    }

    public double getGovernanceWeight() {
        return governanceWeight;
    }
}
