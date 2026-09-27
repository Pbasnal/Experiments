package com.amarkatha.engagement.dto;

import java.time.Instant;
import java.util.UUID;

public record ReaderNotificationDto(
        UUID id,
        String type,
        String seriesSlug,
        String seriesTitle,
        String chapterSlug,
        String chapterTitle,
        String title,
        String message,
        String href,
        Instant readAt,
        Instant createdAt
) {
}
