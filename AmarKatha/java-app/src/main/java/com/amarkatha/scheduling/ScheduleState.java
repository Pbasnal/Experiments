package com.amarkatha.scheduling;

import com.amarkatha.shared.domain.SeriesCadence;
import com.amarkatha.shared.domain.SeriesStatus;
import java.time.Instant;

/**
 * Immutable schedule snapshot. Period is days; release hour is IST 0–23 (minutes always :00).
 */
public record ScheduleState(
        SeriesStatus status,
        SeriesCadence cadence,
        Integer periodDays,
        Short dayOfWeek,
        Short releaseHourIst,
        Instant nextExpectedAt,
        Instant lastPublishedAt,
        String skipMessage
) {
    public boolean hasActiveCadence() {
        return periodDays != null && periodDays > 0 && cadence != SeriesCadence.OFF;
    }

    public int releaseHourOrDefault() {
        return releaseHourIst != null
                ? releaseHourIst
                : ScheduleCalendar.DEFAULT_RELEASE_HOUR_IST;
    }
}
