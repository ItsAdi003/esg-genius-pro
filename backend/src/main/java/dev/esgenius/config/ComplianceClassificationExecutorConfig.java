package dev.esgenius.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ComplianceClassificationExecutorConfig {

    public static final String EXECUTOR_BEAN_NAME = "complianceClassificationExecutor";

    @Bean(name = EXECUTOR_BEAN_NAME)
    public ThreadPoolTaskExecutor complianceClassificationExecutor(GeminiProperties geminiProperties) {
        int concurrency = geminiProperties.getMaxConcurrentClassifications();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("compliance-classify-");
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        executor.setQueueCapacity(256);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
