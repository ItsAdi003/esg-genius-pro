package dev.esgenius.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.esgenius.service.assistant.AssistantAnswerPromptBuilder;
import dev.esgenius.service.assistant.AssistantAnswerProvider;
import dev.esgenius.service.assistant.AssistantAnswerResultParser;
import dev.esgenius.service.assistant.gemini.GeminiAssistantAnswerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("!test")
public class AssistantAnswerConfig {

    @Bean
    AssistantAnswerProvider assistantAnswerProvider(
            GeminiProperties properties,
            RestClient geminiRestClient,
            AssistantAnswerPromptBuilder promptBuilder,
            AssistantAnswerResultParser resultParser,
            ObjectMapper objectMapper) {
        return new GeminiAssistantAnswerProvider(
                properties, geminiRestClient, promptBuilder, resultParser, objectMapper);
    }
}
