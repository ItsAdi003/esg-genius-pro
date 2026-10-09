package dev.esgenius.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Validates Supabase access tokens for /api/** by calling the Auth user endpoint.
 * A 200 response must include a user id; that id and email are exposed as the
 * {@code authenticatedUser} request attribute. Successful checks are cached by a
 * SHA-256 of the token so later requests skip the remote call until the TTL.
 * When Supabase is not configured and {@code app.auth.supabase.required} is true,
 * API requests are rejected with 401. Health checks and CORS preflight are still skipped.
 * When required is false, an unconfigured filter no-ops (local-dev opt-out) and sets no identity.
 * Not registered under the test profile.
 */
public class SupabaseAuthFilter extends OncePerRequestFilter {

    static final String HEALTH_PATH = "/api/v1/health";

    private static final Logger log = LoggerFactory.getLogger(SupabaseAuthFilter.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final SupabaseAuthProperties properties;
    private final HttpClient httpClient;
    private final SupabaseTokenValidationCache validationCache;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupabaseAuthFilter(SupabaseAuthProperties properties) {
        this(properties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    SupabaseAuthFilter(SupabaseAuthProperties properties, HttpClient httpClient) {
        this(properties, httpClient, Clock.systemUTC());
    }

    SupabaseAuthFilter(SupabaseAuthProperties properties, HttpClient httpClient, Clock clock) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.validationCache = new SupabaseTokenValidationCache(
                properties.getTokenCacheTtl(),
                SupabaseTokenValidationCache.MAX_ENTRIES,
                clock);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (HEALTH_PATH.equals(requestPath(request))) {
            return true;
        }
        if (!properties.isConfigured()) {
            return !properties.isRequired();
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isConfigured()) {
            writeUnauthorized(response, "Authentication is not configured");
            return;
        }

        String token = bearerToken(request.getHeader("Authorization"));
        if (token == null) {
            writeUnauthorized(response, "Authentication required");
            return;
        }

        final Optional<AuthenticatedUser> user;
        try {
            user = resolveUser(token);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Supabase token validation was interrupted");
            writeUnauthorized(response, "Unable to validate access token");
            return;
        } catch (IOException ex) {
            log.warn("Supabase token validation failed: {}", ex.getMessage());
            writeUnauthorized(response, "Unable to validate access token");
            return;
        }

        if (user.isEmpty()) {
            writeUnauthorized(response, "Invalid or expired access token");
            return;
        }

        request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, user.get());
        filterChain.doFilter(request, response);
    }

    /**
     * Digest keys currently held. Package-visible so tests can assert the raw token is absent.
     */
    Set<String> validationCacheKeys() {
        return validationCache.keys();
    }

    private Optional<AuthenticatedUser> resolveUser(String accessToken) throws IOException, InterruptedException {
        String cacheKey = SupabaseTokenValidationCache.keyFor(accessToken);
        Optional<AuthenticatedUser> cached = validationCache.get(cacheKey);
        if (cached.isPresent()) {
            log.debug("Supabase access token accepted for user {}", cached.get().userId());
            return cached;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(userEndpoint())
                .timeout(REQUEST_TIMEOUT)
                .header("apikey", properties.getAnonKey())
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            log.debug("Supabase access token rejected with status {}", response.statusCode());
            return Optional.empty();
        }

        Optional<AuthenticatedUser> user = parseUser(response.body());
        if (user.isEmpty()) {
            log.debug("Supabase access token rejected");
            return Optional.empty();
        }
        validationCache.put(cacheKey, user.get());
        log.debug("Supabase access token accepted for user {}", user.get().userId());
        return user;
    }

    private Optional<AuthenticatedUser> parseUser(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        final JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JsonProcessingException ex) {
            return Optional.empty();
        }
        if (root == null || !root.isObject()) {
            return Optional.empty();
        }
        JsonNode idNode = root.get("id");
        if (idNode == null || !idNode.isTextual()) {
            return Optional.empty();
        }
        final UUID userId;
        try {
            userId = UUID.fromString(idNode.asText().trim());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        String email = null;
        JsonNode emailNode = root.get("email");
        if (emailNode != null && emailNode.isTextual()) {
            email = emailNode.asText();
        }
        return Optional.of(new AuthenticatedUser(userId, email));
    }

    private URI userEndpoint() {
        String base = properties.getUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return URI.create(base + "/auth/v1/user");
    }

    private static String bearerToken(String authorization) {
        if (authorization == null) {
            return null;
        }
        String prefix = "Bearer ";
        if (authorization.length() <= prefix.length()
                || !authorization.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }
        String token = authorization.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
