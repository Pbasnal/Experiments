package com.amarkatha.engagement.dto;

import java.time.Instant;

public record FollowedSeriesDto(
        String slug,
        String title,
        String coverGradient,
        String coverUrl,
        String scheduleLabel,
        String status,
        String latestChapterSlug,
        String latestChapterTitle,
        Double latestChapterNumber,
        String lastReadChapterSlug,
        boolean hasUnread,
        Instant followedAt
) {
}
