package com.amarkatha.scheduling;

/**
 * Domain error for invalid schedule transitions (skip without cadence, etc.).
 */
public class ScheduleException extends RuntimeException {

    public ScheduleException(String message) {
        super(message);
    }
}
