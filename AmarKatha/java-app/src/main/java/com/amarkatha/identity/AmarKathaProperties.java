package com.amarkatha.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amarkatha")
public record AmarKathaProperties(
        String contactEmail,
        Admin admin
) {
    public record Admin(String bootstrapEmails) {
        public boolean isBootstrapAdmin(String email) {
            if (email == null || bootstrapEmails == null || bootstrapEmails.isBlank()) {
                return false;
            }
            String normalized = email.trim().toLowerCase();
            return java.util.Arrays.stream(bootstrapEmails.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toLowerCase)
                    .anyMatch(normalized::equals);
        }
    }
}
