package dev.esgenius.service.assistant;

public class AssistantAnswerException extends RuntimeException {

    private final AssistantAnswerFailureCategory category;
    private final Integer httpStatus;
    private final int attempt;
    private final String safeDetail;
    private final boolean retryable;

    public AssistantAnswerException(
            AssistantAnswerFailureCategory category,
            String message,
            boolean retryable) {
        this(category, message, retryable, null, 1, null);
    }

    public AssistantAnswerException(
            AssistantAnswerFailureCategory category,
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

    public AssistantAnswerException(
            AssistantAnswerFailureCategory category,
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

    public AssistantAnswerFailureCategory getCategory() {
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

    public AssistantAnswerException withAttempt(int newAttempt) {
        return new AssistantAnswerException(
                category, getMessage(), retryable, httpStatus, newAttempt, safeDetail, getCause());
    }
}
