package dev.esgenius.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory cache of successful Supabase token validations.
 * Keys are SHA-256 hex digests; the raw token is never stored.
 * Only successes are inserted. Expired entries and the oldest entries past
 * {@code maxEntries} are evicted.
 */
final class SupabaseTokenValidationCache {

    static final int MAX_ENTRIES = 1000;

    private final Duration ttl;
    private final int maxEntries;
    private final Clock clock;
    private final Map<String, CachedIdentity> entries = new LinkedHashMap<>();

    SupabaseTokenValidationCache(Duration ttl, int maxEntries, Clock clock) {
        if (ttl == null || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be zero or positive");
        }
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock is required");
        }
        this.ttl = ttl;
        this.maxEntries = maxEntries;
        this.clock = clock;
    }

    static String keyFor(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    synchronized Optional<AuthenticatedUser> get(String key) {
        CachedIdentity cached = entries.get(key);
        if (cached == null) {
            return Optional.empty();
        }
        if (!clock.instant().isBefore(cached.expiresAt())) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(cached.user());
    }

    synchronized void put(String key, AuthenticatedUser user) {
        requireDigestKey(key);
        Instant now = clock.instant();
        evictExpired(now);
        entries.remove(key);
        while (entries.size() >= maxEntries) {
            Iterator<String> oldest = entries.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
        entries.put(key, new CachedIdentity(user, now.plus(ttl)));
    }

    synchronized Set<String> keys() {
        return Set.copyOf(entries.keySet());
    }

    private void evictExpired(Instant now) {
        Iterator<Map.Entry<String, CachedIdentity>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            if (!now.isBefore(iterator.next().getValue().expiresAt())) {
                iterator.remove();
            }
        }
    }

    private static void requireDigestKey(String key) {
        if (key == null || key.length() != 64 || !isLowerHex(key)) {
            throw new IllegalArgumentException("cache key must be a lowercase SHA-256 hex digest");
        }
    }

    private static boolean isLowerHex(String key) {
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            boolean digit = c >= '0' && c <= '9';
            boolean lower = c >= 'a' && c <= 'f';
            if (!digit && !lower) {
                return false;
            }
        }
        return true;
    }

    private record CachedIdentity(AuthenticatedUser user, Instant expiresAt) {
    }
}
