package com.amarkatha.engagement;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amarkatha.mail")
public record AmarKathaMailProperties(
        boolean enabled,
        String from
) {
    public AmarKathaMailProperties {
        if (from == null || from.isBlank()) {
            from = "noreply@amarkatha.in";
        }
    }
}
