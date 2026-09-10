package dev.esgenius.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

record RequirementQueryProfile(
        String requirementCode,
        Map<String, Double> termWeights,
        Set<String> discriminativeTitleTerms,
        List<String> anchorPhrases,
        List<String> aliasPhrases,
        Optional<Integer> scopeNumber) {
}
