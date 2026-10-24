package dev.esgenius.service.compliance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.esgenius.entity.AssessmentStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ComplianceClassificationResultParser {

    private final ObjectMapper objectMapper;

    public ComplianceClassificationResultParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ComplianceClassificationResult parse(String json) {
        if (json == null || json.isBlank()) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.PARSE_FAILURE,
                    "Classification response was empty",
                    false);
        }

        try {
            return parseNode(objectMapper.readTree(json));
        } catch (ComplianceClassificationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.PARSE_FAILURE,
                    "Failed to parse classification response",
                    false,
                    null,
                    1,
                    ex.getMessage(),
                    ex);
        }
    }

    /**
     * Parses a batched response: a JSON array with one object per requirement, each carrying the
     * requirement's {@code requirementCode} plus the single-result fields. Entries that are malformed
     * (no code, missing fields, invalid status or confidence) are skipped, so that requirement is left
     * for the caller's fallback while the valid entries are kept. A response that is not an array at all
     * fails as a whole.
     */
    public Map<String, ComplianceClassificationResult> parseBatch(String json) {
        if (json == null || json.isBlank()) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.PARSE_FAILURE,
                    "Classification response was empty",
                    false);
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.PARSE_FAILURE,
                    "Failed to parse classification response",
                    false,
                    null,
                    1,
                    ex.getMessage(),
                    ex);
        }
        if (root == null || !root.isArray()) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.PARSE_FAILURE,
                    "Batched classification response was not a JSON array",
                    false);
        }

        Map<String, ComplianceClassificationResult> results = new LinkedHashMap<>();
        for (JsonNode element : root) {
            JsonNode code = element.get("requirementCode");
            if (code == null || code.isNull() || code.asText().isBlank()) {
                continue;
            }
            try {
                results.put(code.asText().trim(), parseNode(element));
            } catch (ComplianceClassificationException ex) {
                // One bad entry must not discard the rest of the batch.
            }
        }
        return results;
    }

    private ComplianceClassificationResult parseNode(JsonNode root) {
        if (!root.hasNonNull("status")) {
            throw validationFailure("Classification response missing required field: status");
        }
        if (!root.has("confidence") || root.get("confidence").isNull()) {
            throw validationFailure("Classification response missing required field: confidence");
        }
        if (!root.hasNonNull("explanation")) {
            throw validationFailure("Classification response missing required field: explanation");
        }
        if (!root.has("gap")) {
            throw validationFailure("Classification response missing required field: gap");
        }
        if (!root.has("recommendation")) {
            throw validationFailure("Classification response missing required field: recommendation");
        }

        AssessmentStatus status = parseStatus(root.get("status").asText());
        double confidence = root.get("confidence").asDouble();
        if (confidence < 0.0 || confidence > 1.0) {
            throw validationFailure(
                    "Classification confidence must be between 0.0 and 1.0, got: " + confidence);
        }

        String explanation = root.get("explanation").asText();
        String gap = root.get("gap").isNull() ? null : root.get("gap").asText();
        String recommendation = root.get("recommendation").isNull() ? null : root.get("recommendation").asText();

        return new ComplianceClassificationResult(status, confidence, explanation, gap, recommendation);
    }

    private AssessmentStatus parseStatus(String rawStatus) {
        try {
            AssessmentStatus status = AssessmentStatus.valueOf(rawStatus.trim());
            if (status == AssessmentStatus.NOT_APPLICABLE) {
                throw validationFailure("Invalid classification status: " + rawStatus);
            }
            return status;
        } catch (IllegalArgumentException ex) {
            throw validationFailure("Invalid classification status: " + rawStatus);
        }
    }

    private ComplianceClassificationException validationFailure(String message) {
        return new ComplianceClassificationException(
                ClassificationFailureCategory.VALIDATION_FAILURE,
                message,
                false);
    }
}
