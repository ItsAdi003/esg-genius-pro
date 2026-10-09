package dev.esgenius.ratelimit;

import dev.esgenius.service.Caller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * In-memory usage limits for a single application instance.
 * Counts live only in this JVM and reset when the process restarts.
 * <p>
 * Windows are rolling, not calendar buckets. Each check drops timestamps that
 * have left their window and removes keys with nothing left, so memory stays
 * bounded by identities that are still inside a window.
 */
@Component
public class UsageLimiter {

    private static final Duration ONE_DAY = Duration.ofHours(24);
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final String GLOBAL_KEY = "global";

    private final UsageLimitProperties properties;
    private final Clock clock;
    private final Object lock = new Object();

    private final Bucket uploads = new Bucket(ONE_DAY);
    private final Bucket analysesPerUser = new Bucket(ONE_DAY);
    private final Bucket assistantAsks = new Bucket(ONE_HOUR);
    private final Bucket globalAnalyses = new Bucket(ONE_DAY);

    public UsageLimiter(UsageLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Autowired
    public UsageLimiter(UsageLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public void consumeUpload(Caller caller) {
        consumePerUser(caller, uploads, properties.getUploadsPerUserPerDay(), "uploads per day");
    }

    /**
     * Per-user analysis limit, then the global cap. Admins skip the per-user
     * limit and are still counted in the global cap. Neither counter moves
     * when the request is rejected.
     */
    public void consumeAnalysis(Caller caller) {
        if (!identified(caller)) {
            return;
        }
        synchronized (lock) {
            Instant now = clock.instant();
            pruneAll(now);
            UsageLimitExceededException perUser = null;
            if (!caller.admin()) {
                perUser = rejection(
                        analysesPerUser,
                        caller.userId().toString(),
                        properties.getAnalysesPerUserPerDay(),
                        "analyses per day",
                        now);
            }
            UsageLimitExceededException global = rejection(
                    globalAnalyses,
                    GLOBAL_KEY,
                    properties.getGlobalAnalysesPerDay(),
                    "analyses per day",
                    now);
            if (perUser != null) {
                throw perUser;
            }
            if (global != null) {
                throw global;
            }
            if (!caller.admin() && properties.getAnalysesPerUserPerDay() > 0) {
                record(analysesPerUser, caller.userId().toString(), now);
            }
            if (properties.getGlobalAnalysesPerDay() > 0) {
                record(globalAnalyses, GLOBAL_KEY, now);
            }
        }
    }

    public void consumeAssistantAsk(Caller caller) {
        consumePerUser(
                caller,
                assistantAsks,
                properties.getAssistantAsksPerUserPerHour(),
                "assistant questions per hour");
    }

    /**
     * Read-only view of this caller's per-user windows. Does not consume budget.
     * A window is {@code null} when that limit is disabled or the caller is not
     * subject to per-user limits (admin or no identity). Does not include the
     * global analysis cap.
     */
    public PerUserLimitSnapshot snapshot(Caller caller) {
        synchronized (lock) {
            Instant now = clock.instant();
            pruneAll(now);
            if (!identified(caller) || caller.admin()) {
                return PerUserLimitSnapshot.unlimited();
            }
            String key = caller.userId().toString();
            return new PerUserLimitSnapshot(
                    windowUsage(uploads, key, properties.getUploadsPerUserPerDay(), now),
                    windowUsage(analysesPerUser, key, properties.getAnalysesPerUserPerDay(), now),
                    windowUsage(assistantAsks, key, properties.getAssistantAsksPerUserPerHour(), now));
        }
    }

    public record PerUserLimitSnapshot(
            LimitWindow uploadsPerDay,
            LimitWindow analysesPerDay,
            LimitWindow assistantAsksPerHour) {
        static PerUserLimitSnapshot unlimited() {
            return new PerUserLimitSnapshot(null, null, null);
        }
    }

    public record LimitWindow(int limit, int used, long resetsInSeconds) {
    }

    /** Keys that still have at least one event inside its window. For tests. */
    int trackedKeyCount() {
        synchronized (lock) {
            pruneAll(clock.instant());
            return uploads.events.size()
                    + analysesPerUser.events.size()
                    + assistantAsks.events.size()
                    + globalAnalyses.events.size();
        }
    }

    private void consumePerUser(Caller caller, Bucket bucket, int limit, String label) {
        if (!identified(caller) || caller.admin() || limit <= 0) {
            return;
        }
        synchronized (lock) {
            Instant now = clock.instant();
            pruneAll(now);
            UsageLimitExceededException hit = rejection(bucket, caller.userId().toString(), limit, label, now);
            if (hit != null) {
                throw hit;
            }
            record(bucket, caller.userId().toString(), now);
        }
    }

    private static boolean identified(Caller caller) {
        return caller != null && caller.userId() != null;
    }

    private static LimitWindow windowUsage(Bucket bucket, String key, int limit, Instant now) {
        if (limit <= 0) {
            return null;
        }
        ArrayDeque<Instant> events = bucket.events.get(key);
        int used = events == null ? 0 : events.size();
        long resetsInSeconds = 0;
        if (used > 0) {
            Instant expiresAt = events.peekFirst().plus(bucket.window);
            long remaining = Duration.between(now, expiresAt).getSeconds();
            resetsInSeconds = Math.max(remaining, 0);
        }
        return new LimitWindow(limit, used, resetsInSeconds);
    }

    private UsageLimitExceededException rejection(
            Bucket bucket, String key, int limit, String label, Instant now) {
        if (limit <= 0) {
            return null;
        }
        ArrayDeque<Instant> events = bucket.events.get(key);
        if (events == null || events.size() < limit) {
            return null;
        }
        long retryAfterSeconds = retryAfterSeconds(events.peekFirst().plus(bucket.window), now);
        return new UsageLimitExceededException(
                "You have reached the limit of " + limit + " " + label
                        + ". Try again in " + formatWait(retryAfterSeconds) + ".",
                retryAfterSeconds);
    }

    private static void record(Bucket bucket, String key, Instant now) {
        bucket.events.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(now);
    }

    private void pruneAll(Instant now) {
        prune(uploads, now);
        prune(analysesPerUser, now);
        prune(assistantAsks, now);
        prune(globalAnalyses, now);
    }

    private static void prune(Bucket bucket, Instant now) {
        Instant cutoff = now.minus(bucket.window);
        Iterator<Map.Entry<String, ArrayDeque<Instant>>> keys = bucket.events.entrySet().iterator();
        while (keys.hasNext()) {
            ArrayDeque<Instant> events = keys.next().getValue();
            while (!events.isEmpty() && !events.peekFirst().isAfter(cutoff)) {
                events.removeFirst();
            }
            if (events.isEmpty()) {
                keys.remove();
            }
        }
    }

    static long retryAfterSeconds(Instant availableAt, Instant now) {
        long millis = Duration.between(now, availableAt).toMillis();
        if (millis <= 0) {
            return 1;
        }
        return (millis + 999) / 1000;
    }

    static String formatWait(long totalSeconds) {
        long seconds = Math.max(totalSeconds, 0);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder text = new StringBuilder();
        if (hours > 0) {
            text.append(hours).append('h');
        }
        if (minutes > 0) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(minutes).append('m');
        }
        if (secs > 0 || text.isEmpty()) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(secs).append('s');
        }
        return text.toString();
    }

    private static final class Bucket {
        private final Duration window;
        private final Map<String, ArrayDeque<Instant>> events = new HashMap<>();

        private Bucket(Duration window) {
            this.window = window;
        }
    }
}
