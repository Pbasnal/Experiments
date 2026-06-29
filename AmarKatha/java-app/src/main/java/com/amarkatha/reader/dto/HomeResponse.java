package com.amarkatha.reader.dto;

import java.util.List;

public record HomeResponse(
        String tagline,
        List<SeriesCardDto> recentlyUpdated,
        List<PlatformRouteDto> platformRoutes
) {
}
