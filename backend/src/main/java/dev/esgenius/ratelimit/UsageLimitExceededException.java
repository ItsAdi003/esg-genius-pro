package dev.esgenius.ratelimit;

/**
 * A caller exceeded a usage limit. {@code retryAfterSeconds} is the delay until
 * the oldest counted event leaves the rolling window.
 */
public class UsageLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public UsageLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
