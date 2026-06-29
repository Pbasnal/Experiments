package com.amarkatha.reader.dto;

import java.time.Instant;
import java.util.List;

public record SeriesCardDto(
        String slug,
        String title,
        String creatorName,
        String description,
        List<String> genres,
        String contentLanguage,
        String coverGradient,
        String scheduleLabel,
        String status,
        Instant lastUpdatedAt,
        int chapterCount
) {
}
