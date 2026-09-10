package dev.esgenius.service;

import java.util.*;

/**
 * Domain-generic ESG terminology aliases and generic BRSR vocabulary used to
 * down-weight non-discriminative matches during lexical evidence retrieval.
 */
final class EsgRetrievalLexicon {

    static final Set<String> GENERIC_BRSR_TERMS = Set.of(
            "entity", "business", "report", "assessment", "information", "fiscal",
            "financial", "year", "years", "details", "provide", "principle", "principles",
            "indicator", "indicators", "disclose", "disclosure", "reporting", "period",
            "total", "shall", "during", "including", "together", "whether", "exists",
            "coverage", "programme", "program", "awareness", "management", "system",
            "process", "actions", "taken", "carried", "material", "categories",
            "considered", "basis", "estimation", "methodology", "method", "used",
            "sources", "operations", "related", "describe", "existence", "stakeholder",
            "stakeholders", "company", "organization", "corporate", "sustainability",
            "responsibility", "brsr", "sebi", "regulation", "regulatory", "compliance",
            "section", "annex", "annexure", "table", "note", "notes", "part",
            "annual", "statement", "policy", "policies", "framework", "standard",
            "standards", "requirement", "requirements", "performance", "data",
            "metric", "metrics", "value", "chain", "upstream", "downstream",
            "employee", "employees", "workforce", "gender", "category");

    static final Map<String, List<String>> TERM_ALIASES = Map.ofEntries(
            Map.entry("ghg", List.of("greenhouse", "gas", "co2", "co2e", "carbon")),
            Map.entry("greenhouse", List.of("ghg", "co2", "co2e")),
            Map.entry("renewable", List.of("solar", "wind", "renewable electricity")),
            Map.entry("energy", List.of("electricity", "fuel", "joules", "kwh", "mwh")),
            Map.entry("consumption", List.of("consumed", "consumption")),
            Map.entry("corruption", List.of("bribery", "anti bribery", "anti corruption")),
            Map.entry("bribery", List.of("corruption", "anti bribery", "anti corruption")),
            Map.entry("whistleblower", List.of("vigil", "whistle blower", "whistleblowing")),
            Map.entry("vigil", List.of("whistleblower", "whistle blower")),
            Map.entry("training", List.of("learning", "upskilling", "development", "hours")),
            Map.entry("development", List.of("learning", "upskilling", "training")),
            Map.entry("health", List.of("occupational", "workplace", "safety")),
            Map.entry("safety", List.of("occupational", "workplace", "incident", "injury")),
            Map.entry("independence", List.of("independent", "independent director", "independent directors")),
            Map.entry("board", List.of("director", "directors", "independent")),
            Map.entry("community", List.of("csr", "beneficiary", "beneficiaries")),
            Map.entry("spend", List.of("expenditure", "expense", "investment", "spent", "inr", "crore")),
            Map.entry("csr", List.of("community development", "expenditure", "spend")),
            Map.entry("water", List.of("groundwater", "surface water", "kilolitre", "kilolitres")),
            Map.entry("waste", List.of("recycling", "recycled", "recovery", "reuse")),
            Map.entry("rights", List.of("human rights", "due diligence", "remediation")),
            Map.entry("human", List.of("human rights", "due diligence")),
            Map.entry("emissions", List.of("emission", "co2", "co2e", "tonnes", "metric tonnes")),
            Map.entry("scope", List.of("scope 1", "scope 2", "scope 3")));

