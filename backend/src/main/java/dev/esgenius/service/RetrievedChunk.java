package dev.esgenius.service;

public record RetrievedChunk(int chunkIndex, String text, double score, Integer pageNumber) {

    public RetrievedChunk(int chunkIndex, String text, double score) {
        this(chunkIndex, text, score, null);
    }
}
