package dev.esgenius.service;

import dev.esgenius.entity.FrameworkRequirement;

import java.util.*;

/**
 * Requirement-specific anchor and penalty rules derived from seeded BRSR metadata.
 */
final class RequirementRetrievalRules {

    private RequirementRetrievalRules() {
    }

    static List<List<String>> requiredAnchorGroups(FrameworkRequirement requirement) {
        String code = requirement.getRequirementCode();
        String title = normalize(requirement.getTitle());

        if ("ENV-001".equals(code)) {
            return List.of(List.of("energy consumption", "energy consumed", "total energy", "electricity consumption", "energy intensity"));
        }
        if ("ENV-002".equals(code)) {
            return List.of(List.of("renewable energy", "renewable electricity", "solar", "wind power", "non renewable", "non-renewable"));
        }
        if ("ENV-003".equals(code)) {
            return List.of(List.of("scope 1", "scope 1 emissions", "direct emissions"));
        }
        if ("ENV-004".equals(code)) {
            return List.of(List.of("scope 2", "scope 2 emissions", "purchased electricity"));
        }
        if ("ENV-005".equals(code)) {
            return List.of(List.of("scope 3", "scope 3 emissions", "value chain emissions"));
        }
        if ("ENV-006".equals(code)) {
            return List.of(List.of("water withdrawal", "water withdrawn", "groundwater", "surface water"));
        }
        if ("ENV-007".equals(code)) {
            return List.of(List.of("waste generated", "waste recycling", "recycling reuse", "waste recovered"));
        }
        if ("SOC-001".equals(code)) {
            return List.of(List.of("occupational health", "health and safety", "workplace safety", "safety incident", "recordable injury"));
        }
        if ("SOC-002".equals(code)) {
            return List.of(List.of("training hours", "learning and development", "employee training", "training and development", "average training"));
        }
        if ("SOC-003".equals(code)) {
            return List.of(List.of("human rights due diligence", "human rights assessment", "human rights"));
        }
        if ("SOC-004".equals(code)) {
            return List.of(List.of(
                    "csr details",
                    "amount spent",
                    "spent on csr",
                    "csr expenditure",
                    "csr spend",
                    "community development expenditure",
                    "section 135"));
        }
        if ("GOV-001".equals(code)) {
            return List.of(List.of("anti corruption", "anti-corruption", "anti bribery", "anti-bribery", "bribery and corruption"));
        }
        if ("GOV-002".equals(code)) {
            return List.of(List.of("independent director", "independent directors", "board composition", "board independence"));
        }
        if ("GOV-003".equals(code)) {
            return List.of(List.of("whistleblower mechanism", "vigil mechanism", "whistle blower"));
        }

        if (title.contains("scope")) {
            return List.of(List.of("scope", "emissions"));
        }
        return List.of();
    }

    static List<String> forbiddenPhrases(FrameworkRequirement requirement) {
        String code = requirement.getRequirementCode();

        if ("ENV-005".equals(code)) {
            return List.of(); // handled by scope gate
        }
        if ("SOC-001".equals(code)) {
            return List.of("investor engagement", "stakeholder engagement", "frequency of engagement");
        }
        if ("SOC-002".equals(code)) {
            return List.of(
                    "brsr principles",
                    "principles of brsr",
                    "business responsibility report principles",
                    "frequency of engagement",
                    "newspaper pamphlets");
        }
        if ("SOC-004".equals(code)) {
            return List.of(
                    "uttar pradesh",
                    "chandauli",
                    "fatehpur",
                    "percentage of r&d and capital expenditure",
                    "capex investments");
        }
        if ("GOV-001".equals(code)) {
            return List.of("settlement application", "securities and exchange board", "sebi settlement");
        }
        if ("GOV-002".equals(code)) {
            return List.of("audit committee refer", "nomination committee");
        }
        if ("GOV-003".equals(code)) {
            return List.of("frequency of engagement", "newspaper pamphlets");
        }
        if ("ENV-001".equals(code) || "ENV-002".equals(code)) {
            return List.of("differently abled", "disabilities act", "rights of persons with disabilities");
        }
        if ("ENV-003".equals(code) || "ENV-004".equals(code) || "ENV-005".equals(code)) {
            return List.of(
                    "particulate matter",
                    "volatile organic compounds",
                    "hazardous air pollutants",
                    "independent assessment evaluation assurance");
        }
        return List.of();
    }

    static boolean passesScopeGate(String normalizedChunk, Optional<Integer> requiredScope) {
        if (requiredScope.isEmpty()) {
            return true;
        }
        int scope = requiredScope.get();
        Set<Integer> scopes = extractScopes(normalizedChunk);
        if (scopes.contains(scope)) {
            return true;
        }
        return scopes.isEmpty();
    }

    static boolean failsScopeGate(String normalizedChunk, Optional<Integer> requiredScope) {
        if (requiredScope.isEmpty()) {
            return false;
        }
        int scope = requiredScope.get();
        Set<Integer> scopes = extractScopes(normalizedChunk);
        return !scopes.isEmpty() && !scopes.contains(scope);
    }

    private static Set<Integer> extractScopes(String normalizedChunk) {
        Set<Integer> scopes = new HashSet<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("scope\\s*([123])")
                .matcher(normalizedChunk);
        while (matcher.find()) {
            scopes.add(Integer.parseInt(matcher.group(1)));
        }
        return scopes;
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
