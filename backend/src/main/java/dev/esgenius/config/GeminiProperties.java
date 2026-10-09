package dev.esgenius.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai.gemini")
public class GeminiProperties {

    private String apiKey = "";
    private String model = "gemini-3.5-flash";
    private String baseUrl = "https://generativelanguage.googleapis.com";
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(60);
    private String thinkingLevel = "low";
    private int maxRetries = 2;
    private Duration initialRetryBackoff = Duration.ofMillis(500);
    private Duration maxRetryBackoff = Duration.ofSeconds(8);
    private Duration interRequestDelay = Duration.ofMillis(300);
    private int maxConcurrentClassifications = 1;

    public static final int MIN_CONCURRENT_CLASSIFICATIONS = 1;
    public static final int MAX_CONCURRENT_CLASSIFICATIONS = 8;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getThinkingLevel() {
        return thinkingLevel;
    }

    public void setThinkingLevel(String thinkingLevel) {
        this.thinkingLevel = thinkingLevel;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getInitialRetryBackoff() {
        return initialRetryBackoff;
    }

    public void setInitialRetryBackoff(Duration initialRetryBackoff) {
        this.initialRetryBackoff = initialRetryBackoff;
    }

    public Duration getMaxRetryBackoff() {
        return maxRetryBackoff;
    }

    public void setMaxRetryBackoff(Duration maxRetryBackoff) {
        this.maxRetryBackoff = maxRetryBackoff;
    }

    public Duration getInterRequestDelay() {
        return interRequestDelay;
    }

    public void setInterRequestDelay(Duration interRequestDelay) {
        this.interRequestDelay = interRequestDelay;
    }

    public int getMaxConcurrentClassifications() {
        return maxConcurrentClassifications;
    }

    public void setMaxConcurrentClassifications(int maxConcurrentClassifications) {
        if (maxConcurrentClassifications < MIN_CONCURRENT_CLASSIFICATIONS
                || maxConcurrentClassifications > MAX_CONCURRENT_CLASSIFICATIONS) {
            throw new IllegalArgumentException(
                    "GEMINI_MAX_CONCURRENCY (app.ai.gemini.max-concurrent-classifications) must be an integer from 1 to 8 inclusive, but was "
                            + maxConcurrentClassifications);
        }
        this.maxConcurrentClassifications = maxConcurrentClassifications;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
