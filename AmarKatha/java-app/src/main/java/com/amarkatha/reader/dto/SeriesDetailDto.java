package com.amarkatha.reader.dto;

import java.util.List;

public record SeriesDetailDto(
        String slug,
        String title,
        String creatorName,
        String description,
        List<String> genres,
        String contentLanguage,
        String coverGradient,
        String scheduleLabel,
        String status,
        java.time.Instant lastUpdatedAt,
        int chapterCount,
        List<ChapterSummaryDto> chapters
) {
}
