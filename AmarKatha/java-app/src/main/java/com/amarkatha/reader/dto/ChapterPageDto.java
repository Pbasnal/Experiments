package com.amarkatha.reader.dto;

public record ChapterPageDto(
        int sortOrder,
        String imageUrl,
        Integer width,
        Integer height
) {
}
