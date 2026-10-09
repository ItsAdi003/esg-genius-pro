package dev.esgenius.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.supabase")
public class SupabaseAuthProperties {

    static final Duration DEFAULT_TOKEN_CACHE_TTL = Duration.ofSeconds(60);

    private String url = "";
    private String anonKey = "";
    /**
     * When true (the default), missing Supabase settings fail closed with 401.
     * Set {@code AUTH_REQUIRED=false} only for local runs without Supabase.
     */
    private boolean required = true;
    /**
     * How long a successful {@code /auth/v1/user} check is reused.
     * Failures are never cached. Zero disables reuse.
     */
    private Duration tokenCacheTtl = DEFAULT_TOKEN_CACHE_TTL;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAnonKey() {
        return anonKey;
    }

    public void setAnonKey(String anonKey) {
        this.anonKey = anonKey;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Duration getTokenCacheTtl() {
        if (tokenCacheTtl == null || tokenCacheTtl.isNegative()) {
            return DEFAULT_TOKEN_CACHE_TTL;
        }
        return tokenCacheTtl;
    }

    public void setTokenCacheTtl(Duration tokenCacheTtl) {
        this.tokenCacheTtl = tokenCacheTtl;
    }

    public boolean isConfigured() {
        return url != null && !url.isBlank() && anonKey != null && !anonKey.isBlank();
    }
}
