package dev.esgenius.config;

import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.service.compliance.ComplianceClassificationProvider;
import dev.esgenius.service.compliance.ComplianceClassificationRequest;
import dev.esgenius.service.compliance.ComplianceClassificationResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestComplianceClassificationConfig {

    @Bean
    @Primary
    ComplianceClassificationProvider testComplianceClassificationProvider() {
        return (ComplianceClassificationRequest request) -> new ComplianceClassificationResult(
                AssessmentStatus.COVERED,
                0.85,
                "The submitted document evidence appears to address this requirement.",
                null,
                null);
    }
}
