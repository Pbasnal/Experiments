package com.amarkatha.publishing;

public class OngoingSeriesCapExceededException extends RuntimeException {

    public OngoingSeriesCapExceededException(int cap) {
        super("Creator already has the maximum of " + cap + " ongoing series");
    }
}
