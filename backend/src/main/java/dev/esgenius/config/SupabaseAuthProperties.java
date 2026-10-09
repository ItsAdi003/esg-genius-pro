package dev.esgenius.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.supabase")
public class SupabaseAuthProperties {

    private String url = "";
    private String anonKey = "";
    /**
     * When true (the default), missing Supabase settings fail closed with 401.
     * Set {@code AUTH_REQUIRED=false} only for local runs without Supabase.
     */
    private boolean required = true;

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

    public boolean isConfigured() {
        return url != null && !url.isBlank() && anonKey != null && !anonKey.isBlank();
    }
}
