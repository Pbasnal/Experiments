package com.amarkatha.reader;

import com.amarkatha.business.HomeDiscoveryInput;
import com.amarkatha.business.HomeDiscoveryInstruction;
import com.amarkatha.business.HomeDiscoveryPolicy;
import com.amarkatha.business.HomeFilter;
import com.amarkatha.catalog.LanguageFilterProperties;
import com.amarkatha.reader.dto.ChapterPageDto;
import com.amarkatha.reader.dto.ChapterReaderDto;
import com.amarkatha.reader.dto.ChapterSummaryDto;
import com.amarkatha.reader.dto.GlimpseDto;
import com.amarkatha.reader.dto.GlimpseImageDto;
import com.amarkatha.reader.dto.HomeResponse;
import com.amarkatha.reader.dto.LanguageOptionDto;
import com.amarkatha.reader.dto.ScheduleStripDto;
import com.amarkatha.reader.dto.SeriesCardDto;
import com.amarkatha.reader.dto.SeriesDetailDto;
import com.amarkatha.shared.demo.DemoLibrary;
import com.amarkatha.shared.demo.DemoLibrary.DemoChapter;
import com.amarkatha.shared.demo.DemoLibrary.DemoGlimpse;
import com.amarkatha.shared.demo.DemoLibrary.DemoGlimpseImage;
import com.amarkatha.shared.demo.DemoLibrary.DemoStory;
import com.amarkatha.shared.demo.DemoPreviewState;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DemoCatalogService {

    private static final List<String> PAGE_URLS = List.of(
            DemoLibrary.PAGE_ONE,
            DemoLibrary.PAGE_TWO,
            DemoLibrary.PAGE_THREE,
            DemoLibrary.PAGE_FOUR
    );

    private final HomeDiscoveryPolicy homeDiscoveryPolicy;
    private final LanguageFilterProperties languageFilterProperties;

    public DemoCatalogService(
            HomeDiscoveryPolicy homeDiscoveryPolicy,
            LanguageFilterProperties languageFilterProperties
    ) {
        this.homeDiscoveryPolicy = homeDiscoveryPolicy;
        this.languageFilterProperties = languageFilterProperties;
    }

    public HomeResponse home(Locale locale) {
        List<SeriesCardDto> cards = DemoLibrary.stories().stream().map(this::card).toList();
        Map<String, Integer> counts = countByLanguage(cards);
        HomeDiscoveryInstruction instruction = homeDiscoveryPolicy.decide(new HomeDiscoveryInput(
                languageFilterProperties.enabled(),
                cards.size(),
                languageFilterProperties.minimumCatalogSize(),
                languageFilterProperties.minimumSeriesPerLanguage(),
                counts
        ));
        List<String> filters = instruction.filters().stream().map(HomeFilter::name).toList();
        List<LanguageOptionDto> languages = instruction.filters().contains(HomeFilter.LANGUAGE)
                ? instruction.eligibleLanguageCodes().stream()
                        .map(code -> languageOption(code, counts.getOrDefault(code, 0)))
                        .filter(option -> option.seriesCount() > 0)
                        .toList()
                : List.of();
        String tagline = "Publish on your rhythm. Share a link. Readers know when you're back.";
        return new HomeResponse(tagline, cards, List.of(), filters, languages);
    }

    public SeriesDetailDto series(String slug) {
        return series(slug, new DemoPreviewState());
    }

    public SeriesDetailDto series(String slug, DemoPreviewState state) {
        DemoStory story = requireStory(slug);
        DemoPreviewState reactions = state == null ? new DemoPreviewState() : state;
        List<ChapterSummaryDto> chapters = story.chapters().stream()
                .map(chapter -> new ChapterSummaryDto(
                        chapter.slug(),
                        chapter.title(),
                        chapter.number(),
                        agoDays(chapter.listedDaysAgo())
                ))
                .toList();
        List<GlimpseDto> glimpses = DemoLibrary.glimpsesFor(story.slug()).stream()
                .map(glimpse -> toGlimpse(glimpse, reactions))
                .toList();
        SeriesCardDto card = card(story);
        return new SeriesDetailDto(
                card.slug(),
                card.title(),
                card.creatorName(),
                card.description(),
                card.genres(),
                card.contentLanguage(),
                card.coverGradient(),
                card.coverUrl(),
                card.scheduleLabel(),
                card.schedule(),
                card.status(),
                card.lastUpdatedAt(),
                chapters.size(),
                chapters,
                false,
                card.rating(),
                card.readerCount(),
                card.editorsPick(),
                glimpses
        );
    }

    private static GlimpseDto toGlimpse(DemoGlimpse glimpse, DemoPreviewState state) {
        List<GlimpseImageDto> images = glimpse.images().stream()
                .map(image -> toImage(image, state))
                .toList();
        return new GlimpseDto(glimpse.id(), glimpse.tag(), agoDays(glimpse.postedDaysAgo()), images);
    }

    private static GlimpseImageDto toImage(DemoGlimpseImage image, DemoPreviewState state) {
        boolean reacted = state.glimpseReacted(image.id());
        long count = image.reactionCount() + (reacted ? 1 : 0);
        return new GlimpseImageDto(image.id(), image.url(), image.sortOrder(), count, reacted);
    }

    public ChapterReaderDto chapter(String seriesSlug, String chapterSlug) {
        DemoStory story = requireStory(seriesSlug);
        DemoChapter chapter = story.chapter(chapterSlug)
                .orElseThrow(() -> notFound("Chapter not found"));
        List<ChapterPageDto> pages = PAGE_URLS.stream()
                .map(url -> new ChapterPageDto(PAGE_URLS.indexOf(url) + 1, url, 800, 1200))
                .toList();
        return new ChapterReaderDto(
                story.slug(),
                story.title(),
                chapter.slug(),
                chapter.title(),
                chapter.number(),
                pages
        );
    }

    private SeriesCardDto card(DemoStory story) {
        Instant updated = agoDays(story.updatedDaysAgo());
        ScheduleStripDto schedule = new ScheduleStripDto(
                story.scheduleHeadline(),
                story.scheduleLabel(),
                story.periodDays() > 0 ? updated.plus(story.periodDays(), ChronoUnit.DAYS) : null,
                "HIATUS".equals(story.status()) ? "Back after this arc." : null,
                story.status(),
                story.periodDays() > 0 ? "EVERY_N_DAYS" : "OFF",
                story.periodDays() > 0 ? story.periodDays() : null,
                story.periodDays() > 0 ? 18 : null
        );
        return new SeriesCardDto(
                story.slug(),
                story.title(),
                story.creatorName(),
                story.description(),
                story.genres(),
                story.language(),
                ReaderPresentation.coverGradient(story.slug()),
                null,
                story.scheduleLabel(),
                schedule,
                story.status(),
                updated,
                story.chapters().size(),
                story.rating(),
                story.readerCount(),
                story.editorsPick()
        );
    }

    private static LanguageOptionDto languageOption(String code, int count) {
        Locale language = Locale.forLanguageTag(code);
        String label = language.getDisplayLanguage(Locale.ENGLISH);
        if (label == null || label.isBlank()) {
            label = code;
        }
        String nativeLabel = language.getDisplayLanguage(language);
        if (nativeLabel == null || nativeLabel.isBlank()) {
            nativeLabel = label;
        }
        return new LanguageOptionDto(code, label, nativeLabel, count);
    }

    private static Map<String, Integer> countByLanguage(List<SeriesCardDto> cards) {
        Map<String, Integer> counts = new HashMap<>();
        for (SeriesCardDto card : cards) {
            counts.merge(card.contentLanguage(), 1, Integer::sum);
        }
        return counts;
    }

    private static DemoStory requireStory(String slug) {
        return DemoLibrary.find(slug).orElseThrow(() -> notFound("Series not found"));
    }

    private static Instant agoDays(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
