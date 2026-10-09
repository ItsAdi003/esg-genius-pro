package dev.esgenius.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UsageLimitProperties.class)
class UsageLimitConfiguration {
}
