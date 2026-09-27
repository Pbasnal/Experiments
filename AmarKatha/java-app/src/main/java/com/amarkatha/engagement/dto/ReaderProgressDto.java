package com.amarkatha.engagement.dto;

import java.time.Instant;

public record ReaderProgressDto(
        String seriesSlug,
        String seriesTitle,
        String coverGradient,
        String coverUrl,
        String chapterSlug,
        String chapterTitle,
        Double chapterNumber,
        Instant lastReadAt
) {
}
