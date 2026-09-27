package dev.esgenius.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.supabase")
public class SupabaseAuthProperties {

    private String url = "";
    private String anonKey = "";

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

    public boolean isConfigured() {
        return url != null && !url.isBlank() && anonKey != null && !anonKey.isBlank();
    }
}
