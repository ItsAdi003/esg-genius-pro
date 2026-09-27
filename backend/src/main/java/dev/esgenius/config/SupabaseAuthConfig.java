package dev.esgenius.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

/**
 * Registers {@link SupabaseAuthFilter} for /api/** outside the test profile.
 * The filter itself no-ops when SUPABASE_URL or SUPABASE_ANON_KEY is blank.
 */
@Configuration
@Profile("!test")
@EnableConfigurationProperties(SupabaseAuthProperties.class)
public class SupabaseAuthConfig {

    @Bean
    public FilterRegistrationBean<SupabaseAuthFilter> supabaseAuthFilter(SupabaseAuthProperties properties) {
        FilterRegistrationBean<SupabaseAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SupabaseAuthFilter(properties));
        registration.addUrlPatterns("/api/*");
        registration.setName("supabaseAuthFilter");
        // After CorsFilter (HIGHEST_PRECEDENCE) so preflight is answered with CORS headers first.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
