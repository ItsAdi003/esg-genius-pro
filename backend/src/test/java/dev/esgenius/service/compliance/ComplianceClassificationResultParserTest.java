package dev.esgenius.service.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.esgenius.entity.AssessmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComplianceClassificationResultParserTest {

    private ComplianceClassificationResultParser parser;

    @BeforeEach
    void setUp() {
        parser = new ComplianceClassificationResultParser(new ObjectMapper());
    }

    @Test
    void parsesValidCoveredResult() {
        ComplianceClassificationResult result = parser.parse("""
                {
                  "status": "COVERED",
                  "confidence": 0.92,
                  "explanation": "Scope 1 emissions are disclosed.",
                  "gap": null,
                  "recommendation": null
                }
                """);

        assertThat(result.status()).isEqualTo(AssessmentStatus.COVERED);
        assertThat(result.confidence()).isEqualTo(0.92);
        assertThat(result.explanation()).contains("Scope 1");
        assertThat(result.gap()).isNull();
        assertThat(result.recommendation()).isNull();
    }

    @Test
    void parsesPartiallyCoveredResult() {
        ComplianceClassificationResult result = parser.parse("""
                {
                  "status": "PARTIALLY_COVERED",
                  "confidence": 0.7,
                  "explanation": "Partial disclosure found.",
                  "gap": "Missing baseline year.",
                  "recommendation": "Add baseline year."
                }
                """);

        assertThat(result.status()).isEqualTo(AssessmentStatus.PARTIALLY_COVERED);
        assertThat(result.gap()).isEqualTo("Missing baseline year.");
    }

    @Test
    void rejectsInvalidStatus() {
        ComplianceClassificationException ex = assertThrows(ComplianceClassificationException.class, () ->
                parser.parse("""
                        {
                          "status": "MAYBE_COVERED",
                          "confidence": 0.5,
                          "explanation": "x",
                          "gap": null,
                          "recommendation": null
                        }
                        """));

        assertThat(ex.getMessage()).contains("Invalid classification status");
    }

    @Test
    void rejectsConfidenceBelowZero() {
        ComplianceClassificationException ex = assertThrows(ComplianceClassificationException.class, () ->
                parser.parse("""
                        {
                          "status": "COVERED",
                          "confidence": -0.1,
                          "explanation": "x",
                          "gap": null,
                          "recommendation": null
                        }
                        """));

        assertThat(ex.getMessage()).contains("between 0.0 and 1.0");
    }

    @Test
    void rejectsConfidenceAboveOne() {
        assertThrows(ComplianceClassificationException.class, () ->
                parser.parse("""
                        {
                          "status": "COVERED",
                          "confidence": 1.1,
                          "explanation": "x",
                          "gap": null,
                          "recommendation": null
                        }
                        """));
    }

    @Test
    void rejectsMissingRequiredField() {
        ComplianceClassificationException ex = assertThrows(ComplianceClassificationException.class, () ->
                parser.parse("""
                        {
                          "status": "COVERED",
                          "confidence": 0.5
                        }
                        """));

        assertThat(ex.getMessage()).contains("explanation");
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(ComplianceClassificationException.class, () -> parser.parse("not-json"));
    }
}
