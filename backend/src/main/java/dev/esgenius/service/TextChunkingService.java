package dev.esgenius.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkingService {

    static final int TARGET_MIN_CHARS = 1200;
    static final int TARGET_MAX_CHARS = 1800;
    static final int OVERLAP_CHARS = 200;

    public List<TextChunk> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.replace("\r\n", "\n").trim();
        if (normalized.length() <= TARGET_MAX_CHARS) {
            return List.of(new TextChunk(0, normalized));
        }

        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        int chunkIndex = 0;

        while (start < normalized.length()) {
            int proposedEnd = Math.min(start + TARGET_MAX_CHARS, normalized.length());
            int end = proposedEnd;

            if (end < normalized.length()) {
                end = findBreakPoint(normalized, start, proposedEnd);
            }

            String chunkText = normalized.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                chunks.add(new TextChunk(chunkIndex++, chunkText));
            }

            if (end >= normalized.length()) {
                break;
            }

            int nextStart = Math.max(end - OVERLAP_CHARS, start + 1);
            nextStart = alignToBoundary(normalized, nextStart, end);
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }

        return chunks.isEmpty() ? List.of(new TextChunk(0, normalized)) : chunks;
    }

    private int findBreakPoint(String text, int start, int proposedEnd) {
        int minEnd = Math.min(text.length(), start + TARGET_MIN_CHARS);
        int searchEnd = Math.min(text.length(), proposedEnd);

        for (int i = searchEnd; i >= minEnd; i--) {
            char ch = text.charAt(i - 1);
            if (ch == '\n' || ch == '.' || ch == ';') {
                return i;
            }
        }

        for (int i = searchEnd; i >= minEnd; i--) {
            if (text.charAt(i - 1) == ' ') {
                return i;
            }
        }

        return searchEnd;
    }

    private int alignToBoundary(String text, int position, int upperBound) {
        int aligned = position;
        while (aligned < upperBound && aligned < text.length()
                && text.charAt(aligned) != '\n' && text.charAt(aligned) != ' ') {
            aligned++;
        }
        while (aligned < upperBound && aligned < text.length() && text.charAt(aligned) == ' ') {
            aligned++;
        }
        return aligned;
    }
}
