package com.amarkatha.creator;

import java.util.List;
import java.util.UUID;

public record CreatorGlimpseView(
        UUID id,
        String tagLabel,
        String postedLabel,
        List<String> thumbnailUrls
) {
}
