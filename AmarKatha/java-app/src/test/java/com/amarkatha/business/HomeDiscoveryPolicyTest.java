package com.amarkatha.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HomeDiscoveryPolicyTest {

    private HomeDiscoveryPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new HomeDiscoveryPolicy();
    }

    @Test
    void hidesLanguageWhenConfigDisabled() {
        HomeDiscoveryInstruction instruction = policy.decide(input(
                false,
                20,
                10,
                2,
                Map.of("en", 10, "hi", 10)
        ));
        assertTrue(instruction.filters().isEmpty());
        assertTrue(instruction.eligibleLanguageCodes().isEmpty());
    }

    @Test
    void hidesLanguageWhenCatalogBelowMinimum() {
        HomeDiscoveryInstruction instruction = policy.decide(input(
                true,
                9,
                10,
                2,
                Map.of("en", 5, "hi", 4)
        ));
        assertTrue(instruction.filters().isEmpty());
    }

    @Test
    void hidesLanguageWhenFewerThanTwoEligibleLanguages() {
        HomeDiscoveryInstruction instruction = policy.decide(input(
                true,
                12,
                10,
                2,
                Map.of("en", 10, "hi", 1, "ta", 1)
        ));
        assertTrue(instruction.filters().isEmpty());
    }

    @Test
    void showsLanguageAtThresholdWithTwoEligibleLanguages() {
        HomeDiscoveryInstruction instruction = policy.decide(input(
                true,
                10,
                10,
                2,
                Map.of("hi", 5, "en", 3, "ta", 2, "te", 1)
        ));
        assertEquals(List.of(HomeFilter.LANGUAGE), instruction.filters());
        assertEquals(List.of("hi", "en", "ta"), instruction.eligibleLanguageCodes());
    }

    @Test
    void debugThresholdsAllowSmallCatalog() {
        HomeDiscoveryInstruction instruction = policy.decide(input(
                true,
                2,
                0,
                1,
                Map.of("en", 1, "hi", 1)
        ));
        assertEquals(List.of(HomeFilter.LANGUAGE), instruction.filters());
        assertEquals(List.of("en", "hi"), instruction.eligibleLanguageCodes());
    }

    private static HomeDiscoveryInput input(
            boolean enabled,
            int totalResults,
            int minimumCatalogSize,
            int minimumSeriesPerLanguage,
            Map<String, Integer> counts
    ) {
        return new HomeDiscoveryInput(
                enabled,
                totalResults,
                minimumCatalogSize,
                minimumSeriesPerLanguage,
                counts
        );
    }
}
