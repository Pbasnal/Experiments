package com.amarkatha.creator;

/**
 * Per-series summary for creator home and series list.
 */
public record CreatorHomeSeriesView(
        java.util.UUID id,
        String title,
        String slug,
        String status,
        String contentLanguage,
        String coverUrl,
        String coverGradient,
        String description,
        String nextSlotLabel,
        String scheduleSummary,
        long draftCount,
        boolean needsSchedulePrompt,
        boolean onHiatus
) {
}
