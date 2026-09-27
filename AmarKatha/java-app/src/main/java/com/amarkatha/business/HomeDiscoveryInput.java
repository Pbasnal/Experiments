package com.amarkatha.business;

import java.util.Map;
import java.util.Objects;

/**
 * Facts for home discovery. Thresholds and flags are inputs so the policy stays Spring-free.
 */
public record HomeDiscoveryInput(
        boolean languageFiltersEnabled,
        int totalResults,
        int minimumCatalogSize,
        int minimumSeriesPerLanguage,
        Map<String, Integer> seriesCountByLanguage
) {
    public HomeDiscoveryInput {
        Objects.requireNonNull(seriesCountByLanguage, "seriesCountByLanguage");
        seriesCountByLanguage = Map.copyOf(seriesCountByLanguage);
    }
}
