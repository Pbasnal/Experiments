package com.amarkatha.shared;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ReaderFeatureGate {

    private final ReaderFeatureProperties features;

    public ReaderFeatureGate(ReaderFeatureProperties features) {
        this.features = features;
    }

    public ReaderFeatureProperties properties() {
        return features;
    }

    public void requireLandingDiscovery() {
        require(features.landingDiscovery(), "landing discovery");
    }

    public void requireProfileProgress() {
        require(features.profileProgress(), "profile");
    }

    public void requireFollows() {
        require(features.follows(), "follows");
    }

    public void requireInAppNotifications() {
        require(features.inAppNotifications(), "notifications");
    }

    public boolean emailNotificationsEnabled(boolean mailConfigured) {
        return features.emailNotifications() && mailConfigured;
    }

    public boolean inAppNotificationsEnabled() {
        return features.inAppNotifications();
    }

    public boolean followsEnabled() {
        return features.follows();
    }

    public boolean profileProgressEnabled() {
        return features.profileProgress();
    }

    public boolean landingDiscoveryEnabled() {
        return features.landingDiscovery();
    }

    private static void require(boolean enabled, String label) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature unavailable: " + label);
        }
    }
}
