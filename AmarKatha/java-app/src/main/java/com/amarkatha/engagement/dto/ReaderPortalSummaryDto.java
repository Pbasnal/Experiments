package com.amarkatha.engagement.dto;

import java.util.List;

public record ReaderPortalSummaryDto(
        String displayName,
        String email,
        String role,
        long followingCount,
        List<ReaderProgressDto> continueReading,
        List<FollowedSeriesDto> following
) {
}
