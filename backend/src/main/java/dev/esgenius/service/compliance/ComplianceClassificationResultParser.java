package dev.esgenius.service.compliance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.esgenius.entity.AssessmentStatus;
import org.springframework.stereotype.Component;

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
            JsonNode root = objectMapper.readTree(json);
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

    private AssessmentStatus parseStatus(String rawStatus) {
        try {
            return AssessmentStatus.valueOf(rawStatus.trim());
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
