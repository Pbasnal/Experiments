package com.amarkatha.identity;

public class OAuthOnboardingException extends RuntimeException {

    private final Reason reason;

    public OAuthOnboardingException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        INVITE_REQUIRED,
        ACCOUNT_NOT_FOUND,
        ADMIN_ACCESS_DENIED,
        MISSING_PROFILE
    }
}
