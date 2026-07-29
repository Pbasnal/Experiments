package com.amarkatha.identity;

public class InviteInvalidException extends RuntimeException {

    private final InviteInvalidReason reason;

    public InviteInvalidException(InviteInvalidReason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public InviteInvalidReason getReason() {
        return reason;
    }

    public enum InviteInvalidReason {
        NOT_FOUND,
        ALREADY_USED,
        EXHAUSTED,
        EXPIRED,
        BLANK
    }
}
