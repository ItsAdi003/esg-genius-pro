package dev.esgenius.service.compliance;

import dev.esgenius.service.RetrievedChunk;
import dev.esgenius.service.TextChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificationEvidenceBuilderTest {

    private final ClassificationEvidenceBuilder builder = new ClassificationEvidenceBuilder(new EvidenceContextExpander());

    @Test
    void buildsExpandedPassagesFromSourceChunks() {
        String chunkText = String.join("\n",
                "Scope 1 boundary text for owned facilities.",
                "Total Scope 1 emissions were 8,200 metric tonnes CO2e.");

        List<TextChunk> chunks = List.of(new TextChunk(0, chunkText));
        List<RetrievedChunk> retrieved = List.of(
                new RetrievedChunk(0, "Scope 1 boundary text for owned facilities.", 42.0));

        List<String> passages = builder.buildExpandedPassages(retrieved, chunks);

        assertThat(passages).hasSize(1);
        assertThat(passages.get(0)).contains("8,200 metric tonnes CO2e");
    }
}
