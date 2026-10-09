package dev.esgenius.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

/**
 * Registers {@link SupabaseAuthFilter} for /api/** outside the test profile.
 * Missing Supabase settings fail closed unless {@code AUTH_REQUIRED=false}.
 */
@Configuration
@Profile("!test")
@EnableConfigurationProperties(SupabaseAuthProperties.class)
public class SupabaseAuthConfig {

    private static final Logger log = LoggerFactory.getLogger(SupabaseAuthConfig.class);

    @Bean
    public FilterRegistrationBean<SupabaseAuthFilter> supabaseAuthFilter(SupabaseAuthProperties properties) {
        logAuthStartup(properties);
        FilterRegistrationBean<SupabaseAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SupabaseAuthFilter(properties));
        registration.addUrlPatterns("/api/*");
        registration.setName("supabaseAuthFilter");
        // After CorsFilter (HIGHEST_PRECEDENCE) so preflight is answered with CORS headers first.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    private static void logAuthStartup(SupabaseAuthProperties properties) {
        if (properties.isConfigured()) {
            return;
        }
        if (properties.isRequired()) {
            log.error(
                    "Supabase auth is required but SUPABASE_URL or SUPABASE_ANON_KEY is unset. "
                            + "/api/** requests will return 401 until both are set. "
                            + "/api/v1/health and CORS preflight stay available. "
                            + "Set AUTH_REQUIRED=false only for local development without Supabase.");
            return;
        }
        log.warn(
                "Supabase auth is DISABLED (AUTH_REQUIRED=false and Supabase is not configured). "
                        + "/api/** requests are not authenticated.");
    }
}
