package com.amarkatha.scheduling;

import com.amarkatha.shared.domain.SeriesCadence;
import com.amarkatha.shared.domain.SeriesStatus;
import java.time.Instant;

/**
 * Reader/creator-facing schedule strip fields. Dates are Instants — format in the viewer's local TZ.
 */
public record ScheduleStripView(
        String headline,
        String scheduleLabel,
        Instant nextExpectedAt,
        String skipMessage,
        SeriesStatus status,
        SeriesCadence cadence,
        Integer periodDays,
        Short releaseHourIst
) {
    public static ScheduleStripView from(ScheduleState state) {
        if (state.status() == SeriesStatus.COMPLETED) {
            return new ScheduleStripView(
                    "Completed",
                    "Completed",
                    null,
                    null,
                    state.status(),
                    state.cadence(),
                    state.periodDays(),
                    state.releaseHourIst()
            );
        }
        if (state.status() == SeriesStatus.HIATUS) {
            return new ScheduleStripView(
                    "On hiatus",
                    "On hiatus — back soon",
                    null,
                    state.skipMessage(),
                    state.status(),
                    state.cadence(),
                    state.periodDays(),
                    state.releaseHourIst()
            );
        }
        if (!state.hasActiveCadence() || state.nextExpectedAt() == null) {
            return new ScheduleStripView(
                    "Schedule",
                    "Updates when published",
                    null,
                    state.skipMessage(),
                    state.status(),
                    SeriesCadence.OFF,
                    null,
                    null
            );
        }
        String periodLabel = periodLabel(state.periodDays());
        return new ScheduleStripView(
                "Next update",
                periodLabel,
                state.nextExpectedAt(),
                state.skipMessage(),
                state.status(),
                state.cadence(),
                state.periodDays(),
                state.releaseHourIst()
        );
    }

    private static String periodLabel(Integer periodDays) {
        if (periodDays == null) {
            return "Updates when published";
        }
        if (periodDays == 7) {
            return "Every 7 days";
        }
        if (periodDays == 14) {
            return "Every 14 days";
        }
        return "Every " + periodDays + " days";
    }
}
