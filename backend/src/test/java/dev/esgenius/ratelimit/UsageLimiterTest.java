package dev.esgenius.ratelimit;

import dev.esgenius.service.Caller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsageLimiterTest {

    private static final Instant START = Instant.parse("2026-10-09T12:00:00Z");

    private UsageLimitProperties properties;
    private MutableClock clock;
    private UsageLimiter limiter;

    @BeforeEach
    void setUp() {
        properties = new UsageLimitProperties();
        clock = new MutableClock(START);
        limiter = new UsageLimiter(properties, clock);
    }

    @Test
    void rollingWindowExpiresAndReportsWhenToRetry() {
        properties.setAnalysesPerUserPerDay(5);
        properties.setGlobalAnalysesPerDay(100);
        Caller user = user();

        for (int i = 0; i < 5; i++) {
            limiter.consumeAnalysis(user);
        }

        UsageLimitExceededException blocked = catchAnalysis(user);
        assertThat(blocked.getMessage())
                .isEqualTo("You have reached the limit of 5 analyses per day. Try again in 24h.");
        assertThat(blocked.getRetryAfterSeconds()).isEqualTo(86_400);

        clock.advance(Duration.ofHours(20).plusMinutes(48));
        UsageLimitExceededException later = catchAnalysis(user);
        assertThat(later.getMessage())
                .isEqualTo("You have reached the limit of 5 analyses per day. Try again in 3h 12m.");
        assertThat(later.getRetryAfterSeconds()).isEqualTo(3 * 3600 + 12 * 60);

        clock.advance(Duration.ofHours(3).plusMinutes(12));
        limiter.consumeAnalysis(user);
    }

    @Test
    void perUserLimitsAreIsolated() {
        properties.setUploadsPerUserPerDay(1);
        Caller first = user();
        Caller second = user();

        limiter.consumeUpload(first);
        assertThatThrownBy(() -> limiter.consumeUpload(first)).isInstanceOf(UsageLimitExceededException.class);

        limiter.consumeUpload(second);
    }

    @Test
    void adminsAreExemptFromPerUserLimitsAndAreNotCounted() {
        properties.setUploadsPerUserPerDay(1);
        properties.setAssistantAsksPerUserPerHour(1);
        Caller admin = new Caller(UUID.randomUUID(), true);
        Caller user = user();

        limiter.consumeUpload(admin);
        limiter.consumeUpload(admin);
        limiter.consumeAssistantAsk(admin);
        limiter.consumeAssistantAsk(admin);
        assertThat(limiter.trackedKeyCount()).isZero();

        limiter.consumeUpload(user);
        assertThatThrownBy(() -> limiter.consumeUpload(user)).isInstanceOf(UsageLimitExceededException.class);
        limiter.consumeAssistantAsk(user);
    }

    @Test
    void globalAnalysisCapIncludesAdminsAndDoesNotBurnARejectedPerUserSlot() {
        properties.setAnalysesPerUserPerDay(5);
        properties.setGlobalAnalysesPerDay(2);
        Caller admin = new Caller(UUID.randomUUID(), true);
        Caller first = user();
        Caller second = user();

        limiter.consumeAnalysis(admin);
        limiter.consumeAnalysis(first);

        UsageLimitExceededException blocked = catchAnalysis(second);
        assertThat(blocked.getMessage())
                .isEqualTo("You have reached the limit of 2 analyses per day. Try again in 24h.");
        assertThatThrownBy(() -> limiter.consumeAnalysis(admin)).isInstanceOf(UsageLimitExceededException.class);

        properties.setGlobalAnalysesPerDay(100);
        for (int i = 0; i < 5; i++) {
            limiter.consumeAnalysis(second);
        }
        assertThat(catchAnalysis(second).getMessage())
                .isEqualTo("You have reached the limit of 5 analyses per day. Try again in 24h.");
    }

    @Test
    void nonPositiveLimitsAreDisabled() {
        properties.setUploadsPerUserPerDay(0);
        properties.setAnalysesPerUserPerDay(-1);
        properties.setAssistantAsksPerUserPerHour(0);
        properties.setGlobalAnalysesPerDay(-5);
        Caller user = user();

        for (int i = 0; i < 50; i++) {
            limiter.consumeUpload(user);
            limiter.consumeAnalysis(user);
            limiter.consumeAssistantAsk(user);
        }
        assertThat(limiter.trackedKeyCount()).isZero();
    }

    @Test
    void missingIdentityIsNotLimited() {
        properties.setUploadsPerUserPerDay(1);
        properties.setAnalysesPerUserPerDay(1);
        properties.setAssistantAsksPerUserPerHour(1);
        properties.setGlobalAnalysesPerDay(1);

        for (int i = 0; i < 10; i++) {
            limiter.consumeUpload(null);
            limiter.consumeUpload(Caller.anonymous());
            limiter.consumeUpload(Caller.unidentifiedAdmin());
            limiter.consumeAnalysis(Caller.unidentifiedAdmin());
            limiter.consumeAssistantAsk(Caller.anonymous());
        }
        assertThat(limiter.trackedKeyCount()).isZero();
    }

    @Test
    void snapshotAfterConsumesReportsUsedLimitAndResetWithoutConsuming() {
        properties.setUploadsPerUserPerDay(20);
        properties.setAnalysesPerUserPerDay(5);
        properties.setAssistantAsksPerUserPerHour(30);
        properties.setGlobalAnalysesPerDay(0);
        Caller user = user();

        limiter.consumeUpload(user);
        limiter.consumeUpload(user);
        limiter.consumeUpload(user);
        limiter.consumeAnalysis(user);
        limiter.consumeAssistantAsk(user);
        limiter.consumeAssistantAsk(user);

        UsageLimiter.PerUserLimitSnapshot first = limiter.snapshot(user);
        assertWindow(first.uploadsPerDay(), 20, 3, 86_400);
        assertWindow(first.analysesPerDay(), 5, 1, 86_400);
        assertWindow(first.assistantAsksPerHour(), 30, 2, 3_600);

        UsageLimiter.PerUserLimitSnapshot second = limiter.snapshot(user);
        assertThat(second).isEqualTo(first);
        assertThat(limiter.trackedKeyCount()).isEqualTo(3);

        for (int i = 0; i < 17; i++) {
            limiter.consumeUpload(user);
        }
        assertThatThrownBy(() -> limiter.consumeUpload(user)).isInstanceOf(UsageLimitExceededException.class);
        assertWindow(limiter.snapshot(user).uploadsPerDay(), 20, 20, 86_400);
    }

    @Test
    void snapshotUsedDropsWhenTheOldestEventLeavesTheWindow() {
        properties.setUploadsPerUserPerDay(20);
        properties.setAnalysesPerUserPerDay(5);
        properties.setAssistantAsksPerUserPerHour(30);
        Caller user = user();

        limiter.consumeUpload(user);
        clock.advance(Duration.ofHours(1));
        limiter.consumeUpload(user);
        clock.advance(Duration.ofHours(1));
        limiter.consumeUpload(user);

        UsageLimiter.PerUserLimitSnapshot midWindow = limiter.snapshot(user);
        assertWindow(midWindow.uploadsPerDay(), 20, 3, 22 * 3600);

        clock.advance(Duration.ofHours(22));
        UsageLimiter.PerUserLimitSnapshot afterOldestExpired = limiter.snapshot(user);
        assertWindow(afterOldestExpired.uploadsPerDay(), 20, 2, 3600);

        clock.advance(Duration.ofHours(2));
        UsageLimiter.PerUserLimitSnapshot empty = limiter.snapshot(user);
        assertWindow(empty.uploadsPerDay(), 20, 0, 0);
        assertWindow(empty.analysesPerDay(), 5, 0, 0);
        assertWindow(empty.assistantAsksPerHour(), 30, 0, 0);
    }

    @Test
    void snapshotIsNullWhenTheLimitIsDisabled() {
        properties.setUploadsPerUserPerDay(0);
        properties.setAnalysesPerUserPerDay(-1);
        properties.setAssistantAsksPerUserPerHour(0);
        Caller user = user();

        limiter.consumeUpload(user);
        limiter.consumeAnalysis(user);
        limiter.consumeAssistantAsk(user);

        UsageLimiter.PerUserLimitSnapshot snapshot = limiter.snapshot(user);
        assertThat(snapshot.uploadsPerDay()).isNull();
        assertThat(snapshot.analysesPerDay()).isNull();
        assertThat(snapshot.assistantAsksPerHour()).isNull();
    }

    @Test
    void snapshotIsNullForAdminsEvenAfterConsumeAttempts() {
        properties.setUploadsPerUserPerDay(20);
        properties.setAnalysesPerUserPerDay(5);
        properties.setAssistantAsksPerUserPerHour(30);
        Caller admin = new Caller(UUID.randomUUID(), true);

        limiter.consumeUpload(admin);
        limiter.consumeAnalysis(admin);
        limiter.consumeAssistantAsk(admin);

        UsageLimiter.PerUserLimitSnapshot snapshot = limiter.snapshot(admin);
        assertThat(snapshot.uploadsPerDay()).isNull();
        assertThat(snapshot.analysesPerDay()).isNull();
        assertThat(snapshot.assistantAsksPerHour()).isNull();
    }

    @Test
    void snapshotIsNullWhenThereIsNoIdentity() {
        properties.setUploadsPerUserPerDay(20);
        properties.setAnalysesPerUserPerDay(5);
        properties.setAssistantAsksPerUserPerHour(30);

        assertUnlimited(limiter.snapshot(null));
        assertUnlimited(limiter.snapshot(Caller.anonymous()));
        assertUnlimited(limiter.snapshot(Caller.unidentifiedAdmin()));
    }

    @Test
    void staleKeysAreEvictedSoTheTrackedSetStaysBoundedByTheOpenWindow() {
        properties.setUploadsPerUserPerDay(2);
        properties.setAssistantAsksPerUserPerHour(2);
        properties.setGlobalAnalysesPerDay(0);

        for (int i = 0; i < 40; i++) {
            Caller caller = user();
            limiter.consumeUpload(caller);
            limiter.consumeAssistantAsk(caller);
        }
        assertThat(limiter.trackedKeyCount()).isEqualTo(80);

        clock.advance(Duration.ofHours(25));
        limiter.consumeUpload(user());
        assertThat(limiter.trackedKeyCount()).isEqualTo(1);
    }

    private UsageLimitExceededException catchAnalysis(Caller caller) {
        try {
            limiter.consumeAnalysis(caller);
        } catch (UsageLimitExceededException ex) {
            return ex;
        }
        throw new AssertionError("expected the analysis limit to reject the call");
    }

    private static Caller user() {
        return new Caller(UUID.randomUUID(), false);
    }

    private static void assertWindow(UsageLimiter.LimitWindow window, int limit, int used, long resetsInSeconds) {
        assertThat(window).isNotNull();
        assertThat(window.limit()).isEqualTo(limit);
        assertThat(window.used()).isEqualTo(used);
        assertThat(window.resetsInSeconds()).isEqualTo(resetsInSeconds);
    }

    private static void assertUnlimited(UsageLimiter.PerUserLimitSnapshot snapshot) {
        assertThat(snapshot.uploadsPerDay()).isNull();
        assertThat(snapshot.analysesPerDay()).isNull();
        assertThat(snapshot.assistantAsksPerHour()).isNull();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
