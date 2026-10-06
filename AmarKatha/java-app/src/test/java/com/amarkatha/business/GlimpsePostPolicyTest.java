package com.amarkatha.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GlimpsePostPolicyTest {

    private final GlimpsePostPolicy policy = new GlimpsePostPolicy();

    @Test
    void acceptsOneToTenImagesAndAKnownTag() {
        assertEquals(GlimpseTag.CHARACTER, policy.requireValid("character", 1));
        assertEquals(GlimpseTag.TEASER, policy.requireValid("TEASER", 10));
    }

    @Test
    void rejectsZeroAndElevenImages() {
        assertThrows(IllegalArgumentException.class, () -> policy.requireValid("LORE", 0));
        assertThrows(IllegalArgumentException.class, () -> policy.requireValid("LORE", 11));
    }

    @Test
    void rejectsAnUnknownTag() {
        assertThrows(IllegalArgumentException.class, () -> policy.requireValid("SKETCH", 1));
        assertThrows(IllegalArgumentException.class, () -> policy.requireValid(" ", 1));
    }
}
