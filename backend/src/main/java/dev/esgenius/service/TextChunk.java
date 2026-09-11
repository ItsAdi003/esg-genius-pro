package dev.esgenius.service;

public record TextChunk(int chunkIndex, String text, Integer pageNumber) {

    public TextChunk(int chunkIndex, String text) {
        this(chunkIndex, text, null);
    }
}
