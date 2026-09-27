package com.amarkatha.analytics;

import java.util.Map;

/**
 * Allowlisted generic product / RUM event payload from the reader SPA.
 */
public record ProductEventRequest(
        String type,
        String seriesSlug,
        String chapterSlug,
        String referrer,
        Map<String, Object> meta
) {
}
