package com.amarkatha.engagement.dto;

import java.time.Instant;

public record NotificationPreferenceDto(
        boolean emailNewChapter,
        boolean inAppNewChapter,
        boolean emailProductUpdates,
        Instant updatedAt
) {
}
