package dev.esgenius.service;

import dev.esgenius.entity.Framework;
import dev.esgenius.entity.FrameworkRequirement;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.FrameworkRequirementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline evaluation of retrieval for the complete BRSR requirement set against a real report. No model
 * is called. Runs only when {@code BRSR_EVAL_PDF} points at a PDF, and writes a per-requirement report to
 * {@code target/brsr-retrieval-eval.txt}: which requirements find evidence, on which page, and how strong.
 *
 * <pre>$env:BRSR_EVAL_PDF = "D:\path\to\report.pdf"; .\mvnw.cmd -o test -Dtest=BrsrFullRetrievalEvaluationTest</pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "BRSR_EVAL_PDF", matches = ".+")
class BrsrFullRetrievalEvaluationTest {

    @Autowired
    private FrameworkRepository frameworkRepository;

    @Autowired
    private FrameworkRequirementRepository requirementRepository;

    @Autowired
    private PdfTextExtractionService extractionService;

    @Autowired
    private TextChunkingService chunkingService;

    @Autowired
    private LexicalEvidenceRetrievalService retrievalService;

    @Test
    void reportRetrievalForEveryActiveRequirement() throws Exception {
        ExtractedPdf pdf = extractionService.extract(Path.of(System.getenv("BRSR_EVAL_PDF")));
        List<TextChunk> chunks = chunkingService.chunkPages(pdf.pages());
        assertThat(chunks).isNotEmpty();

        Framework brsr = frameworkRepository.findByCode("BRSR").orElseThrow();
        List<FrameworkRequirement> requirements = requirementRepository.findActiveByFramework(brsr);

        List<String> lines = new ArrayList<>();
        lines.add("pages=" + pdf.pages().size() + " chunks=" + chunks.size() + " requirements=" + requirements.size());
        int withEvidence = 0;
        for (FrameworkRequirement requirement : requirements) {
            List<RetrievedChunk> retrieved = retrievalService.retrieve(requirement, chunks);
            if (retrieved.isEmpty()) {
                lines.add(String.format("%-8s NONE                       | %s", requirement.getRequirementCode(),
                        requirement.getTitle()));
                continue;
            }
            withEvidence++;
            StringBuilder pages = new StringBuilder();
            for (RetrievedChunk chunk : retrieved) {
                pages.append(chunk.pageNumber()).append(',');
            }
            lines.add(String.format("%-8s score=%-7.1f pages=%-14s| %s", requirement.getRequirementCode(),
                    retrieved.get(0).score(), pages, requirement.getTitle()));
            lines.add("           top: " + oneLine(retrieved.get(0).text(), 170));
        }
        lines.add(0, "with evidence: " + withEvidence + " of " + requirements.size());

        Files.createDirectories(Path.of("target"));
        Files.write(Path.of("target", "brsr-retrieval-eval.txt"), lines);
        assertThat(withEvidence).isGreaterThan(0);
    }

    private static String oneLine(String text, int max) {
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() <= max ? flat : flat.substring(0, max) + "...";
    }
}
