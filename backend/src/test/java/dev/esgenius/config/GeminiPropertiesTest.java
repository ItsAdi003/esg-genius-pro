package dev.esgenius.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiPropertiesTest {

    @Test
    void maxConcurrentClassificationsDefaultsToOne() {
        assertThat(new GeminiProperties().getMaxConcurrentClassifications()).isEqualTo(1);
    }

    @Test
    void assistantTotalDeadlineDefaultsToEightySeconds() {
        assertThat(new GeminiProperties().getAssistantTotalDeadline()).isEqualTo(Duration.ofSeconds(80));
    }

    @Test
    void acceptsConcurrencyBounds() {
        GeminiProperties properties = new GeminiProperties();

        properties.setMaxConcurrentClassifications(1);
        assertThat(properties.getMaxConcurrentClassifications()).isEqualTo(1);

        properties.setMaxConcurrentClassifications(8);
        assertThat(properties.getMaxConcurrentClassifications()).isEqualTo(8);
    }

    @Test
    void rejectsConcurrencyBelowOne() {
        assertThatThrownBy(() -> new GeminiProperties().setMaxConcurrentClassifications(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GEMINI_MAX_CONCURRENCY")
                .hasMessageContaining("1 to 8")
                .hasMessageContaining("0");
    }

    @Test
    void rejectsConcurrencyAboveEight() {
        assertThatThrownBy(() -> new GeminiProperties().setMaxConcurrentClassifications(9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GEMINI_MAX_CONCURRENCY")
                .hasMessageContaining("1 to 8")
                .hasMessageContaining("9");
    }
}
