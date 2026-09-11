package dev.esgenius.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkingServiceTest {

    private final TextChunkingService chunkingService = new TextChunkingService();

    @Test
    void chunkReturnsEmptyForBlankText() {
        assertThat(chunkingService.chunk(null)).isEmpty();
        assertThat(chunkingService.chunk("   ")).isEmpty();
    }

    @Test
    void chunkReturnsSingleChunkForShortText() {
        String text = "Short ESG disclosure paragraph about emissions.";
        List<TextChunk> chunks = chunkingService.chunk(text);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).chunkIndex()).isZero();
        assertThat(chunks.get(0).text()).isEqualTo(text);
    }

    @Test
    void chunkProducesMultipleChunksWithinTargetSize() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            builder.append("Paragraph ").append(i)
                    .append(": The company reduced Scope 1 and Scope 2 greenhouse gas emissions through energy efficiency initiatives.\n\n");
        }
        String text = builder.toString();

        List<TextChunk> chunks = chunkingService.chunk(text);

        assertThat(chunks.size()).isGreaterThan(1);
        for (TextChunk chunk : chunks) {
            assertThat(chunk.text().length()).isLessThanOrEqualTo(TextChunkingService.TARGET_MAX_CHARS + 50);
            assertThat(chunk.text().length()).isGreaterThan(100);
        }
    }

    @Test
    void chunkOverlapSharesTrailingContentWithNextChunk() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            builder.append("Section ").append(i)
                    .append(" discusses renewable energy procurement and carbon reduction targets for FY2025.\n\n");
        }
        String text = builder.toString();

        List<TextChunk> chunks = chunkingService.chunk(text);
        assertThat(chunks.size()).isGreaterThan(1);

        String firstTail = chunks.get(0).text().substring(
                Math.max(0, chunks.get(0).text().length() - TextChunkingService.OVERLAP_CHARS));
        String secondHead = chunks.get(1).text().substring(
                0, Math.min(chunks.get(1).text().length(), TextChunkingService.OVERLAP_CHARS + 20));

        assertThat(secondHead).contains(firstTail.substring(Math.max(0, firstTail.length() - 40)).trim());
    }

    @Test
    void chunkIsDeterministic() {
        String text = buildLongText();
        List<TextChunk> first = chunkingService.chunk(text);
        List<TextChunk> second = chunkingService.chunk(text);

        assertThat(first).hasSize(second.size());
        for (int i = 0; i < first.size(); i++) {
            assertThat(first.get(i).chunkIndex()).isEqualTo(second.get(i).chunkIndex());
            assertThat(first.get(i).text()).isEqualTo(second.get(i).text());
        }
    }

    @Test
    void chunkIndexesAreSequentialNotPageNumbers() {
        String text = buildLongText();
        List<TextChunk> chunks = chunkingService.chunk(text);

        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).chunkIndex()).isEqualTo(i);
        }
    }

    @Test
    void legacyChunkingLeavesPageNumberNull() {
        List<TextChunk> chunks = chunkingService.chunk(buildLongText());

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allMatch(chunk -> chunk.pageNumber() == null);
    }

    @Test
    void pageAwareChunkingDoesNotCrossPageBoundaries() {
        List<TextChunk> chunks = chunkingService.chunkPages(List.of(
                new ExtractedPdfPage(1, "Page one disclosure about renewable energy procurement."),
                new ExtractedPdfPage(2, "Page two disclosure about Scope 1 emissions from owned facilities.")));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).pageNumber()).isEqualTo(1);
        assertThat(chunks.get(0).text()).contains("renewable energy");
        assertThat(chunks.get(1).pageNumber()).isEqualTo(2);
        assertThat(chunks.get(1).text()).contains("Scope 1");
    }

    @Test
    void longPageSplitsIntoMultipleChunksWithSamePageNumber() {
        StringBuilder pageText = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            pageText.append("Paragraph ").append(i)
                    .append(": Scope 1 emissions from owned facilities were reduced through efficiency projects.\n\n");
        }

        List<TextChunk> chunks = chunkingService.chunkPages(List.of(new ExtractedPdfPage(27, pageText.toString())));

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).allMatch(chunk -> chunk.pageNumber() == 27);
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).chunkIndex()).isEqualTo(i);
        }
    }

    @Test
    void shortPagesRemainSeparateChunks() {
        List<TextChunk> chunks = chunkingService.chunkPages(List.of(
                new ExtractedPdfPage(3, "Short page three text."),
                new ExtractedPdfPage(4, "Short page four text.")));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).pageNumber()).isEqualTo(3);
        assertThat(chunks.get(1).pageNumber()).isEqualTo(4);
    }

    private String buildLongText() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            builder.append("Employee health and safety training covered all manufacturing sites. ");
            builder.append("Water withdrawal from groundwater sources was monitored quarterly. ");
            builder.append("Board composition includes independent directors with sustainability expertise.\n\n");
        }
        return builder.toString();
    }
}
