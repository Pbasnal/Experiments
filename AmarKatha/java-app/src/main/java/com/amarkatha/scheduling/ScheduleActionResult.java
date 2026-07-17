package com.amarkatha.scheduling;

/**
 * Result of a schedule mutation: new state plus audit metadata.
 */
public record ScheduleActionResult(
        ScheduleState state,
        ScheduleEventType eventType,
        String message,
        InstantPair expectedChange
) {
    public record InstantPair(java.time.Instant previousNextExpectedAt, java.time.Instant newNextExpectedAt) {
    }

    public static ScheduleActionResult of(
            ScheduleState previous,
            ScheduleState next,
            ScheduleEventType type,
            String message
    ) {
        return new ScheduleActionResult(
                next,
                type,
                message,
                new InstantPair(previous.nextExpectedAt(), next.nextExpectedAt())
        );
    }
}
