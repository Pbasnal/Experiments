package com.amarkatha.business;

/**
 * A Glimpse is a free post of 1–10 images with one of the five tags.
 */
public final class GlimpsePostPolicy {

    public static final int MIN_IMAGES = 1;
    public static final int MAX_IMAGES = 10;

    public GlimpseTag requireValid(String tag, int imageCount) {
        GlimpseTag parsed = GlimpseTag.parse(tag);
        if (parsed == null) {
            throw new IllegalArgumentException("Unknown glimpse tag");
        }
        if (imageCount < MIN_IMAGES || imageCount > MAX_IMAGES) {
            throw new IllegalArgumentException("A glimpse needs 1 to 10 images");
        }
        return parsed;
    }
}
