package com.amarkatha.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amarkatha.reader.dto.GlimpseDto;
import com.amarkatha.reader.dto.SeriesDetailDto;

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
    void monsoonMarketIncludesStripsBetweenChapters() {
        SeriesDetailDto series = catalog.series("monsoon-market");

        assertEquals(2, series.glimpses().size());
        GlimpseDto turnaround = series.glimpses().get(0);
        GlimpseDto teaser = series.glimpses().get(1);
        assertEquals("CHARACTER", turnaround.tag());
        assertEquals(3, turnaround.images().size());
        assertTrue(turnaround.images().get(0).reactionCount() > 0);
        assertEquals("TEASER", teaser.tag());
        assertEquals(2, teaser.images().size());

        var chapters = series.chapters();
        assertTrue(turnaround.postedAt().isAfter(chapters.get(0).listedAt()));
        assertTrue(turnaround.postedAt().isBefore(chapters.get(1).listedAt()));
        assertTrue(teaser.postedAt().isAfter(chapters.get(1).listedAt()));
        assertTrue(teaser.postedAt().isBefore(chapters.get(2).listedAt()));

        SeriesDetailDto salt = catalog.series("salt-and-saffron");
        assertEquals(1, salt.glimpses().size());
        assertEquals("LORE", salt.glimpses().get(0).tag());
        assertEquals(1, salt.glimpses().get(0).images().size());
    }

    @Test
    void chapterUsesSamplePages() {
        var chapter = catalog.chapter("monsoon-market", "chapter-2");

        assertEquals("Monsoon Market", chapter.seriesTitle());
        assertEquals(4, chapter.pages().size());
        assertTrue(chapter.pages().get(0).imageUrl().startsWith("/demo/"));
    }
}
