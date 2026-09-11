package dev.esgenius.service.compliance;

import org.springframework.stereotype.Component;

@Component
public class EvidenceContextExpander {

    public static final int CONTEXT_BEFORE_CHARS = 350;
    public static final int CONTEXT_AFTER_CHARS = 550;
    public static final int MAX_EXPANDED_PASSAGE_CHARS = 1800;

    public String expandPassageContext(String fullChunkText, String matchedPassage) {
        if (matchedPassage == null || matchedPassage.isBlank()) {
            return matchedPassage;
        }
        if (fullChunkText == null || fullChunkText.isBlank()) {
            return matchedPassage;
        }

        int matchIndex = findMatchIndex(fullChunkText, matchedPassage);
        if (matchIndex < 0) {
            return truncate(matchedPassage, MAX_EXPANDED_PASSAGE_CHARS);
        }

        int matchEnd = matchIndex + matchedPassage.length();
        int start = Math.max(0, matchIndex - CONTEXT_BEFORE_CHARS);
        int end = Math.min(fullChunkText.length(), matchEnd + CONTEXT_AFTER_CHARS);

        String expanded = fullChunkText.substring(start, end).trim();
        if (expanded.length() <= MAX_EXPANDED_PASSAGE_CHARS) {
            return expanded;
        }

        int matchCenter = matchIndex - start + (matchedPassage.length() / 2);
        int halfWindow = MAX_EXPANDED_PASSAGE_CHARS / 2;
        int windowStart = Math.max(0, matchCenter - halfWindow);
        int windowEnd = Math.min(expanded.length(), windowStart + MAX_EXPANDED_PASSAGE_CHARS);
        windowStart = Math.max(0, windowEnd - MAX_EXPANDED_PASSAGE_CHARS);
        return expanded.substring(windowStart, windowEnd).trim();
    }

    private int findMatchIndex(String fullChunkText, String matchedPassage) {
        int direct = fullChunkText.indexOf(matchedPassage);
        if (direct >= 0) {
            return direct;
        }

        String normalizedChunk = normalizeForMatch(fullChunkText);
        String normalizedPassage = normalizeForMatch(matchedPassage);
        int normalizedIndex = normalizedChunk.indexOf(normalizedPassage);
        if (normalizedIndex < 0) {
            return -1;
        }

        int chunkOffset = 0;
        int normalizedOffset = 0;
        while (chunkOffset < fullChunkText.length() && normalizedOffset < normalizedIndex) {
            char ch = fullChunkText.charAt(chunkOffset);
            if (Character.isLetterOrDigit(ch)) {
                normalizedOffset++;
            }
            chunkOffset++;
        }
        return chunkOffset;
    }

    private String normalizeForMatch(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                builder.append(Character.toLowerCase(ch));
            }
        }
        return builder.toString();
    }

    private String truncate(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars).trim();
    }
}
