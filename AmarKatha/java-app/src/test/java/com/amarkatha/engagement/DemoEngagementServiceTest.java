package com.amarkatha.engagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amarkatha.business.ReadingEntryPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class DemoEngagementServiceTest {

    private final DemoEngagementService engagement = new DemoEngagementService(new ReadingEntryPolicy());

    @Test
    void readTargetResumesTheSampleChapter() {
        var target = engagement.readTarget("monsoon-market", new MockHttpSession());

        assertEquals("/read/s/monsoon-market/c/chapter-2", target.href());
        assertTrue(target.resumed());
    }

    @Test
    void readTargetOpensTheSeriesWhenTheSampleHasNoProgress() {
        var target = engagement.readTarget("paper-boats", new MockHttpSession());

        assertEquals("/read/s/paper-boats", target.href());
        assertFalse(target.resumed());
    }

    @Test
    void followChangesStayInTheSession() {
        MockHttpSession session = new MockHttpSession();

        assertFalse(engagement.followState("night-bus", session).followed());
        engagement.follow("night-bus", session);
        assertTrue(engagement.followState("night-bus", session).followed());
        engagement.unfollow("monsoon-market", session);
        assertFalse(engagement.followState("monsoon-market", session).followed());
    }

    @Test
    void readingAChapterBecomesTheResumePoint() {
        MockHttpSession session = new MockHttpSession();

        engagement.recordRead("paper-boats", "chapter-3", session);

        assertEquals("/read/s/paper-boats/c/chapter-3", engagement.readTarget("paper-boats", session).href());
    }
}
