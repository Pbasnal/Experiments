package com.amarkatha.identity;

public enum OAuthIntent {
    /** Unified Google button on signup — creates admin if bootstrap email, else needs invite for new creators. */
    GOOGLE_AUTH,
    CREATOR_SIGNUP,
    CREATOR_LOGIN,
    /** Kept for deep links; same outcome as GOOGLE_AUTH for bootstrap admins. */
    ADMIN_LOGIN
}
