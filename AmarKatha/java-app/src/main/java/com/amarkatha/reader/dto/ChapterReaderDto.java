package com.amarkatha.reader.dto;

import java.util.List;

public record ChapterReaderDto(
        String seriesSlug,
        String seriesTitle,
        String chapterSlug,
        String title,
        double chapterNumber,
        List<ChapterPageDto> pages
) {
}
