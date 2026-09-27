package com.amarkatha.business;

/**
 * Decides where a reader enters a series. Persistence and URL construction stay
 * outside the business layer.
 */
public final class ReadingEntryPolicy {

    public ReadingEntryInstruction decide(ReadingEntryInput input) {
        return input.hasReadableProgress()
                ? ReadingEntryInstruction.RESUME_LAST_CHAPTER
                : ReadingEntryInstruction.OPEN_SERIES;
    }
}
