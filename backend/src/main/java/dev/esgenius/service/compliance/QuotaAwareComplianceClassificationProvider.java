package dev.esgenius.service.compliance;

/**
 * Stops delegating to Gemini (or any provider) after a daily quota failure for this analysis.
 */
public final class QuotaAwareComplianceClassificationProvider implements ComplianceClassificationProvider {

    private final ComplianceClassificationProvider delegate;

    public QuotaAwareComplianceClassificationProvider(ComplianceClassificationProvider delegate) {
        this.delegate = delegate;
    }

    public ComplianceClassificationProvider delegate() {
        return delegate;
    }

    @Override
    public ComplianceClassificationResult classify(ComplianceClassificationRequest request) {
        if (isQuotaExhausted(request)) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.QUOTA_EXHAUSTED,
                    "Gemini daily quota has already been exhausted for this analysis",
                    false);
        }
        try {
            return delegate.classify(request);
        } catch (ComplianceClassificationException ex) {
            if (ex.getCategory() == ClassificationFailureCategory.QUOTA_EXHAUSTED) {
                markQuotaExhausted(request);
            }
            throw ex;
        }
    }

    private static boolean isQuotaExhausted(ComplianceClassificationRequest request) {
        return request.runContext() != null && request.runContext().isQuotaExhausted();
    }

    private static void markQuotaExhausted(ComplianceClassificationRequest request) {
        if (request.runContext() != null) {
            request.runContext().markQuotaExhausted();
        }
    }
}
