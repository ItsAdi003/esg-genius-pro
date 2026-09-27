package dev.esgenius.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class AssistantAnswerResultParser {

    private final ObjectMapper objectMapper;

    public AssistantAnswerResultParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AssistantAnswerResult parse(String json, int passageCount) {
        if (json == null || json.isBlank()) {
            throw new AssistantAnswerException(
                    AssistantAnswerFailureCategory.PARSE_FAILURE,
                    "Assistant response was empty",
                    false);
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.hasNonNull("answer")) {
                throw validationFailure("Assistant response missing required field: answer");
            }
            if (!root.has("passageIndices") || !root.get("passageIndices").isArray()) {
                throw validationFailure("Assistant response missing required field: passageIndices");
            }

            String answer = root.get("answer").asText().trim();
            if (answer.isEmpty()) {
                throw validationFailure("Assistant response answer was blank");
            }

            Set<Integer> zeroBased = new LinkedHashSet<>();
            for (JsonNode indexNode : root.get("passageIndices")) {
                if (!indexNode.isIntegralNumber()) {
                    throw validationFailure("passageIndices must contain integers");
                }
                int oneBased = indexNode.asInt();
                if (oneBased < 1 || oneBased > passageCount) {
                    throw validationFailure(
                            "passageIndices value " + oneBased + " is outside 1.." + passageCount);
                }
                zeroBased.add(oneBased - 1);
            }

            return new AssistantAnswerResult(answer, List.copyOf(zeroBased));
        } catch (AssistantAnswerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AssistantAnswerException(
                    AssistantAnswerFailureCategory.PARSE_FAILURE,
                    "Failed to parse assistant response",
                    false,
                    null,
                    1,
                    ex.getMessage(),
                    ex);
        }
    }

    private AssistantAnswerException validationFailure(String message) {
        return new AssistantAnswerException(
                AssistantAnswerFailureCategory.VALIDATION_FAILURE,
                message,
                false);
    }
}
