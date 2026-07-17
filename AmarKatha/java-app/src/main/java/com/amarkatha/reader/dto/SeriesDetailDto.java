package com.amarkatha.reader.dto;

import java.time.Instant;
import java.util.List;

public record SeriesDetailDto(
        String slug,
        String title,
        String creatorName,
        String description,
        List<String> genres,
        String contentLanguage,
        String coverGradient,
        String coverUrl,
        String scheduleLabel,
        ScheduleStripDto schedule,
        String status,
        Instant lastUpdatedAt,
        int chapterCount,
        List<ChapterSummaryDto> chapters
) {
}
