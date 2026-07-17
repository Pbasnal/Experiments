package com.amarkatha.scheduling;

import com.amarkatha.shared.domain.SeriesCadence;
import com.amarkatha.shared.domain.SeriesStatus;
import java.time.Instant;

/**
 * Pure schedule transitions. No persistence.
 */
public final class ScheduleActions {

    private final ScheduleCalendar calendar;

    public ScheduleActions(ScheduleCalendar calendar) {
        this.calendar = calendar;
    }

    public ScheduleActionResult activate(
            ScheduleState current,
            int periodDays,
            int isoDayOfWeek,
            int releaseHourIst
    ) {
        requireNotCompleted(current);
        ScheduleCalendar.requirePeriodDays(periodDays);
        ScheduleCalendar.requireIsoDayOfWeek(isoDayOfWeek);
        ScheduleCalendar.requireReleaseHour(releaseHourIst);

        Instant from = current.lastPublishedAt() != null
                ? max(calendar.now(), current.lastPublishedAt())
                : calendar.now();
        Instant next = calendar.nextOccurrence(isoDayOfWeek, releaseHourIst, from);
        SeriesCadence cadence = ScheduleCalendar.cadenceForPeriodDays(periodDays);

        ScheduleState nextState = new ScheduleState(
                current.status() == SeriesStatus.HIATUS ? SeriesStatus.ONGOING : current.status(),
                cadence,
                periodDays,
                (short) isoDayOfWeek,
                (short) releaseHourIst,
                next,
                current.lastPublishedAt(),
                null
        );
        return ScheduleActionResult.of(current, nextState, ScheduleEventType.ACTIVATE_CADENCE, null);
    }

    public ScheduleActionResult clearCadence(ScheduleState current) {
        requireNotCompleted(current);
        ScheduleState nextState = new ScheduleState(
                current.status(),
                SeriesCadence.OFF,
                null,
                null,
                null,
                null,
                current.lastPublishedAt(),
                null
        );
        return ScheduleActionResult.of(current, nextState, ScheduleEventType.CLEAR_CADENCE, null);
    }

    public ScheduleActionResult skip(ScheduleState current, String message) {
        requireNotCompleted(current);
        if (current.status() == SeriesStatus.HIATUS) {
            throw new ScheduleException("Resume the series before skipping a slot.");
        }
        if (!current.hasActiveCadence() || current.nextExpectedAt() == null) {
            throw new ScheduleException("Set a release period before skipping.");
        }
        String note = normalizeSkipMessage(message);
        int hour = current.releaseHourOrDefault();
        Instant advanced = calendar.advanceByPeriod(current.nextExpectedAt(), current.periodDays(), hour);
        ScheduleState nextState = new ScheduleState(
                current.status(),
                current.cadence(),
                current.periodDays(),
                current.dayOfWeek(),
                (short) hour,
                advanced,
                current.lastPublishedAt(),
                note
        );
        return ScheduleActionResult.of(current, nextState, ScheduleEventType.SKIP, note);
    }

    public ScheduleActionResult hiatus(ScheduleState current) {
        requireNotCompleted(current);
        if (current.status() == SeriesStatus.HIATUS) {
            throw new ScheduleException("Series is already on hiatus.");
        }
        ScheduleState nextState = new ScheduleState(
                SeriesStatus.HIATUS,
                current.cadence(),
                current.periodDays(),
                current.dayOfWeek(),
                current.releaseHourIst(),
                null,
                current.lastPublishedAt(),
                current.skipMessage()
        );
        return ScheduleActionResult.of(current, nextState, ScheduleEventType.HIATUS, null);
    }

    public ScheduleActionResult resume(ScheduleState current) {
        requireNotCompleted(current);
        if (current.status() != SeriesStatus.HIATUS) {
            throw new ScheduleException("Series is not on hiatus.");
        }
        Instant next = null;
        int hour = current.releaseHourOrDefault();
        if (current.hasActiveCadence() && current.dayOfWeek() != null) {
            next = calendar.nextOccurrence(current.dayOfWeek(), hour, calendar.now());
        }
        ScheduleState nextState = new ScheduleState(
                SeriesStatus.ONGOING,
                current.cadence(),
                current.periodDays(),
                current.dayOfWeek(),
                current.hasActiveCadence() ? (short) hour : current.releaseHourIst(),
                next,
                current.lastPublishedAt(),
                null
        );
        return ScheduleActionResult.of(current, nextState, ScheduleEventType.RESUME, null);
    }

    /**
     * After a chapter is published: update lastPublishedAt and advance next expected when cadence is active.
     */
    public ScheduleActionResult onPublish(ScheduleState current, Instant publishedAt) {
        Instant priorNext = current.nextExpectedAt();
        Instant lastPublished = publishedAt;
        Instant next = current.nextExpectedAt();
        ScheduleEventType type = ScheduleEventType.PUBLISH_ADVANCE;
        int hour = current.releaseHourOrDefault();

        if (current.status() == SeriesStatus.ONGOING
                && current.hasActiveCadence()
                && current.dayOfWeek() != null) {
            next = calendar.nextOccurrence(current.dayOfWeek(), hour, publishedAt);
            if (priorNext != null && publishedAt.isBefore(priorNext)) {
                type = ScheduleEventType.PUBLISH_EARLY;
            }
        }

        ScheduleState nextState = new ScheduleState(
                current.status(),
                current.cadence(),
                current.periodDays(),
                current.dayOfWeek(),
                current.releaseHourIst() != null ? current.releaseHourIst() : (short) hour,
                next,
                lastPublished,
                current.skipMessage()
        );
        return ScheduleActionResult.of(current, nextState, type, null);
    }

    private static void requireNotCompleted(ScheduleState current) {
        if (current.status() == SeriesStatus.COMPLETED) {
            throw new ScheduleException("Completed series cannot change schedule.");
        }
    }

    private static String normalizeSkipMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String trimmed = message.trim();
        if (trimmed.length() > 280) {
            throw new ScheduleException("Skip note must be at most 280 characters.");
        }
        return trimmed;
    }

    private static Instant max(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }
}
