package com.amarkatha.reader.dto;

import java.time.Instant;

/**
 * Schedule strip payload. {@code nextExpectedAt} is UTC Instant — format in the viewer's local timezone.
 */
public record ScheduleStripDto(
        String headline,
        String scheduleLabel,
        Instant nextExpectedAt,
        String skipMessage,
        String status,
        String cadence,
        Integer periodDays,
        Integer releaseHourIst
) {
}
