package com.amarkatha.business;

import java.util.List;
import java.util.Objects;

/**
 * Data the home API executes: which filters to render and which languages qualify.
 */
public record HomeDiscoveryInstruction(
        List<HomeFilter> filters,
        List<String> eligibleLanguageCodes
) {
    public HomeDiscoveryInstruction {
        Objects.requireNonNull(filters, "filters");
        Objects.requireNonNull(eligibleLanguageCodes, "eligibleLanguageCodes");
        filters = List.copyOf(filters);
        eligibleLanguageCodes = List.copyOf(eligibleLanguageCodes);
    }

    public static HomeDiscoveryInstruction none() {
        return new HomeDiscoveryInstruction(List.of(), List.of());
    }
}
