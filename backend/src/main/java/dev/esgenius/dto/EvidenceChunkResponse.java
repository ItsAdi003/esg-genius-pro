package dev.esgenius.dto;

public record EvidenceChunkResponse(int chunkIndex, String text, double retrievalScore) {
}
