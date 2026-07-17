package com.amarkatha.publishing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SlugGeneratorTest {

    @Test
    void fromTitleNormalizesUnicodeAndSpaces() {
        assertEquals("amar-katha", SlugGenerator.fromTitle("Amar Katha"));
        assertEquals("nayika", SlugGenerator.fromTitle("Nāyikā"));
    }

    @Test
    void withSuffixAppendsCounter() {
        assertEquals("series-2", SlugGenerator.withSuffix("series", 2));
    }
}
