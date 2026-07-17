package com.amarkatha.scheduling;

import com.amarkatha.shared.domain.SeriesCadence;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * IST calendar math. {@code next_expected_at} is the chosen weekday at {@code releaseHourIst}:00 IST,
 * stored as UTC Instant.
 */
public final class ScheduleCalendar {

    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    public static final int MIN_PERIOD_DAYS = 1;
    public static final int MAX_PERIOD_DAYS = 90;
    public static final int DEFAULT_RELEASE_HOUR_IST = 18;

    private final Clock clock;

    public ScheduleCalendar(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public Instant now() {
        return clock.instant();
    }

    /**
     * Next slot on {@code isoDayOfWeek} at {@code releaseHourIst}:00 IST that is strictly after {@code from}.
     * If {@code from} is already on that weekday and the hour today is still ahead, returns today's slot.
     */
    public Instant nextOccurrence(int isoDayOfWeek, int releaseHourIst, Instant from) {
        requireIsoDayOfWeek(isoDayOfWeek);
        requireReleaseHour(releaseHourIst);
        DayOfWeek target = DayOfWeek.of(isoDayOfWeek);
        ZonedDateTime fromIst = from.atZone(IST);
        LocalDate date = fromIst.toLocalDate();

        if (date.getDayOfWeek() == target) {
            Instant todaySlot = atIstHour(date, releaseHourIst);
            if (todaySlot.isAfter(from)) {
                return todaySlot;
            }
        }

        LocalDate candidate = date.plusDays(1);
        while (candidate.getDayOfWeek() != target) {
            candidate = candidate.plusDays(1);
        }
        return atIstHour(candidate, releaseHourIst);
    }

    /**
     * Advance {@code fromNextExpected} by {@code periodDays} IST calendar days, keeping the release hour.
     */
    public Instant advanceByPeriod(Instant fromNextExpected, int periodDays, int releaseHourIst) {
        requirePeriodDays(periodDays);
        requireReleaseHour(releaseHourIst);
        LocalDate base = toIstDate(fromNextExpected);
        return atIstHour(base.plusDays(periodDays), releaseHourIst);
    }

    public Instant atIstHour(LocalDate istDate, int releaseHourIst) {
        requireReleaseHour(releaseHourIst);
        return istDate.atTime(LocalTime.of(releaseHourIst, 0)).atZone(IST).toInstant();
    }

    public LocalDate toIstDate(Instant instant) {
        return ZonedDateTime.ofInstant(instant, IST).toLocalDate();
    }

    public static SeriesCadence cadenceForPeriodDays(int periodDays) {
        requirePeriodDays(periodDays);
        if (periodDays == 7) {
            return SeriesCadence.WEEKLY;
        }
        if (periodDays == 14) {
            return SeriesCadence.BIWEEKLY;
        }
        return SeriesCadence.CUSTOM;
    }

    public static void requirePeriodDays(int periodDays) {
        if (periodDays < MIN_PERIOD_DAYS || periodDays > MAX_PERIOD_DAYS) {
            throw new ScheduleException(
                    "Period must be between " + MIN_PERIOD_DAYS + " and " + MAX_PERIOD_DAYS + " days."
            );
        }
    }

    public static void requireIsoDayOfWeek(int dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new ScheduleException("Day of week must be 1 (Mon) through 7 (Sun).");
        }
    }

    public static void requireReleaseHour(int hour) {
        if (hour < 0 || hour > 23) {
            throw new ScheduleException("Release hour must be between 0 and 23 (IST).");
        }
    }
}
