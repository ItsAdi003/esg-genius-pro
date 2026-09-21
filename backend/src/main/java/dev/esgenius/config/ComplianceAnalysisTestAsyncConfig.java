package dev.esgenius.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
@Profile("test")
public class ComplianceAnalysisTestAsyncConfig {

    @Bean(name = "complianceAnalysisExecutor")
    public TaskExecutor complianceAnalysisExecutor() {
        return new SyncTaskExecutor();
    }
}
