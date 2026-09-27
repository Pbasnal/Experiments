package com.amarkatha.business;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Decides which home discovery filters to show. Thresholds never leave this policy.
 */
public final class HomeDiscoveryPolicy {

    private static final int MIN_ELIGIBLE_LANGUAGES = 2;

    public HomeDiscoveryInstruction decide(HomeDiscoveryInput input) {
        if (!input.languageFiltersEnabled()
                || input.totalResults() < input.minimumCatalogSize()) {
            return HomeDiscoveryInstruction.none();
        }

        List<String> eligible = input.seriesCountByLanguage().entrySet().stream()
                .filter(entry -> entry.getValue() != null
                        && entry.getValue() >= input.minimumSeriesPerLanguage())
                .sorted(languageOrder())
                .map(Map.Entry::getKey)
                .toList();

        if (eligible.size() < MIN_ELIGIBLE_LANGUAGES) {
            return HomeDiscoveryInstruction.none();
        }

        return new HomeDiscoveryInstruction(List.of(HomeFilter.LANGUAGE), eligible);
    }

    private static Comparator<Map.Entry<String, Integer>> languageOrder() {
        return Comparator
                .<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey);
    }
}
