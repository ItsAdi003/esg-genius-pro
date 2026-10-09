package dev.esgenius.service.compliance;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-analysis state shared by classification workers.
 * Quota exhaustion is visible across threads: once set, no new Gemini call starts for this analysis.
 * Calls already inside the provider may finish.
 */
public final class AnalysisRunContext {

    private final AtomicBoolean quotaExhausted = new AtomicBoolean(false);

    public boolean isQuotaExhausted() {
        return quotaExhausted.get();
    }

    public void markQuotaExhausted() {
        quotaExhausted.set(true);
    }
}
