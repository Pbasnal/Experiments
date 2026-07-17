package com.amarkatha.creator;

/**
 * Per-series summary for creator home: next slot + draft warnings.
 */
public record CreatorHomeSeriesView(
        java.util.UUID id,
        String title,
        String slug,
        String status,
        String contentLanguage,
        String nextSlotLabel,
        String scheduleSummary,
        long draftCount,
        boolean needsSchedulePrompt,
        boolean onHiatus
) {
}
