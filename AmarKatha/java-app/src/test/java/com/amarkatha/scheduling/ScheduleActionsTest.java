package com.amarkatha.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amarkatha.shared.domain.SeriesCadence;
import com.amarkatha.shared.domain.SeriesStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduleActionsTest {

    private ScheduleActions actions;
    private ScheduleCalendar calendar;

    @BeforeEach
    void setUp() {
        Instant fixed = Instant.parse("2026-07-15T10:00:00Z");
        calendar = new ScheduleCalendar(Clock.fixed(fixed, ZoneOffset.UTC));
        actions = new ScheduleActions(calendar);
    }

    private ScheduleState blank() {
        return new ScheduleState(
                SeriesStatus.ONGOING,
                SeriesCadence.OFF,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void activateSetsNextOnChosenWeekdayAndHour() {
        ScheduleActionResult result = actions.activate(blank(), 7, 5, 18);
        assertEquals(SeriesCadence.WEEKLY, result.state().cadence());
        assertEquals(7, result.state().periodDays());
        assertEquals((short) 18, result.state().releaseHourIst());
        assertEquals(calendar.atIstHour(LocalDate.of(2026, 7, 17), 18), result.state().nextExpectedAt());
        assertEquals(ScheduleEventType.ACTIVATE_CADENCE, result.eventType());
    }

    @Test
    void skipAdvancesByPeriodDaysKeepingHour() {
        ScheduleState active = actions.activate(blank(), 10, 5, 20).state();
        Instant before = active.nextExpectedAt();
        ScheduleActionResult skipped = actions.skip(active, "Exams this week");
        assertEquals(calendar.advanceByPeriod(before, 10, 20), skipped.state().nextExpectedAt());
        assertEquals("Exams this week", skipped.state().skipMessage());
        assertEquals(ScheduleEventType.SKIP, skipped.eventType());
    }

    @Test
    void skipRequiresActiveCadence() {
        assertThrows(ScheduleException.class, () -> actions.skip(blank(), null));
    }

    @Test
    void hiatusClearsNextAndResumeRestores() {
        ScheduleState active = actions.activate(blank(), 7, 1, 18).state();
        ScheduleState onHiatus = actions.hiatus(active).state();
        assertEquals(SeriesStatus.HIATUS, onHiatus.status());
        assertNull(onHiatus.nextExpectedAt());
        assertEquals(7, onHiatus.periodDays());

        ScheduleState resumed = actions.resume(onHiatus).state();
        assertEquals(SeriesStatus.ONGOING, resumed.status());
        assertTrue(resumed.nextExpectedAt() != null);
        assertNull(resumed.skipMessage());
    }

    @Test
    void onPublishAdvancesNextOccurrence() {
        ScheduleState active = actions.activate(blank(), 7, 5, 18).state();
        Instant published = Instant.parse("2026-07-16T10:00:00Z");
        ScheduleActionResult after = actions.onPublish(active, published);
        assertEquals(published, after.state().lastPublishedAt());
        assertEquals(calendar.atIstHour(LocalDate.of(2026, 7, 17), 18), after.state().nextExpectedAt());
        assertEquals(ScheduleEventType.PUBLISH_EARLY, after.eventType());
    }

    @Test
    void onPublishAfterSlotIsAdvance() {
        ScheduleState active = actions.activate(blank(), 7, 5, 18).state();
        Instant published = Instant.parse("2026-07-20T08:00:00Z");
        ScheduleActionResult after = actions.onPublish(active, published);
        assertEquals(calendar.atIstHour(LocalDate.of(2026, 7, 24), 18), after.state().nextExpectedAt());
        assertEquals(ScheduleEventType.PUBLISH_ADVANCE, after.eventType());
    }

    @Test
    void clearCadenceTurnsOff() {
        ScheduleState active = actions.activate(blank(), 7, 5, 18).state();
        ScheduleState cleared = actions.clearCadence(active).state();
        assertEquals(SeriesCadence.OFF, cleared.cadence());
        assertNull(cleared.periodDays());
        assertNull(cleared.releaseHourIst());
        assertNull(cleared.nextExpectedAt());
    }
}
