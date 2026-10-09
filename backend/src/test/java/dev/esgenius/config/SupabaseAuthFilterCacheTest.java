package dev.esgenius.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class SupabaseAuthFilterCacheTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String EMAIL = "ada@example.com";
    private static final String TOKEN = "access-token-one";
    private static final String USER_JSON =
            "{\"id\":\"22222222-2222-2222-2222-222222222222\",\"email\":\"ada@example.com\"}";
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final Instant START = Instant.parse("2026-10-09T12:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validTokenSetsAuthenticatedUser() throws Exception {
        FakeHttpClient client = FakeHttpClient.fixed(200, USER_JSON);
        SupabaseAuthFilter filter = configuredFilter(client, new AdjustableClock(START));

        boolean[] continued = {false};
        MockHttpServletRequest request = authorized(TOKEN);
        MockHttpServletResponse response = invoke(filter, request, continued);

        assertThat(continued[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(AuthenticatedUser.from(request)).contains(new AuthenticatedUser(USER_ID, EMAIL));
        assertThat(client.calls).hasValue(1);
        assertThat(client.authorizations).containsExactly("Bearer " + TOKEN);
    }

    @Test
    void okBodyWithoutIdIsUnauthorizedAndNotCached() throws Exception {
        FakeHttpClient client = FakeHttpClient.fixed(200, "{\"email\":\"ada@example.com\"}");
        SupabaseAuthFilter filter = configuredFilter(client, new AdjustableClock(START));

        boolean[] firstContinued = {false};
        MockHttpServletRequest first = authorized(TOKEN);
        MockHttpServletResponse firstResponse = invoke(filter, first, firstContinued);
        boolean[] secondContinued = {false};
        invoke(filter, authorized(TOKEN), secondContinued);

        assertThat(firstContinued[0]).isFalse();
        assertThat(secondContinued[0]).isFalse();
        assertUnauthorized(firstResponse, "Invalid or expired access token");
        assertThat(AuthenticatedUser.from(first)).isEmpty();
        assertThat(client.calls).hasValue(2);
        assertThat(filter.validationCacheKeys()).isEmpty();
    }

    @Test
    void nonJsonOkBodyIsUnauthorized() throws Exception {
        FakeHttpClient client = FakeHttpClient.fixed(200, "not-json");
        SupabaseAuthFilter filter = configuredFilter(client, new AdjustableClock(START));

        boolean[] continued = {false};
        MockHttpServletRequest request = authorized(TOKEN);
        MockHttpServletResponse response = invoke(filter, request, continued);

        assertThat(continued[0]).isFalse();
        assertUnauthorized(response, "Invalid or expired access token");
        assertThat(AuthenticatedUser.from(request)).isEmpty();
        assertThat(client.calls).hasValue(1);
        assertThat(filter.validationCacheKeys()).isEmpty();
    }

    @Test
    void secondRequestWithSameTokenWithinTtlCallsHttpClientOnce() throws Exception {
        FakeHttpClient client = FakeHttpClient.fixed(200, USER_JSON);
        AdjustableClock clock = new AdjustableClock(START);
        SupabaseAuthFilter filter = configuredFilter(client, clock);

        MockHttpServletRequest first = authorized(TOKEN);
        MockHttpServletRequest second = authorized(TOKEN);
        invoke(filter, first, new boolean[1]);
        clock.advance(TTL.minusSeconds(1));
        invoke(filter, second, new boolean[1]);

        assertThat(client.calls).hasValue(1);
        assertThat(AuthenticatedUser.from(first)).contains(new AuthenticatedUser(USER_ID, EMAIL));
        assertThat(AuthenticatedUser.from(second)).contains(new AuthenticatedUser(USER_ID, EMAIL));
    }

    @Test
    void requestAfterTtlCallsHttpClientAgain() throws Exception {
        FakeHttpClient client = FakeHttpClient.fixed(200, USER_JSON);
        AdjustableClock clock = new AdjustableClock(START);
        SupabaseAuthFilter filter = configuredFilter(client, clock);

        invoke(filter, authorized(TOKEN), new boolean[1]);
        clock.advance(TTL);
        invoke(filter, authorized(TOKEN), new boolean[1]);

        assertThat(client.calls).hasValue(2);
    }

    @Test
    void failedValidationIsNotCached() throws Exception {
        FakeHttpClient client = FakeHttpClient.fixed(401, "{}");
        SupabaseAuthFilter filter = configuredFilter(client, new AdjustableClock(START));

        boolean[] firstContinued = {false};
        MockHttpServletResponse first = invoke(filter, authorized(TOKEN), firstContinued);
        boolean[] secondContinued = {false};
        invoke(filter, authorized(TOKEN), secondContinued);

        assertThat(firstContinued[0]).isFalse();
        assertThat(secondContinued[0]).isFalse();
        assertUnauthorized(first, "Invalid or expired access token");
        assertThat(client.calls).hasValue(2);
        assertThat(filter.validationCacheKeys()).isEmpty();
    }

    @Test
    void differentTokensAreValidatedSeparatelyAndCacheKeysAreDigests() throws Exception {
        String tokenA = "token-a";
        String tokenB = "token-b";
        UUID idA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID idB = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        FakeHttpClient client = new FakeHttpClient(authorization -> {
            if (authorization.equals("Bearer " + tokenA)) {
                return new Reply(200, userJson(idA, "a@example.com"));
            }
            if (authorization.equals("Bearer " + tokenB)) {
                return new Reply(200, userJson(idB, "b@example.com"));
            }
            return new Reply(401, "{}");
        });
        SupabaseAuthFilter filter = configuredFilter(client, new AdjustableClock(START));

        MockHttpServletRequest requestA = authorized(tokenA);
        MockHttpServletRequest requestB = authorized(tokenB);
        invoke(filter, requestA, new boolean[1]);
        invoke(filter, requestB, new boolean[1]);
        invoke(filter, authorized(tokenA), new boolean[1]);

        assertThat(client.calls).hasValue(2);
        assertThat(AuthenticatedUser.from(requestA)).contains(new AuthenticatedUser(idA, "a@example.com"));
        assertThat(AuthenticatedUser.from(requestB)).contains(new AuthenticatedUser(idB, "b@example.com"));
        assertThat(filter.validationCacheKeys())
                .containsExactlyInAnyOrder(
                        SupabaseTokenValidationCache.keyFor(tokenA),
                        SupabaseTokenValidationCache.keyFor(tokenB))
                .doesNotContain(tokenA, tokenB);
    }

    @Test
    void disabledAuthSetsNoAuthenticatedUser() throws Exception {
        FakeHttpClient client = FakeHttpClient.fixed(200, USER_JSON);
        SupabaseAuthProperties properties = new SupabaseAuthProperties();
        properties.setRequired(false);
        SupabaseAuthFilter filter = new SupabaseAuthFilter(properties, client);

        boolean[] continued = {false};
        MockHttpServletRequest request = authorized(TOKEN);
        MockHttpServletResponse response = invoke(filter, request, continued);

        assertThat(continued[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(AuthenticatedUser.from(request)).isEmpty();
        assertThat(client.calls).hasValue(0);
    }

    @Test
    void cacheEvictsExpiredAndOldestEntries() {
        AdjustableClock clock = new AdjustableClock(START);
        SupabaseTokenValidationCache cache = new SupabaseTokenValidationCache(TTL, 2, clock);
        AuthenticatedUser first = new AuthenticatedUser(USER_ID, EMAIL);
        AuthenticatedUser second = new AuthenticatedUser(
                UUID.fromString("33333333-3333-3333-3333-333333333333"), "b@example.com");
        AuthenticatedUser third = new AuthenticatedUser(
                UUID.fromString("44444444-4444-4444-4444-444444444444"), "c@example.com");
        String keyA = SupabaseTokenValidationCache.keyFor("token-a");
        String keyB = SupabaseTokenValidationCache.keyFor("token-b");
        String keyC = SupabaseTokenValidationCache.keyFor("token-c");

        cache.put(keyA, first);
        cache.put(keyB, second);
        cache.put(keyC, third);

        assertThat(cache.keys()).containsExactlyInAnyOrder(keyB, keyC);
        assertThat(cache.get(keyA)).isEmpty();
        assertThat(cache.get(keyC)).contains(third);

        clock.advance(TTL);
        assertThat(cache.get(keyB)).isEmpty();
        assertThat(cache.keys()).contains(keyC);
    }

    private static SupabaseAuthFilter configuredFilter(HttpClient client, Clock clock) {
        SupabaseAuthProperties properties = new SupabaseAuthProperties();
        properties.setUrl("http://127.0.0.1:9");
        properties.setAnonKey("anon-test-key");
        properties.setTokenCacheTtl(TTL);
        return new SupabaseAuthFilter(properties, client, clock);
    }

    private static String userJson(UUID id, String email) {
        return "{\"id\":\"" + id + "\",\"email\":\"" + email + "\"}";
    }

    private static MockHttpServletRequest authorized(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/documents");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static MockHttpServletResponse invoke(
            SupabaseAuthFilter filter,
            MockHttpServletRequest request,
            boolean[] continued) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> continued[0] = true;
        filter.doFilter(request, response, chain);
        return response;
    }

    private void assertUnauthorized(MockHttpServletResponse response, String message) throws Exception {
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asText()).isEqualTo("Unauthorized");
        assertThat(body.get("message").asText()).isEqualTo(message);
        assertThat(body.get("timestamp").asText()).isNotBlank();
    }

    private record Reply(int status, String body) {
    }

    /**
     * HttpClient stand-in. {@code send} records the call and returns the scripted status and body.
     * The body handler is not applied; the filter reads {@link HttpResponse#body()} directly.
     */
    private static final class FakeHttpClient extends HttpClient {
        private final AtomicInteger calls = new AtomicInteger();
        private final List<String> authorizations = new CopyOnWriteArrayList<>();
        private final Function<String, Reply> replies;

        private FakeHttpClient(Function<String, Reply> replies) {
            this.replies = replies;
        }

        private static FakeHttpClient fixed(int status, String body) {
            return new FakeHttpClient(authorization -> new Reply(status, body));
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            calls.incrementAndGet();
            String authorization = request.headers().firstValue("Authorization").orElse("");
            authorizations.add(authorization);
            Reply reply = replies.apply(authorization);
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) new FixedResponse(request, reply.status, reply.body);
            return response;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }
    }

    private static final class FixedResponse implements HttpResponse<String> {
        private final HttpRequest request;
        private final int status;
        private final String body;

        private FixedResponse(HttpRequest request, int status, String body) {
            this.request = request;
            this.status = status;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return status;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }

    private static final class AdjustableClock extends Clock {
        private Instant now;

        private AdjustableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
