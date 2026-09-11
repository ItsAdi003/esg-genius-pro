package dev.esgenius.config;

import dev.esgenius.service.compliance.ComplianceClassificationProvider;
import dev.esgenius.service.compliance.gemini.GeminiComplianceClassificationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("wiring")
class ComplianceClassificationProductionWiringTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ComplianceClassificationProvider classificationProvider;

    @Test
    void productionStyleContextRegistersExactlyOneProvider() {
        var providers = applicationContext.getBeansOfType(ComplianceClassificationProvider.class);

        assertThat(providers).hasSize(1);
        assertThat(providers.values().iterator().next())
                .isInstanceOf(GeminiComplianceClassificationProvider.class);
        assertThat(classificationProvider).isInstanceOf(GeminiComplianceClassificationProvider.class);
    }
}
