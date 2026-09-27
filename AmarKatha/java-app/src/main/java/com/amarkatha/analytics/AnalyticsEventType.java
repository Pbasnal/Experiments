package com.amarkatha.analytics;

public enum AnalyticsEventType {
    SERIES_VIEW,
    CHAPTER_VIEW,
    LANDING_VIEW,
    SIGNUP_CLICK,
    SIGNUP_SUCCESS,
    FOLLOW,
    UNFOLLOW,
    NOTIFICATION_CLICK,
    NOTIFICATION_READ,
    WEB_VITAL_LCP,
    WEB_VITAL_INP,
    WEB_VITAL_CLS,
    FRONTEND_EXCEPTION;

    public boolean clientIngestible() {
        return switch (this) {
            case LANDING_VIEW,
                    SIGNUP_CLICK,
                    NOTIFICATION_CLICK,
                    WEB_VITAL_LCP,
                    WEB_VITAL_INP,
                    WEB_VITAL_CLS,
                    FRONTEND_EXCEPTION -> true;
            case SERIES_VIEW,
                    CHAPTER_VIEW,
                    SIGNUP_SUCCESS,
                    FOLLOW,
                    UNFOLLOW,
                    NOTIFICATION_READ -> false;
        };
    }

    public boolean requiresMetaValue() {
        return switch (this) {
            case WEB_VITAL_LCP, WEB_VITAL_INP, WEB_VITAL_CLS -> true;
            default -> false;
        };
    }

    public boolean requiresExceptionName() {
        return this == FRONTEND_EXCEPTION;
    }
}
