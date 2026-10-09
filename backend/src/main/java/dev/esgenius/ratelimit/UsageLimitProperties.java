package dev.esgenius.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * In-code defaults for {@code app.limits.*}. A value of {@code 0} or below disables that limit.
 * Deployment config may override these with RATE_LIMIT_UPLOADS_PER_DAY,
 * RATE_LIMIT_ANALYSES_PER_DAY, RATE_LIMIT_ASSISTANT_PER_HOUR, and
 * RATE_LIMIT_GLOBAL_ANALYSES_PER_DAY.
 */
@ConfigurationProperties(prefix = "app.limits")
public class UsageLimitProperties {

    private int uploadsPerUserPerDay = 20;
    private int analysesPerUserPerDay = 5;
    private int assistantAsksPerUserPerHour = 30;
    private int globalAnalysesPerDay = 20;

    public int getUploadsPerUserPerDay() {
        return uploadsPerUserPerDay;
    }

    public void setUploadsPerUserPerDay(int uploadsPerUserPerDay) {
        this.uploadsPerUserPerDay = uploadsPerUserPerDay;
    }

    public int getAnalysesPerUserPerDay() {
        return analysesPerUserPerDay;
    }

    public void setAnalysesPerUserPerDay(int analysesPerUserPerDay) {
        this.analysesPerUserPerDay = analysesPerUserPerDay;
    }

    public int getAssistantAsksPerUserPerHour() {
        return assistantAsksPerUserPerHour;
    }

    public void setAssistantAsksPerUserPerHour(int assistantAsksPerUserPerHour) {
        this.assistantAsksPerUserPerHour = assistantAsksPerUserPerHour;
    }

    public int getGlobalAnalysesPerDay() {
        return globalAnalysesPerDay;
    }

    public void setGlobalAnalysesPerDay(int globalAnalysesPerDay) {
        this.globalAnalysesPerDay = globalAnalysesPerDay;
    }
}
