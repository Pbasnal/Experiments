package com.amarkatha.publishing;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugGenerator {

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

    private SlugGenerator() {
    }

    public static String fromTitle(String title) {
        if (title == null || title.isBlank()) {
            return "series";
        }
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = NON_SLUG.matcher(normalized.toLowerCase(Locale.ROOT).trim())
                .replaceAll("-");
        slug = EDGE_DASHES.matcher(slug).replaceAll("");
        return slug.isEmpty() ? "series" : slug;
    }

    public static String withSuffix(String baseSlug, int suffix) {
        return baseSlug + "-" + suffix;
    }
}
