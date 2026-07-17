package com.amarkatha.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amarkatha.shared.domain.SeriesCadence;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduleCalendarTest {

    private ScheduleCalendar calendar;

    @BeforeEach
    void setUp() {
        Instant fixed = Instant.parse("2026-07-15T10:00:00Z"); // Wed 15:30 IST
        calendar = new ScheduleCalendar(Clock.fixed(fixed, ZoneOffset.UTC));
    }

    @Test
    void nextOccurrenceUsesReleaseHourIst() {
        Instant next = calendar.nextOccurrence(5, 18, Instant.parse("2026-07-15T10:00:00Z"));
        assertEquals(LocalDate.of(2026, 7, 17), calendar.toIstDate(next));
        assertEquals(calendar.atIstHour(LocalDate.of(2026, 7, 17), 18), next);
    }

    @Test
    void nextOccurrenceSameDayIfHourStillAhead() {
        // Friday 10:00 UTC = 15:30 IST; Friday 18:00 IST is still ahead
        Instant fridayMorningUtc = Instant.parse("2026-07-17T10:00:00Z");
        Instant next = calendar.nextOccurrence(5, 18, fridayMorningUtc);
        assertEquals(calendar.atIstHour(LocalDate.of(2026, 7, 17), 18), next);
    }

    @Test
    void advanceByPeriodKeepsHour() {
        Instant friday = calendar.atIstHour(LocalDate.of(2026, 7, 17), 20);
        Instant advanced = calendar.advanceByPeriod(friday, 7, 20);
        assertEquals(calendar.atIstHour(LocalDate.of(2026, 7, 24), 20), advanced);
    }

    @Test
    void cadenceForPeriodDaysMapsPresets() {
        assertEquals(SeriesCadence.WEEKLY, ScheduleCalendar.cadenceForPeriodDays(7));
        assertEquals(SeriesCadence.BIWEEKLY, ScheduleCalendar.cadenceForPeriodDays(14));
        assertEquals(SeriesCadence.CUSTOM, ScheduleCalendar.cadenceForPeriodDays(10));
    }

    @Test
    void rejectsInvalidHour() {
        assertThrows(ScheduleException.class, () -> ScheduleCalendar.requireReleaseHour(-1));
        assertThrows(ScheduleException.class, () -> ScheduleCalendar.requireReleaseHour(24));
    }
}
