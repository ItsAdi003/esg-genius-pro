package dev.esgenius.config;

import dev.esgenius.service.assistant.AssistantAnswerProvider;
import dev.esgenius.service.assistant.AssistantAnswerRequest;
import dev.esgenius.service.assistant.AssistantAnswerResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("test")
public class TestAssistantAnswerConfig {

    @Bean
    @Primary
    AssistantAnswerProvider testAssistantAnswerProvider() {
        return (AssistantAnswerRequest request) -> new AssistantAnswerResult(
                "Test assistant answer grounded in the supplied passage.",
                request.evidencePassages() == null || request.evidencePassages().isEmpty()
                        ? List.of()
                        : List.of(0));
    }
}
