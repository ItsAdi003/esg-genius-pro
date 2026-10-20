package dev.esgenius.service.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.esgenius.entity.AssessmentStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComplianceClassificationBatchParserTest {

    private final ComplianceClassificationResultParser parser =
            new ComplianceClassificationResultParser(new ObjectMapper());

    @Test
    void parsesOneResultPerRequirementCode() {
        Map<String, ComplianceClassificationResult> results = parser.parseBatch("""
                [
                  {"requirementCode":"P6-E01","status":"COVERED","confidence":0.9,"explanation":"All present.",
                   "gap":null,"recommendation":null},
                  {"requirementCode":"P6-E03","status":"PARTIALLY_COVERED","confidence":0.6,"explanation":"Some.",
                   "gap":"Missing intensity.","recommendation":"Add intensity."}
                ]
                """);

        assertThat(results).containsOnlyKeys("P6-E01", "P6-E03");
        assertThat(results.get("P6-E01").status()).isEqualTo(AssessmentStatus.COVERED);
        assertThat(results.get("P6-E03").gap()).isEqualTo("Missing intensity.");
        assertThat(results.get("P6-E03").recommendation()).isEqualTo("Add intensity.");
    }

    @Test
    void trimsTheRequirementCode() {
        Map<String, ComplianceClassificationResult> results = parser.parseBatch("""
                [{"requirementCode":"  A-01 ","status":"COVERED","confidence":1.0,"explanation":"Yes.",
                  "gap":null,"recommendation":null}]
                """);

        assertThat(results).containsOnlyKeys("A-01");
    }

    @Test
    void skipsMalformedEntriesButKeepsTheValidOnes() {
        Map<String, ComplianceClassificationResult> results = parser.parseBatch("""
                [
                  {"requirementCode":"P1-E01","status":"COVERED","confidence":0.9,"explanation":"Fine.",
                   "gap":null,"recommendation":null},
                  {"requirementCode":"P1-E02","status":"MOSTLY","confidence":0.9,"explanation":"Bad status.",
                   "gap":null,"recommendation":null},
                  {"requirementCode":"P1-E03","status":"COVERED","confidence":4.2,"explanation":"Bad confidence.",
                   "gap":null,"recommendation":null},
                  {"status":"COVERED","confidence":0.9,"explanation":"No code.","gap":null,"recommendation":null},
                  {"requirementCode":"P1-E04","status":"COVERED","explanation":"No confidence.",
                   "gap":null,"recommendation":null},
                  {"requirementCode":"P1-E05","status":"NOT_COVERED","confidence":0.8,"explanation":"Absent.",
                   "gap":"Nothing disclosed.","recommendation":"Disclose it."}
                ]
                """);

        assertThat(results).containsOnlyKeys("P1-E01", "P1-E05");
    }

    @Test
    void anEmptyArrayIsAnEmptyResult() {
        assertThat(parser.parseBatch("[]")).isEmpty();
    }

    @Test
    void aSingleObjectIsNotAValidBatchResponse() {
        ComplianceClassificationException failure = assertThrows(ComplianceClassificationException.class, () ->
                parser.parseBatch("""
                        {"status":"COVERED","confidence":0.9,"explanation":"x","gap":null,"recommendation":null}
                        """));

        assertThat(failure.getCategory()).isEqualTo(ClassificationFailureCategory.PARSE_FAILURE);
    }

    @Test
    void emptyOrUnparseableTextFails() {
        assertThat(assertThrows(ComplianceClassificationException.class, () -> parser.parseBatch(""))
                .getCategory()).isEqualTo(ClassificationFailureCategory.PARSE_FAILURE);
        assertThat(assertThrows(ComplianceClassificationException.class, () -> parser.parseBatch("not json {"))
                .getCategory()).isEqualTo(ClassificationFailureCategory.PARSE_FAILURE);
    }

    @Test
    void theSingleResultParserStillWorks() {
        ComplianceClassificationResult result = parser.parse("""
                {"status":"COVERED","confidence":0.7,"explanation":"Fine.","gap":null,"recommendation":null}
                """);

        assertThat(result.status()).isEqualTo(AssessmentStatus.COVERED);
        assertThat(result.confidence()).isEqualTo(0.7);
    }
}
