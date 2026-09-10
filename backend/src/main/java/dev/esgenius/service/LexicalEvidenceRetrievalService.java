package dev.esgenius.service;

import dev.esgenius.entity.FrameworkRequirement;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deterministic lexical evidence retrieval using BM25-inspired scoring, IDF
 * weighting, requirement-specific anchor gates, and ESG terminology aliases.
 */
@Service
public class LexicalEvidenceRetrievalService {

    static final int MAX_RESULTS = 3;
    static final double MIN_SCORE_THRESHOLD = 0.15;
    static final int MIN_DISCRIMINATIVE_TERM_MATCHES = 2;

    private static final double TITLE_FIELD_WEIGHT = 6.0;
    private static final double DESCRIPTION_FIELD_WEIGHT = 2.5;
    private static final double CODE_FIELD_WEIGHT = 3.0;
    private static final double ANCHOR_PHRASE_BONUS = 5.0;
    private static final double ALIAS_PHRASE_BONUS = 4.0;
    private static final double REQUIRED_ANCHOR_BONUS = 8.0;
    private static final double SCOPE_MATCH_BONUS = 10.0;
    private static final double SCOPE_MISMATCH_PENALTY = 50.0;
    private static final double FORBIDDEN_PHRASE_PENALTY = 40.0;
    private static final double GENERIC_IDF_FACTOR = 0.05;
    private static final double BM25_K1 = 1.2;
    private static final double BM25_B = 0.75;

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9]+");
    private static final Pattern SCOPE_PATTERN = Pattern.compile("scope\\s*([123])", Pattern.CASE_INSENSITIVE);

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "to", "in", "for", "on", "with", "by", "from",
            "at", "as", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "will", "shall", "should", "would", "could", "may", "might",
            "must", "can", "that", "this", "these", "those", "it", "its", "their", "they",
            "them", "we", "our", "you", "your", "vs", "non");

    public List<RetrievedChunk> retrieve(FrameworkRequirement requirement, List<TextChunk> chunks) {
        if (chunks.isEmpty()) {
            return List.of();
        }

        RequirementQueryProfile query = buildQueryProfile(requirement);
        if (query.termWeights().isEmpty()) {
            return List.of();
        }

        List<List<String>> requiredAnchorGroups = RequirementRetrievalRules.requiredAnchorGroups(requirement);
        List<String> forbiddenPhrases = RequirementRetrievalRules.forbiddenPhrases(requirement);

        Map<String, Double> idf = computeIdf(chunks, query.termWeights().keySet());
        double averageChunkLength = chunks.stream()
                .mapToInt(chunk -> tokenize(chunk.text()).size())
                .average()
                .orElse(1.0);

        List<RetrievedChunk> scored = new ArrayList<>();
        for (TextChunk chunk : chunks) {
            ChunkEvaluation evaluation = evaluateBestPassage(
                    chunk, requirement, query, idf, averageChunkLength, requiredAnchorGroups, forbiddenPhrases);
            if (evaluation.passesAnchorGate() && evaluation.score() >= MIN_SCORE_THRESHOLD) {
                scored.add(new RetrievedChunk(chunk.chunkIndex(), evaluation.passageText(), evaluation.score()));
            }
        }

        scored.sort(Comparator.comparingDouble(RetrievedChunk::score).reversed());
        return scored.subList(0, Math.min(MAX_RESULTS, scored.size()));
    }

    RequirementQueryProfile buildQueryProfile(FrameworkRequirement requirement) {
        Map<String, Double> termWeights = new HashMap<>();
        Set<String> discriminativeTitleTerms = new LinkedHashSet<>();

        addTerms(termWeights, requirement.getRequirementCode(), CODE_FIELD_WEIGHT, discriminativeTitleTerms);
        addTerms(termWeights, requirement.getTitle(), TITLE_FIELD_WEIGHT, discriminativeTitleTerms);
        addTerms(termWeights, requirement.getDescription(), DESCRIPTION_FIELD_WEIGHT, discriminativeTitleTerms);

        expandAliasTerms(termWeights, requirement.getTitle(), requirement.getDescription());

        List<String> anchorPhrases = buildAnchorPhrases(requirement);
        List<String> aliasPhrases = EsgRetrievalLexicon.aliasPhrasesForRequirement(
                requirement.getTitle(), requirement.getDescription());

        Optional<Integer> scopeNumber = extractScopeNumber(requirement.getTitle());

        return new RequirementQueryProfile(
                requirement.getRequirementCode(),
                termWeights,
                discriminativeTitleTerms,
                anchorPhrases,
                aliasPhrases,
                scopeNumber);
    }

    private void addTerms(
            Map<String, Double> terms,
            String text,
            double fieldWeight,
            Set<String> discriminativeCollector) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String token : tokenize(text)) {
            if (STOP_WORDS.contains(token) || token.length() < 2) {
                continue;
            }
            if (EsgRetrievalLexicon.isGenericTerm(token)) {
                continue;
            }
            terms.merge(token, fieldWeight, Math::max);
            if (fieldWeight >= DESCRIPTION_FIELD_WEIGHT) {
                discriminativeCollector.add(token);
            }
        }
    }

    private void expandAliasTerms(Map<String, Double> terms, String title, String description) {
        Set<String> seeds = new LinkedHashSet<>(terms.keySet());
        for (String seed : seeds) {
            double baseWeight = terms.get(seed);
            for (String alias : EsgRetrievalLexicon.expandTermAliases(seed)) {
                if (alias.contains(" ") || STOP_WORDS.contains(alias) || alias.length() < 2) {
                    continue;
                }
                terms.merge(alias, baseWeight * 0.85, Math::max);
            }
        }
        for (String phrase : EsgRetrievalLexicon.aliasPhrasesForRequirement(title, description)) {
            for (String token : tokenize(phrase)) {
                if (!STOP_WORDS.contains(token) && token.length() >= 2 && !EsgRetrievalLexicon.isGenericTerm(token)) {
                    terms.merge(token, TITLE_FIELD_WEIGHT * 0.7, Math::max);
                }
            }
        }
    }

    private List<String> buildAnchorPhrases(FrameworkRequirement requirement) {
        List<String> phrases = new ArrayList<>(RequirementRetrievalRules.requiredAnchorGroups(requirement)
                .stream()
                .flatMap(Collection::stream)
                .toList());
        addNormalizedPhrase(phrases, requirement.getTitle());
        addNormalizedPhrase(phrases, requirement.getDescription());
        extractScopeNumber(requirement.getTitle())
                .map(scope -> "scope " + scope)
                .ifPresent(scopePhrase -> {
                    phrases.add(scopePhrase);
                    phrases.add(scopePhrase + " emissions");
                });
        return phrases.stream().filter(p -> p.contains(" ")).distinct().toList();
    }

    private void addNormalizedPhrase(List<String> phrases, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String normalized = normalize(text);
        if (normalized.contains(" ")) {
            phrases.add(normalized);
        }
        String[] words = normalized.split(" ");
        for (int i = 0; i < words.length - 1; i++) {
            phrases.add(words[i] + " " + words[i + 1]);
        }
    }

    private Map<String, Double> computeIdf(List<TextChunk> chunks, Set<String> queryTerms) {
        int documentCount = chunks.size();
        Map<String, Integer> documentFrequency = new HashMap<>();

        for (TextChunk chunk : chunks) {
            Set<String> uniqueTerms = new HashSet<>(tokenize(chunk.text()));
            for (String term : queryTerms) {
                if (uniqueTerms.contains(term)) {
                    documentFrequency.merge(term, 1, Integer::sum);
                }
            }
        }

        Map<String, Double> idf = new HashMap<>();
        for (String term : queryTerms) {
            int df = documentFrequency.getOrDefault(term, 0);
            double rawIdf = Math.log((documentCount - df + 0.5) / (df + 0.5) + 1.0);
            if (EsgRetrievalLexicon.isGenericTerm(term)) {
                rawIdf *= GENERIC_IDF_FACTOR;
            }
            idf.put(term, Math.max(rawIdf, 0.05));
        }
        return idf;
    }

    private ChunkEvaluation evaluateBestPassage(
            TextChunk chunk,
            FrameworkRequirement requirement,
            RequirementQueryProfile query,
            Map<String, Double> idf,
            double averageChunkLength,
            List<List<String>> requiredAnchorGroups,
            List<String> forbiddenPhrases) {
        ChunkEvaluation best = new ChunkEvaluation(0.0, false, "");
        for (String passage : splitPassages(chunk.text())) {
            ChunkEvaluation evaluation = evaluatePassage(
                    passage, query, idf, averageChunkLength, requiredAnchorGroups, forbiddenPhrases);
            if (evaluation.passesAnchorGate() && evaluation.score() > best.score()) {
                best = evaluation;
            }
        }
        return best;
    }

    private ChunkEvaluation evaluatePassage(
            String passageText,
            RequirementQueryProfile query,
            Map<String, Double> idf,
            double averageChunkLength,
            List<List<String>> requiredAnchorGroups,
            List<String> forbiddenPhrases) {
        String normalizedPassage = normalize(passageText);
        List<String> passageTokens = tokenize(passageText);
        if (passageTokens.isEmpty()) {
            return new ChunkEvaluation(0.0, false, passageText);
        }

        if (RequirementRetrievalRules.failsScopeGate(normalizedPassage, query.scopeNumber())) {
            return new ChunkEvaluation(0.0, false, passageText);
        }

        boolean matchedRequiredAnchor = matchesRequiredAnchor(normalizedPassage, requiredAnchorGroups);
        boolean containsForbidden = forbiddenPhrases.stream().anyMatch(normalizedPassage::contains);
        if (containsForbidden && !matchedRequiredAnchor) {
            return new ChunkEvaluation(0.0, false, passageText);
        }

        Map<String, Integer> termFrequency = new HashMap<>();
        for (String token : passageTokens) {
            termFrequency.merge(token, 1, Integer::sum);
        }

        int docLength = passageTokens.size();
        double score = 0.0;
        Set<String> matchedDiscriminative = new HashSet<>();

        for (Map.Entry<String, Double> entry : query.termWeights().entrySet()) {
            String term = entry.getKey();
            int tf = termFrequency.getOrDefault(term, 0);
            if (tf == 0) {
                continue;
            }
            double termIdf = idf.getOrDefault(term, 0.05);
            score += bm25TermScore(tf, docLength, averageChunkLength, termIdf, entry.getValue());
            if (query.discriminativeTitleTerms().contains(term)) {
                matchedDiscriminative.add(term);
            }
        }

        for (String phrase : query.anchorPhrases()) {
            if (normalizedPassage.contains(phrase)) {
                score += ANCHOR_PHRASE_BONUS;
            }
        }

        for (String phrase : query.aliasPhrases()) {
            if (normalizedPassage.contains(phrase)) {
                score += ALIAS_PHRASE_BONUS;
            }
        }

        if (matchedRequiredAnchor) {
            score += REQUIRED_ANCHOR_BONUS;
        }

        if (query.scopeNumber().isPresent()
                && extractScopesInText(normalizedPassage).contains(query.scopeNumber().get())) {
            score += SCOPE_MATCH_BONUS;
        }

        for (String forbidden : forbiddenPhrases) {
            if (normalizedPassage.contains(forbidden)) {
                score -= FORBIDDEN_PHRASE_PENALTY;
            }
        }

        boolean passesAnchor = passesAnchorGate(
                normalizedPassage, query, matchedDiscriminative, requiredAnchorGroups, matchedRequiredAnchor);

        return new ChunkEvaluation(Math.max(score, 0.0), passesAnchor, passageText.trim());
    }

    private List<String> splitPassages(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] rawSegments = text.split("(?<=[.!?])\\s+|\\n+");
        List<String> passages = new ArrayList<>();
        for (String segment : rawSegments) {
            String trimmed = segment.trim();
            if (trimmed.length() >= 30) {
                passages.add(trimmed);
            }
        }
        if (passages.isEmpty()) {
            passages.add(text.trim());
        }
        return passages;
    }

    private boolean matchesRequiredAnchor(String normalizedChunk, List<List<String>> requiredAnchorGroups) {
        if (requiredAnchorGroups.isEmpty()) {
            return false;
        }
        return requiredAnchorGroups.stream()
                .anyMatch(group -> group.stream().anyMatch(normalizedChunk::contains));
    }

    private double bm25TermScore(
            int termFrequency,
            int docLength,
            double averageDocLength,
            double idf,
            double termWeight) {
        double numerator = termFrequency * (BM25_K1 + 1.0);
        double denominator = termFrequency + BM25_K1 * (1.0 - BM25_B + BM25_B * docLength / averageDocLength);
        return termWeight * idf * (numerator / denominator);
    }

    boolean passesAnchorGate(
            String normalizedChunk,
            RequirementQueryProfile query,
            Set<String> matchedDiscriminativeTerms,
            List<List<String>> requiredAnchorGroups,
            boolean matchedRequiredAnchor) {
        if (!RequirementRetrievalRules.passesScopeGate(normalizedChunk, query.scopeNumber())) {
            return false;
        }

        if (!requiredAnchorGroups.isEmpty()) {
            return matchedRequiredAnchor;
        }

        if (query.scopeNumber().isPresent()) {
            return extractScopesInText(normalizedChunk).contains(query.scopeNumber().get());
        }

        for (String phrase : query.anchorPhrases()) {
            if (normalizedChunk.contains(phrase)) {
                return true;
            }
        }

        for (String phrase : query.aliasPhrases()) {
            if (normalizedChunk.contains(phrase)) {
                return true;
            }
        }

        long discriminativeMatches = matchedDiscriminativeTerms.stream()
                .filter(term -> !EsgRetrievalLexicon.isGenericTerm(term))
                .count();
        return discriminativeMatches >= MIN_DISCRIMINATIVE_TERM_MATCHES;
    }

    private Set<Integer> extractScopesInText(String normalizedText) {
        Set<Integer> scopes = new HashSet<>();
        Matcher matcher = SCOPE_PATTERN.matcher(normalizedText);
        while (matcher.find()) {
            scopes.add(Integer.parseInt(matcher.group(1)));
        }
        return scopes;
    }

    private Optional<Integer> extractScopeNumber(String title) {
        if (title == null) {
            return Optional.empty();
        }
        Matcher matcher = SCOPE_PATTERN.matcher(normalize(title));
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }

    private List<String> tokenize(String text) {
        return TOKEN_PATTERN.matcher(normalize(text))
                .results()
                .map(match -> match.group())
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toList());
    }

    private String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record ChunkEvaluation(double score, boolean passesAnchorGate, String passageText) {
    }
}
