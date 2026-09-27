package dev.esgenius.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SupabaseAuthFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicReference<String> apiKey = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private int statusCode = 200;

    @BeforeEach
    void startAuthServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/auth/v1/user", exchange -> {
            hits.incrementAndGet();
            apiKey.set(exchange.getRequestHeaders().getFirst("apikey"));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopAuthServer() {
        server.stop(0);
    }

    @Test
    void validBearerTokenCallsSupabaseUserEndpointAndContinues() throws Exception {
        boolean[] continued = {false};
        MockHttpServletResponse response = invoke(configuredFilter(), authorized("/api/v1/documents"), continued);

        assertThat(continued[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(hits.get()).isEqualTo(1);
        assertThat(apiKey.get()).isEqualTo("anon-test-key");
        assertThat(authorization.get()).isEqualTo("Bearer access-token");
    }

    @Test
    void missingAuthorizationReturnsUnauthorizedJsonWithoutCallingSupabase() throws Exception {
        boolean[] continued = {false};
        MockHttpServletResponse response = invoke(configuredFilter(), request("/api/v1/documents"), continued);

        assertThat(continued[0]).isFalse();
        assertThat(hits.get()).isZero();
        assertUnauthorized(response, "Authentication required");
    }

    @Test
    void rejectedTokenReturnsUnauthorizedJson() throws Exception {
        statusCode = 401;
        boolean[] continued = {false};
        MockHttpServletResponse response = invoke(configuredFilter(), authorized("/api/v1/documents"), continued);

        assertThat(continued[0]).isFalse();
        assertThat(hits.get()).isEqualTo(1);
        assertUnauthorized(response, "Invalid or expired access token");
    }

    @Test
    void unconfiguredFilterDoesNotCallSupabase() throws Exception {
        SupabaseAuthProperties properties = new SupabaseAuthProperties();
        boolean[] continued = {false};
        MockHttpServletResponse response = invoke(new SupabaseAuthFilter(properties), request("/api/v1/documents"), continued);

        assertThat(continued[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(hits.get()).isZero();
    }

    @Test
    void healthAndPreflightSkipValidation() throws Exception {
        SupabaseAuthFilter filter = configuredFilter();
        boolean[] healthContinued = {false};
        invoke(filter, request("/api/v1/health"), healthContinued);

        MockHttpServletRequest preflight = request("/api/v1/documents");
        preflight.setMethod("OPTIONS");
        boolean[] preflightContinued = {false};
        invoke(filter, preflight, preflightContinued);

        assertThat(healthContinued[0]).isTrue();
        assertThat(preflightContinued[0]).isTrue();
        assertThat(hits.get()).isZero();
    }

    private SupabaseAuthFilter configuredFilter() {
        SupabaseAuthProperties properties = new SupabaseAuthProperties();
        properties.setUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setAnonKey("anon-test-key");
        return new SupabaseAuthFilter(properties);
    }

    private static MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("GET", path);
    }

    private static MockHttpServletRequest authorized(String path) {
        MockHttpServletRequest request = request(path);
        request.addHeader("Authorization", "Bearer access-token");
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
}
