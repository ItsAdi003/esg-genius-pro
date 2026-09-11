package dev.esgenius.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.esgenius.service.compliance.ComplianceClassificationPromptBuilder;
import dev.esgenius.service.compliance.ComplianceClassificationProvider;
import dev.esgenius.service.compliance.ComplianceClassificationResultParser;
import dev.esgenius.service.compliance.gemini.GeminiComplianceClassificationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("!test")
public class ComplianceClassificationConfig {

    @Bean
    ComplianceClassificationProvider complianceClassificationProvider(
            GeminiProperties properties,
            RestClient geminiRestClient,
            ComplianceClassificationPromptBuilder promptBuilder,
            ComplianceClassificationResultParser resultParser,
            ObjectMapper objectMapper) {
        return new GeminiComplianceClassificationProvider(
                properties, geminiRestClient, promptBuilder, resultParser, objectMapper);
    }
}
