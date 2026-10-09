package dev.esgenius.service.compliance;

/**
 * Stops delegating to Gemini (or any provider) after a daily quota failure on this thread.
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
        if (QuotaExhaustionScope.isExhausted()) {
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.QUOTA_EXHAUSTED,
                    "Gemini daily quota has already been exhausted for this analysis",
                    false);
        }
        try {
            return delegate.classify(request);
        } catch (ComplianceClassificationException ex) {
            if (ex.getCategory() == ClassificationFailureCategory.QUOTA_EXHAUSTED) {
                QuotaExhaustionScope.markExhausted();
            }
            throw ex;
        }
    }
}
