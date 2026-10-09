package dev.esgenius.config;

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
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates Supabase access tokens for /api/** by calling the Auth user endpoint.
 * When Supabase is not configured and {@code app.auth.supabase.required} is true,
 * API requests are rejected with 401. Health checks and CORS preflight are still skipped.
 * When required is false, an unconfigured filter no-ops (local-dev opt-out).
 * Not registered under the test profile.
 */
public class SupabaseAuthFilter extends OncePerRequestFilter {

    static final String HEALTH_PATH = "/api/v1/health";

    private static final Logger log = LoggerFactory.getLogger(SupabaseAuthFilter.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final SupabaseAuthProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupabaseAuthFilter(SupabaseAuthProperties properties) {
        this(properties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    SupabaseAuthFilter(SupabaseAuthProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
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

        try {
            if (!isAccessTokenValid(token)) {
                writeUnauthorized(response, "Invalid or expired access token");
                return;
            }
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

        filterChain.doFilter(request, response);
    }

    private boolean isAccessTokenValid(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(userEndpoint())
                .timeout(REQUEST_TIMEOUT)
                .header("apikey", properties.getAnonKey())
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode() == 200;
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
