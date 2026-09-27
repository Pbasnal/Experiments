package com.amarkatha.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReadingEntryPolicyTest {

    private final ReadingEntryPolicy policy = new ReadingEntryPolicy();

    @Test
    void opensSeriesWhenThereIsNoReadableProgress() {
        assertEquals(
                ReadingEntryInstruction.OPEN_SERIES,
                policy.decide(new ReadingEntryInput(false))
        );
    }

    @Test
    void resumesLastChapterWhenReadableProgressExists() {
        assertEquals(
                ReadingEntryInstruction.RESUME_LAST_CHAPTER,
                policy.decide(new ReadingEntryInput(true))
        );
    }
}
