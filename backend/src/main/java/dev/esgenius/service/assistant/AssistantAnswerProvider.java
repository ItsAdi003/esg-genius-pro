package dev.esgenius.service.assistant;

public interface AssistantAnswerProvider {

    AssistantAnswerResult answer(AssistantAnswerRequest request);
}
