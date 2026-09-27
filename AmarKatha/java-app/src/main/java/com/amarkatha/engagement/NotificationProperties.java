package com.amarkatha.engagement;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amarkatha.notifications")
public record NotificationProperties(
        long outboxPollIntervalMs,
        long emailPollIntervalMs,
        int maxAttempts,
        int batchSize,
        int listLimitDefault,
        int listLimitMax,
        long emailProcessingLeaseMs
) {
    public NotificationProperties {
        if (outboxPollIntervalMs <= 0) {
            outboxPollIntervalMs = 5_000L;
        }
        if (emailPollIntervalMs <= 0) {
            emailPollIntervalMs = 10_000L;
        }
        if (maxAttempts <= 0) {
            maxAttempts = 10;
        }
        if (batchSize <= 0) {
            batchSize = 50;
        }
        if (listLimitDefault <= 0) {
            listLimitDefault = 50;
        }
        if (listLimitMax <= 0) {
            listLimitMax = 100;
        }
        if (emailProcessingLeaseMs <= 0) {
            emailProcessingLeaseMs = 300_000L;
        }
    }
}
