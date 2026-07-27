package com.amarkatha.bootstrap;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Builds absolute public URLs for OG tags, emails, and share links.
 * Prefer {@code AMARKATHA_PUBLIC_BASE_URL} in production; otherwise use
 * forwarded headers / the incoming request (for local and reverse-proxy setups).
 */
@Component
public class AbsoluteUrlBuilder {

    private final String publicBaseUrl;

    public AbsoluteUrlBuilder(
            @Value("${amarkatha.public-base-url:}") String publicBaseUrl
    ) {
        this.publicBaseUrl = normalizeBase(publicBaseUrl);
    }

    public String absolute(HttpServletRequest request, String path) {
        String normalizedPath = normalizePath(path);
        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl + normalizedPath;
        }
        return requestOrigin(request) + normalizedPath;
    }

    public String configuredBaseUrl() {
        return publicBaseUrl;
    }

    static String normalizeBase(String base) {
        if (base == null) {
            return "";
        }
        String trimmed = base.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    static String requestOrigin(HttpServletRequest request) {
        String scheme = firstHeader(request, "X-Forwarded-Proto");
        if (!StringUtils.hasText(scheme)) {
            scheme = request.getScheme();
        } else if (scheme.contains(",")) {
            scheme = scheme.split(",")[0].trim();
        }

        String host = firstHeader(request, "X-Forwarded-Host");
        if (!StringUtils.hasText(host)) {
            host = request.getHeader("Host");
        }
        if (StringUtils.hasText(host) && host.contains(",")) {
            host = host.split(",")[0].trim();
        }
        if (!StringUtils.hasText(host)) {
            int port = request.getServerPort();
            boolean defaultPort = ("http".equals(scheme) && port == 80)
                    || ("https".equals(scheme) && port == 443);
            host = request.getServerName() + (defaultPort ? "" : ":" + port);
        }
        return scheme + "://" + host;
    }

    private static String firstHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
