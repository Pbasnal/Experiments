package com.amarkatha.publishing;

import java.util.List;

/**
 * Cover URL and placeholder gradients shared by creator workspace and reader.
 */
public final class SeriesCoverPresentation {

    private static final List<String> GRADIENTS = List.of(
            "linear-gradient(135deg, #6366f1 0%, #a855f7 50%, #ec4899 100%)",
            "linear-gradient(135deg, #f59e0b 0%, #ef4444 55%, #7c3aed 100%)",
            "linear-gradient(135deg, #0f172a 0%, #334155 45%, #dc2626 100%)",
            "linear-gradient(135deg, #059669 0%, #14b8a6 50%, #6366f1 100%)",
            "linear-gradient(135deg, #db2777 0%, #f97316 60%, #eab308 100%)",
            "linear-gradient(135deg, #1e1b4b 0%, #4338ca 40%, #06b6d4 100%)"
    );

    private SeriesCoverPresentation() {
    }

    public static String coverGradient(String slug) {
        int idx = Math.floorMod(slug == null ? 0 : slug.hashCode(), GRADIENTS.size());
        return GRADIENTS.get(idx);
    }

    public static String coverUrl(String storageKey, int version) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        return "/media/" + storageKey + "?v=" + version;
    }
}