    static final List<List<String>> PHRASE_ALIAS_GROUPS = List.of(
            List.of("greenhouse gas", "ghg emissions", "ghg emission"),
            List.of("renewable energy", "solar power", "wind power", "renewable electricity"),
            List.of("energy consumption", "energy consumed", "electricity consumption", "total energy"),
            List.of("anti corruption", "anti bribery", "anti-corruption", "anti-bribery", "bribery and corruption"),
            List.of("whistleblower mechanism", "vigil mechanism", "whistle blower"),
            List.of("training and development", "learning and development", "training hours", "employee training"),
            List.of("health and safety", "occupational health", "workplace safety", "occupational health and safety"),
            List.of("board independence", "independent director", "independent directors", "board composition"),
            List.of("community development spend", "csr expenditure", "csr spend", "community development expenditure"),
            List.of("scope 1 emissions", "scope 1 ghg", "direct emissions"),
            List.of("scope 2 emissions", "scope 2 ghg", "purchased electricity"),
            List.of("scope 3 emissions", "scope 3 ghg", "value chain emissions"),
            List.of("water withdrawal", "water withdrawn", "groundwater withdrawal"),
            List.of("waste recycling", "waste recovered", "recycling reuse"),
            List.of("human rights due diligence", "human rights assessment"));

    private EsgRetrievalLexicon() {
    }

    static boolean isGenericTerm(String term) {
        return GENERIC_BRSR_TERMS.contains(term);
    }

    static Set<String> expandTermAliases(String term) {
        Set<String> expanded = new LinkedHashSet<>();
        expanded.add(term);
        List<String> aliases = TERM_ALIASES.get(term);
        if (aliases != null) {
            expanded.addAll(aliases);
        }
        return expanded;
    }

    static List<String> aliasPhrasesForRequirement(String title, String description) {
        String combined = normalizeForMatch(title) + " " + normalizeForMatch(description);
        List<String> phrases = new ArrayList<>();

        if (containsAny(combined, "energy consumption", "total energy")) {
            addPhrases(phrases, "energy consumption", "energy consumed", "electricity consumption", "total energy");
        }
        if (containsAny(combined, "renewable", "non-renewable", "non renewable")) {
            addPhrases(phrases, "renewable energy", "solar power", "wind power", "renewable electricity", "non renewable");
        }
        if (containsAny(combined, "scope 1", "scope 2", "scope 3", "ghg", "emissions")) {
            addPhrases(phrases, "greenhouse gas", "ghg emissions", "scope 1 emissions", "scope 2 emissions",
                    "scope 3 emissions", "direct emissions", "purchased electricity", "value chain emissions");
        }
        if (containsAny(combined, "water withdrawal", "water")) {
            addPhrases(phrases, "water withdrawal", "water withdrawn", "groundwater withdrawal");
        }
        if (containsAny(combined, "waste", "recycling")) {
            addPhrases(phrases, "waste recycling", "waste generated", "waste recovered", "recycling reuse");
        }
        if (containsAny(combined, "health", "safety", "occupational")) {
            addPhrases(phrases, "health and safety", "occupational health", "workplace safety",
                    "occupational health and safety");
        }
        if (containsAny(combined, "training", "development hours", "development")) {
            addPhrases(phrases, "training hours", "employee training", "learning and development",
                    "training and development");
        }
        if (containsAny(combined, "human rights", "due diligence")) {
            addPhrases(phrases, "human rights due diligence", "human rights assessment");
        }
        if (containsAny(combined, "community", "csr", "development spend")) {
            addPhrases(phrases, "community development spend", "csr expenditure", "csr spend",
                    "community development expenditure");
        }
        if (containsAny(combined, "anti-corruption", "anti corruption", "bribery")) {
            addPhrases(phrases, "anti corruption", "anti bribery", "anti-corruption", "anti-bribery",
                    "bribery and corruption");
        }
        if (containsAny(combined, "board composition", "independence", "independent")) {
            addPhrases(phrases, "board independence", "independent director", "independent directors",
                    "board composition");
        }
        if (containsAny(combined, "whistleblower", "vigil")) {
            addPhrases(phrases, "whistleblower mechanism", "vigil mechanism", "whistle blower");
        }

        return phrases.stream().distinct().toList();
    }

    private static void addPhrases(List<String> target, String... phrases) {
        for (String phrase : phrases) {
            target.add(normalizeForMatch(phrase));
        }
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(normalizeForMatch(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeForMatch(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
