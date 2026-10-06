package com.amarkatha.reader.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GlimpseDto(
        UUID id,
        String tag,
        Instant postedAt,
        List<GlimpseImageDto> images
) {
}
