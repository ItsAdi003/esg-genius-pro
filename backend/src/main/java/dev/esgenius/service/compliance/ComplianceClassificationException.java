package dev.esgenius.service.compliance;

public class ComplianceClassificationException extends RuntimeException {

    private final ClassificationFailureCategory category;
    private final Integer httpStatus;
    private final int attempt;
    private final String safeDetail;
    private final boolean retryable;

    public ComplianceClassificationException(
            ClassificationFailureCategory category,
            String message,
            boolean retryable) {
        this(category, message, retryable, null, 1, null);
    }

    public ComplianceClassificationException(
            ClassificationFailureCategory category,
            String message,
            boolean retryable,
            Integer httpStatus,
            int attempt,
            String safeDetail) {
        super(message);
        this.category = category;
        this.httpStatus = httpStatus;
        this.attempt = attempt;
        this.safeDetail = safeDetail;
        this.retryable = retryable;
    }

    public ComplianceClassificationException(
            ClassificationFailureCategory category,
            String message,
            boolean retryable,
            Integer httpStatus,
            int attempt,
            String safeDetail,
            Throwable cause) {
        super(message, cause);
        this.category = category;
        this.httpStatus = httpStatus;
        this.attempt = attempt;
        this.safeDetail = safeDetail;
        this.retryable = retryable;
    }

    public ClassificationFailureCategory getCategory() {
        return category;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public int getAttempt() {
        return attempt;
    }

    public String getSafeDetail() {
        return safeDetail;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public ComplianceClassificationException withAttempt(int newAttempt) {
        return new ComplianceClassificationException(
                category, getMessage(), retryable, httpStatus, newAttempt, safeDetail, getCause());
    }
}
