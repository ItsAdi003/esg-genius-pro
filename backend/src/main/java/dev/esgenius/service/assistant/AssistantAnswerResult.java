package dev.esgenius.service.assistant;

import java.util.List;

/**
 * @param passageIndices zero-based indexes into the evidence passages supplied with the question
 */
public record AssistantAnswerResult(String answer, List<Integer> passageIndices) {
}
