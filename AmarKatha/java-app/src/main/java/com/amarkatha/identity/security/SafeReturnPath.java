package com.amarkatha.identity.security;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validates same-origin relative return paths for OAuth round-trips.
 * Rejects protocol-relative URLs, absolute URLs, and path traversal tricks.
 */
public final class SafeReturnPath {

    private static final Pattern SAFE_RELATIVE = Pattern.compile(
            "^/[A-Za-z0-9._~!$&'()*+,;=:@%/\\-]*$"
    );
    private static final Pattern SERIES_SLUG = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private SafeReturnPath() {
    }

    public static Optional<String> normalize(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return Optional.empty();
        }
        String path = candidate.trim();
        if (!path.startsWith("/") || path.startsWith("//")) {
            return Optional.empty();
        }
        if (path.contains("://") || path.contains("\\") || path.indexOf('\n') >= 0 || path.indexOf('\r') >= 0) {
            return Optional.empty();
        }
        int queryIdx = path.indexOf('?');
        int hashIdx = path.indexOf('#');
        String pathOnly = path;
        String suffix = "";
        if (queryIdx >= 0 || hashIdx >= 0) {
            int cut = queryIdx >= 0 && hashIdx >= 0 ? Math.min(queryIdx, hashIdx)
                    : queryIdx >= 0 ? queryIdx : hashIdx;
            pathOnly = path.substring(0, cut);
            suffix = path.substring(cut);
            if (suffix.indexOf('\n') >= 0 || suffix.indexOf('\r') >= 0) {
                return Optional.empty();
            }
        }
        if (!SAFE_RELATIVE.matcher(pathOnly).matches()) {
            return Optional.empty();
        }
        if (pathOnly.contains("/../") || pathOnly.endsWith("/..") || pathOnly.contains("/./")) {
            return Optional.empty();
        }
        return Optional.of(pathOnly + suffix);
    }

    public static Optional<String> normalizeSeriesSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        String trimmed = slug.trim().toLowerCase();
        if (!SERIES_SLUG.matcher(trimmed).matches()) {
            return Optional.empty();
        }
        return Optional.of(trimmed);
    }

    public static void store(HttpSession session, String candidate) {
        if (session == null) {
            return;
        }
        Optional<String> safe = normalize(candidate);
        if (safe.isPresent()) {
            session.setAttribute(AuthSessionKeys.OAUTH_RETURN_TO, safe.get());
        } else {
            session.removeAttribute(AuthSessionKeys.OAUTH_RETURN_TO);
        }
    }

    public static Optional<String> consume(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object raw = session.getAttribute(AuthSessionKeys.OAUTH_RETURN_TO);
        session.removeAttribute(AuthSessionKeys.OAUTH_RETURN_TO);
        if (raw == null) {
            return Optional.empty();
        }
        return normalize(raw.toString());
    }

    public static Optional<String> peek(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object raw = session.getAttribute(AuthSessionKeys.OAUTH_RETURN_TO);
        if (raw == null) {
            return Optional.empty();
        }
        return normalize(raw.toString());
    }
}
