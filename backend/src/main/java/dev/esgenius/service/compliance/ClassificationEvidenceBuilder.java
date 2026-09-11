package dev.esgenius.service.compliance;

import dev.esgenius.service.RetrievedChunk;
import dev.esgenius.service.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClassificationEvidenceBuilder {

    private final EvidenceContextExpander contextExpander;

    public ClassificationEvidenceBuilder(EvidenceContextExpander contextExpander) {
        this.contextExpander = contextExpander;
    }

    public List<String> buildExpandedPassages(List<RetrievedChunk> retrieved, List<TextChunk> sourceChunks) {
        Map<Integer, String> chunkTextByIndex = new HashMap<>();
        for (TextChunk chunk : sourceChunks) {
            chunkTextByIndex.put(chunk.chunkIndex(), chunk.text());
        }

        List<String> passages = new ArrayList<>();
        for (RetrievedChunk match : retrieved) {
            String fullChunkText = chunkTextByIndex.get(match.chunkIndex());
            String expanded = contextExpander.expandPassageContext(fullChunkText, match.text());
            if (expanded != null && !expanded.isBlank()) {
                passages.add(expanded);
            }
        }
        return passages;
    }
}
