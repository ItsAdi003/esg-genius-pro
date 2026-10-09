package dev.esgenius.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Binds {@code app.auth.admin-user-ids} and makes Supabase auth settings available
 * in every profile, including tests where the auth filter is not registered.
 */
@Configuration
@EnableConfigurationProperties({AuthProperties.class, SupabaseAuthProperties.class})
class AuthPropertiesConfiguration {
}

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String adminUserIds = "";

    private Set<UUID> adminUserIdSet = Set.of();

    @PostConstruct
    void parseAdminUserIds() {
        if (adminUserIds == null || adminUserIds.isBlank()) {
            adminUserIdSet = Set.of();
            return;
        }
        Set<UUID> parsed = new LinkedHashSet<>();
        for (String part : adminUserIds.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                parsed.add(UUID.fromString(trimmed));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "app.auth.admin-user-ids contains an invalid UUID: " + trimmed, ex);
            }
        }
        adminUserIdSet = Collections.unmodifiableSet(parsed);
    }

    public String getAdminUserIds() {
        return adminUserIds;
    }

    public void setAdminUserIds(String adminUserIds) {
        this.adminUserIds = adminUserIds;
    }

    public boolean isAdmin(UUID userId) {
        return userId != null && adminUserIdSet.contains(userId);
    }
}
