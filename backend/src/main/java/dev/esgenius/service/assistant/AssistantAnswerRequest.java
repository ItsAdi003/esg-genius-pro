package dev.esgenius.service.assistant;

import java.util.List;

public record AssistantAnswerRequest(String question, List<String> evidencePassages) {
}
