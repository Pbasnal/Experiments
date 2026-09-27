package com.amarkatha.shared.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * Anonymous analytics correlation cookie — not authentication.
 */
public final class ReaderIdCookie {

    public static final String COOKIE_NAME = "reader_id";
    private static final int COOKIE_MAX_AGE = 365 * 24 * 60 * 60;

    private ReaderIdCookie() {
    }

    public static String ensure(HttpServletRequest request, HttpServletResponse response) {
        String existing = read(request);
        if (existing != null && looksLikeUuid(existing)) {
            return existing;
        }
        String created = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(COOKIE_NAME, created);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "Lax");
        if (request.isSecure()) {
            cookie.setSecure(true);
        }
        response.addCookie(cookie);
        return created;
    }

    public static String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static boolean looksLikeUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
