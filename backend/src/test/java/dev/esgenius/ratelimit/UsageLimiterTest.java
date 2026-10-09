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
