package com.amarkatha.business;

import java.util.Locale;

public enum GlimpseTag {
    CHARACTER,
    BACKGROUND,
    LORE,
    ITEMS,
    TEASER;

    public static GlimpseTag parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return GlimpseTag.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
