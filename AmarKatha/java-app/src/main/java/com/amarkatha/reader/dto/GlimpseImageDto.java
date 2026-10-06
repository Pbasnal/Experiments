package com.amarkatha.reader.dto;

import java.util.UUID;

public record GlimpseImageDto(
        UUID id,
        String url,
        int sortOrder,
        long reactionCount,
        boolean reacted
) {
}
