package dev.esgenius.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsFilterOrderTest {

    private static final String ORIGIN = "http://localhost:8080";

    @Test
    void corsFilterRunsBeforeAuthFilter() {
        assertThat(corsRegistration().getOrder()).isLessThan(authRegistration(new SupabaseAuthProperties()).getOrder());
    }

    @Test
    void unauthorizedResponseStillCarriesCorsHeaders() throws Exception {
        // Fail-closed auth (Supabase unset, required) answers 401 to every non-health /api request.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/frameworks");
        request.setServletPath("/api/v1/frameworks");
        request.addHeader("Origin", ORIGIN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        List<FilterRegistrationBean<?>> registrations = new ArrayList<>();
        registrations.add(authRegistration(new SupabaseAuthProperties()));
        registrations.add(corsRegistration());
        registrations.sort(Comparator.comparingInt(FilterRegistrationBean::getOrder));
        Filter[] filters = registrations.stream().map(r -> (Filter) r.getFilter()).toArray(Filter[]::new);
        new MockFilterChain(new HttpServlet() {}, filters).doFilter(request, response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(ORIGIN);
    }

    private static FilterRegistrationBean<?> corsRegistration() {
        return new CorsConfig().corsFilter(ORIGIN);
    }

    private static FilterRegistrationBean<?> authRegistration(SupabaseAuthProperties properties) {
        return new SupabaseAuthConfig().supabaseAuthFilter(properties);
    }
}
