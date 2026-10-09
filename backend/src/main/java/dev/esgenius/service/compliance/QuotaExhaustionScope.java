package dev.esgenius.service.compliance;

/**
 * Thread-local flag so one analysis stops calling Gemini after a daily quota failure.
 * Analysis work runs on a dedicated executor thread; begin/end must run on that thread.
 */
public final class QuotaExhaustionScope {

    private static final ThreadLocal<Boolean> EXHAUSTED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private QuotaExhaustionScope() {
    }

    public static void begin() {
        EXHAUSTED.set(Boolean.FALSE);
    }

    public static void end() {
        EXHAUSTED.remove();
    }

    public static boolean isExhausted() {
        return Boolean.TRUE.equals(EXHAUSTED.get());
    }

    public static void markExhausted() {
        EXHAUSTED.set(Boolean.TRUE);
    }
}
