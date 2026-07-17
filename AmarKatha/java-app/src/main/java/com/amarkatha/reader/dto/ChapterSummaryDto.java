package com.amarkatha.reader.dto;

import java.time.Instant;

public record ChapterSummaryDto(
        String slug,
        String title,
        double chapterNumber,
        Instant listedAt
) {
}
