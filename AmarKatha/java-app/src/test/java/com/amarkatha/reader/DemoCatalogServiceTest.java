package com.amarkatha.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amarkatha.business.HomeDiscoveryPolicy;
import com.amarkatha.catalog.LanguageFilterProperties;
import com.amarkatha.reader.dto.HomeResponse;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class DemoCatalogServiceTest {

    private final DemoCatalogService catalog = new DemoCatalogService(
            new HomeDiscoveryPolicy(),
            new LanguageFilterProperties(true, 10, 2)
    );

    @Test
    void homeShowsAFilledCatalogWithLanguageFiltersAndEditorsPicks() {
        HomeResponse home = catalog.home(Locale.ENGLISH);

        assertTrue(home.recentlyUpdated().size() >= 12);
        assertEquals(java.util.List.of("LANGUAGE"), home.filters());
        assertTrue(home.languageOptions().size() >= 2);
        assertTrue(home.recentlyUpdated().stream().anyMatch(card -> card.editorsPick() && card.rating() > 0));
        assertTrue(home.recentlyUpdated().stream().allMatch(card -> card.readerCount() > 0));
    }

    @Test
    void chapterUsesSamplePages() {
        var chapter = catalog.chapter("monsoon-market", "chapter-2");

        assertEquals("Monsoon Market", chapter.seriesTitle());
        assertEquals(4, chapter.pages().size());
        assertTrue(chapter.pages().get(0).imageUrl().startsWith("/demo/"));
    }
}
