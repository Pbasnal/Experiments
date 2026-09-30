package com.amarkatha.shared.demo;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Raw facts placed on the request by the identity filter.
 * {@code ExperienceSourcePolicy} is what turns them into live or demo.
 */
public final class DemoModeSignals {

    public static final String ADMIN = "amarkatha.demo.admin";
    public static final String REQUESTED = "amarkatha.demo.requested";
    public static final String VIEWER_ID = "amarkatha.viewerId";

    private DemoModeSignals() {
    }

    public static boolean admin(HttpServletRequest request) {
        return request != null && Boolean.TRUE.equals(request.getAttribute(ADMIN));
    }

    public static boolean requested(HttpServletRequest request) {
        return request != null && Boolean.TRUE.equals(request.getAttribute(REQUESTED));
    }
}
