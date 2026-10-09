package dev.esgenius.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfLimitsPropertiesTest {

    @Test
    void defaultsMatchInCodeLimits() {
        PdfLimitsProperties properties = new PdfLimitsProperties();

        assertThat(properties.getMaxPages()).isEqualTo(400);
        assertThat(properties.getMaxExtractedChars()).isEqualTo(3_000_000);
    }
}
