package com.amarkatha.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ReaderFeatureGateTest {

    @Test
    void defaultsAllowCorePortalExceptEmail() {
        ReaderFeatureGate gate = new ReaderFeatureGate(
                new ReaderFeatureProperties(true, true, true, true, false)
        );

        assertTrue(gate.landingDiscoveryEnabled());
        assertTrue(gate.profileProgressEnabled());
        assertTrue(gate.followsEnabled());
        assertTrue(gate.inAppNotificationsEnabled());
        assertFalse(gate.emailNotificationsEnabled(true));
        assertFalse(gate.emailNotificationsEnabled(false));
    }

    @Test
    void emailRequiresBothFlagAndMailCapability() {
        ReaderFeatureGate gate = new ReaderFeatureGate(
                new ReaderFeatureProperties(true, true, true, true, true)
        );

        assertTrue(gate.emailNotificationsEnabled(true));
        assertFalse(gate.emailNotificationsEnabled(false));
    }

    @Test
    void disabledFollowsThrowsNotFound() {
        ReaderFeatureGate gate = new ReaderFeatureGate(
                new ReaderFeatureProperties(true, true, false, true, false)
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, gate::requireFollows);
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void disabledProfileThrowsNotFound() {
        ReaderFeatureGate gate = new ReaderFeatureGate(
                new ReaderFeatureProperties(true, false, true, true, false)
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, gate::requireProfileProgress);
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void disabledNotificationsThrowsNotFound() {
        ReaderFeatureGate gate = new ReaderFeatureGate(
                new ReaderFeatureProperties(true, true, true, false, false)
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, gate::requireInAppNotifications);
        assertEquals(404, ex.getStatusCode().value());
    }
}
