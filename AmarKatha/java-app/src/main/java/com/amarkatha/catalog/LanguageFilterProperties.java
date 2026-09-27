package com.amarkatha.catalog;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Backend gate for the home language strip. React must not recompute these thresholds.
 */
@ConfigurationProperties(prefix = "amarkatha.catalog.language-filters")
public record LanguageFilterProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("10") int minimumCatalogSize,
        @DefaultValue("2") int minimumSeriesPerLanguage
) {
}
