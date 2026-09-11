package dev.esgenius.dto;

public record EvidenceChunkResponse(int chunkIndex, Integer pageNumber, String text, double retrievalScore) {
}
