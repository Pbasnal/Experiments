package com.amarkatha.identity.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class SafeReturnPathTest {

    @Test
    void acceptsSameOriginRelativePaths() {
        assertEquals(Optional.of("/read/s/my-series"), SafeReturnPath.normalize("/read/s/my-series"));
        assertEquals(Optional.of("/read/profile"), SafeReturnPath.normalize("/read/profile"));
        assertEquals(Optional.of("/read/s/foo?x=1"), SafeReturnPath.normalize("/read/s/foo?x=1"));
    }

    @Test
    void rejectsOpenRedirects() {
        assertTrue(SafeReturnPath.normalize("https://evil.example/phish").isEmpty());
        assertTrue(SafeReturnPath.normalize("//evil.example/phish").isEmpty());
        assertTrue(SafeReturnPath.normalize("/\\evil.example").isEmpty());
        assertTrue(SafeReturnPath.normalize("/read/../../etc/passwd").isEmpty());
        assertTrue(SafeReturnPath.normalize("read/s/foo").isEmpty());
    }

    @Test
    void storeAndConsumeRoundTrip() {
        MockHttpSession session = new MockHttpSession();
        SafeReturnPath.store(session, "/read/s/demo");
        assertEquals(Optional.of("/read/s/demo"), SafeReturnPath.consume(session));
        assertEquals(Optional.empty(), SafeReturnPath.consume(session));
    }

    @Test
    void normalizesSeriesSlug() {
        assertEquals(Optional.of("my-series"), SafeReturnPath.normalizeSeriesSlug("my-series"));
        assertTrue(SafeReturnPath.normalizeSeriesSlug("../x").isEmpty());
        assertTrue(SafeReturnPath.normalizeSeriesSlug("My Series").isEmpty());
    }
}
