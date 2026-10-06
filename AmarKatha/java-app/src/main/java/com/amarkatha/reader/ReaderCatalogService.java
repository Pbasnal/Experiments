package com.amarkatha.reader;

import com.amarkatha.business.HomeDiscoveryInput;
import com.amarkatha.business.HomeDiscoveryInstruction;
import com.amarkatha.business.HomeDiscoveryPolicy;
import com.amarkatha.business.HomeFilter;
import com.amarkatha.catalog.LanguageFilterProperties;
import com.amarkatha.publishing.ChapterMediaIntegrityService;
import com.amarkatha.publishing.ChapterPageRepository;
import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.GlimpseImageRepository;
import com.amarkatha.publishing.GlimpseRepository;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.SeriesScheduleService;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.ChapterPage;
import com.amarkatha.publishing.domain.Glimpse;
import com.amarkatha.publishing.domain.GlimpseImage;
import com.amarkatha.publishing.domain.Series;
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
import com.amarkatha.shared.glimpse.GlimpseReactionLookup;
import com.amarkatha.shared.glimpse.ImageReactionState;
import com.amarkatha.scheduling.ScheduleStripView;
import com.amarkatha.shared.ReaderFeatureGate;
import com.amarkatha.shared.domain.ChapterState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReaderCatalogService {

    private static final String TAGLINE_MESSAGE_KEY = "app.tagline";
    private static final String TAGLINE_FALLBACK =
            "Publish on your rhythm. Share a link. Readers know when you're back.";

    private final SeriesRepository seriesRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final ChapterMediaIntegrityService mediaIntegrityService;
    private final SeriesScheduleService seriesScheduleService;
    private final JdbcTemplate jdbcTemplate;
    private final HomeDiscoveryPolicy homeDiscoveryPolicy;
    private final LanguageFilterProperties languageFilterProperties;
    private final ReaderFeatureGate readerFeatureGate;
    private final MessageSource messageSource;
    private final GlimpseRepository glimpseRepository;
    private final GlimpseImageRepository glimpseImageRepository;
    private final GlimpseReactionLookup glimpseReactionLookup;

    public ReaderCatalogService(
            SeriesRepository seriesRepository,
            ChapterRepository chapterRepository,
            ChapterPageRepository chapterPageRepository,
            ChapterMediaIntegrityService mediaIntegrityService,
            SeriesScheduleService seriesScheduleService,
            JdbcTemplate jdbcTemplate,
            HomeDiscoveryPolicy homeDiscoveryPolicy,
            LanguageFilterProperties languageFilterProperties,
            ReaderFeatureGate readerFeatureGate,
            MessageSource messageSource,
            GlimpseRepository glimpseRepository,
            GlimpseImageRepository glimpseImageRepository,
            GlimpseReactionLookup glimpseReactionLookup
    ) {
        this.seriesRepository = seriesRepository;
        this.chapterRepository = chapterRepository;
        this.chapterPageRepository = chapterPageRepository;
        this.mediaIntegrityService = mediaIntegrityService;
        this.seriesScheduleService = seriesScheduleService;
        this.jdbcTemplate = jdbcTemplate;
        this.homeDiscoveryPolicy = homeDiscoveryPolicy;
        this.languageFilterProperties = languageFilterProperties;
        this.readerFeatureGate = readerFeatureGate;
        this.messageSource = messageSource;
        this.glimpseRepository = glimpseRepository;
        this.glimpseImageRepository = glimpseImageRepository;
        this.glimpseReactionLookup = glimpseReactionLookup;
    }

    @Transactional(readOnly = true)
    public HomeResponse home(Locale locale) {
        return home(locale, null);
    }

    @Transactional(readOnly = true)
    public HomeResponse home(Locale locale, UUID viewerId) {
        List<TaggedCard> tagged = taggedCards();
        List<SeriesCardDto> catalog = cardsOf(tagged);
        List<SeriesCardDto> visible = cardsOf(tagged.stream()
                .filter(card -> viewerId == null || !viewerId.equals(card.creatorId()))
                .toList());
        Map<String, Integer> catalogCountByLanguage = countByLanguage(catalog);
        boolean discoveryEnabled = readerFeatureGate.landingDiscoveryEnabled();
        HomeDiscoveryInstruction instruction = homeDiscoveryPolicy.decide(new HomeDiscoveryInput(
                discoveryEnabled && languageFilterProperties.enabled(),
                catalog.size(),
                languageFilterProperties.minimumCatalogSize(),
                languageFilterProperties.minimumSeriesPerLanguage(),
                catalogCountByLanguage
        ));
        List<String> filters = discoveryEnabled
                ? instruction.filters().stream().map(HomeFilter::name).toList()
                : List.of();
        List<LanguageOptionDto> languageOptions = discoveryEnabled
                && instruction.filters().contains(HomeFilter.LANGUAGE)
                ? toLanguageOptions(instruction.eligibleLanguageCodes(), countByLanguage(visible)).stream()
                        .filter(option -> option.seriesCount() > 0)
                        .toList()
                : List.of();
        return new HomeResponse(
                resolveTagline(locale),
                visible,
                List.of(),
                filters,
                languageOptions
        );
    }

    @Transactional(readOnly = true)
    public List<SeriesCardDto> recentlyUpdated() {
        return cardsOf(taggedCards());
    }

    @Transactional(readOnly = true)
    public SeriesDetailDto seriesBySlug(String slug) {
        return seriesBySlug(slug, null);
    }

    @Transactional(readOnly = true)
    public SeriesDetailDto seriesBySlug(String slug, UUID viewerId) {
        Series series = seriesRepository.findBySlug(slug)
                .orElseThrow(() -> notFound("Series not found"));
        List<Chapter> listed = listedPublishedChapters(series.getId());
        List<Chapter> intact = listed.stream()
                .filter(c -> mediaIntegrityService.isChapterMediaIntact(c.getId()))
                .toList();
        if (intact.isEmpty()) {
            long publishedCount = chapterRepository.countBySeriesIdAndState(
                    series.getId(),
                    ChapterState.PUBLISHED
            );
            if (publishedCount > 0) {
                throw unavailable(
                        "This series is temporarily unavailable while we restore chapter media."
                );
            }
            throw notFound("Series not found");
        }
        String creatorName = loadCreatorNames(List.of(series.getCreatorId()))
                .getOrDefault(series.getCreatorId(), "Creator");
        ScheduleStripDto schedule = toScheduleDto(series);
        List<ChapterSummaryDto> summaries = intact.stream()
                .map(c -> new ChapterSummaryDto(
                        c.getSlug(),
                        c.getTitle(),
                        c.getChapterNumber(),
                        c.getListedAt()
                ))
                .toList();
        return new SeriesDetailDto(
                series.getSlug(),
                series.getTitle(),
                creatorName,
                series.getDescription() == null ? "" : series.getDescription(),
                series.getGenres() == null ? List.of() : series.getGenres(),
                series.getContentLanguage(),
                ReaderPresentation.coverGradient(series.getSlug()),
                ReaderPresentation.coverUrl(series.getCoverStorageKey(), series.getVersion()),
                schedule.scheduleLabel(),
                schedule,
                series.getStatus().name(),
                lastUpdated(series),
                summaries.size(),
                summaries,
                viewerId != null && viewerId.equals(series.getCreatorId()),
                series.getRating(),
                series.getReaderCount(),
                series.isEditorsPick(),
                glimpsesFor(series.getId(), viewerId)
        );
    }

    private List<GlimpseDto> glimpsesFor(UUID seriesId, UUID viewerId) {
        List<Glimpse> glimpses = glimpseRepository.findBySeriesIdOrderByPostedAtAsc(seriesId);
        if (glimpses.isEmpty()) {
            return List.of();
        }
        List<UUID> glimpseIds = glimpses.stream().map(Glimpse::getId).toList();
        List<GlimpseImage> images = glimpseImageRepository.findByGlimpseIdInOrderBySortOrderAsc(glimpseIds);
        Map<UUID, ImageReactionState> reactions = glimpseReactionLookup.forImages(
                viewerId,
                images.stream().map(GlimpseImage::getId).toList()
        );
        Map<UUID, List<GlimpseImageDto>> imagesByGlimpse = new HashMap<>();
        for (GlimpseImage image : images) {
            ImageReactionState reaction = reactions.getOrDefault(
                    image.getId(),
                    new ImageReactionState(0, false)
            );
            imagesByGlimpse
                    .computeIfAbsent(image.getGlimpseId(), ignored -> new ArrayList<>())
                    .add(new GlimpseImageDto(
                            image.getId(),
                            ReaderPresentation.mediaUrl(image.getStorageKey()),
                            image.getSortOrder(),
                            reaction.count(),
                            reaction.reacted()
                    ));
        }
        return glimpses.stream()
                .map(glimpse -> new GlimpseDto(
                        glimpse.getId(),
                        glimpse.getTag(),
                        glimpse.getPostedAt(),
                        imagesByGlimpse.getOrDefault(glimpse.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChapterReaderDto chapterBySlug(String seriesSlug, String chapterSlug) {
        Series series = seriesRepository.findBySlug(seriesSlug)
                .orElseThrow(() -> notFound("Series not found"));
        Chapter chapter = chapterRepository
                .findBySeriesIdAndSlugAndStateAndListedAtIsNotNull(
                        series.getId(),
                        chapterSlug,
                        ChapterState.PUBLISHED
                )
                .orElse(null);
        if (chapter == null) {
            if (chapterRepository.findBySeriesIdAndSlugAndState(
                    series.getId(),
                    chapterSlug,
                    ChapterState.PUBLISHED
            ).isPresent()) {
                throw unavailable(
                        "This chapter is temporarily unavailable while we restore its pages."
                );
            }
            throw notFound("Chapter not found");
        }
        if (!mediaIntegrityService.isChapterMediaIntact(chapter.getId())) {
            throw unavailable(
                    "This chapter is temporarily unavailable while we restore its pages."
            );
        }
        List<ChapterPage> pages = chapterPageRepository.findByChapterIdOrderBySortOrderAsc(chapter.getId());
        List<ChapterPageDto> pageDtos = pages.stream()
                .map(p -> {
                    String key = ChapterMediaIntegrityService.effectiveStorageKey(p);
                    return new ChapterPageDto(
                            p.getSortOrder(),
                            ReaderPresentation.mediaUrl(key),
                            p.getWidth(),
                            p.getHeight()
                    );
                })
                .toList();
        return new ChapterReaderDto(
                series.getSlug(),
                series.getTitle(),
                chapter.getSlug(),
                chapter.getTitle(),
                chapter.getChapterNumber(),
                pageDtos
        );
    }

    private List<TaggedCard> taggedCards() {
        List<Series> seriesList = seriesRepository.findDiscoverableOrderByRecent();
        Map<UUID, String> creatorNames = loadCreatorNames(
                seriesList.stream().map(Series::getCreatorId).distinct().toList()
        );
        return seriesList.stream()
                .map(series -> new TaggedCard(
                        series.getCreatorId(),
                        toCard(series, creatorNames.getOrDefault(series.getCreatorId(), "Creator"))
                ))
                .filter(tagged -> tagged.card().chapterCount() > 0)
                .toList();
    }

    private static List<SeriesCardDto> cardsOf(List<TaggedCard> tagged) {
        return tagged.stream().map(TaggedCard::card).toList();
    }

    private SeriesCardDto toCard(Series series, String creatorName) {
        int chapterCount = (int) listedPublishedChapters(series.getId()).stream()
                .filter(c -> mediaIntegrityService.isChapterMediaIntact(c.getId()))
                .count();
        ScheduleStripDto schedule = toScheduleDto(series);
        return new SeriesCardDto(
                series.getSlug(),
                series.getTitle(),
                creatorName,
                series.getDescription() == null ? "" : series.getDescription(),
                series.getGenres() == null ? List.of() : series.getGenres(),
                series.getContentLanguage(),
                ReaderPresentation.coverGradient(series.getSlug()),
                ReaderPresentation.coverUrl(series.getCoverStorageKey(), series.getVersion()),
                schedule.scheduleLabel(),
                schedule,
                series.getStatus().name(),
                lastUpdated(series),
                chapterCount,
                series.getRating(),
                series.getReaderCount(),
                series.isEditorsPick()
        );
    }

    private ScheduleStripDto toScheduleDto(Series series) {
        ScheduleStripView view = seriesScheduleService.stripFor(series);
        return new ScheduleStripDto(
                view.headline(),
                view.scheduleLabel(),
                view.nextExpectedAt(),
                view.skipMessage(),
                view.status().name(),
                view.cadence() == null ? null : view.cadence().name(),
                view.periodDays(),
                view.releaseHourIst() == null ? null : view.releaseHourIst().intValue()
        );
    }

    private List<Chapter> listedPublishedChapters(UUID seriesId) {
        return chapterRepository.findBySeriesIdAndStateAndListedAtIsNotNullOrderByChapterNumberAsc(
                seriesId,
                ChapterState.PUBLISHED
        );
    }

    private static Instant lastUpdated(Series series) {
        return series.getLastPublishedAt() != null ? series.getLastPublishedAt() : series.getCreatedAt();
    }

    private Map<UUID, String> loadCreatorNames(List<UUID> creatorIds) {
        if (creatorIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", creatorIds.stream().map(id -> "?").toList());
        Object[] args = creatorIds.toArray();
        Map<UUID, String> names = new HashMap<>();
        jdbcTemplate.query(
                "SELECT id, COALESCE(NULLIF(display_name, ''), email) AS name FROM app_user WHERE id IN ("
                        + placeholders + ")",
                rs -> {
                    names.put(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("name")
                    );
                },
                args
        );
        return names;
    }

    private String resolveTagline(Locale locale) {
        Locale effective = locale == null ? Locale.ENGLISH : locale;
        return messageSource.getMessage(TAGLINE_MESSAGE_KEY, null, TAGLINE_FALLBACK, effective);
    }

    private static Map<String, Integer> countByLanguage(List<SeriesCardDto> series) {
        Map<String, Integer> counts = new HashMap<>();
        for (SeriesCardDto card : series) {
            String code = normalizeLanguageCode(card.contentLanguage());
            counts.merge(code, 1, Integer::sum);
        }
        return counts;
    }

    private static List<LanguageOptionDto> toLanguageOptions(
            List<String> eligibleCodes,
            Map<String, Integer> seriesCountByLanguage
    ) {
        return eligibleCodes.stream()
                .map(code -> {
                    Locale language = Locale.forLanguageTag(code);
                    String label = language.getDisplayLanguage(Locale.ENGLISH);
                    if (label == null || label.isBlank()) {
                        label = code;
                    }
                    String nativeLabel = language.getDisplayLanguage(language);
                    if (nativeLabel == null || nativeLabel.isBlank()) {
                        nativeLabel = label;
                    }
                    return new LanguageOptionDto(
                            code,
                            label,
                            nativeLabel,
                            seriesCountByLanguage.getOrDefault(code, 0)
                    );
                })
                .toList();
    }

    private static String normalizeLanguageCode(String contentLanguage) {
        if (contentLanguage == null || contentLanguage.isBlank()) {
            return "other";
        }
        return contentLanguage.trim().toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException unavailable(String message) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    private record TaggedCard(UUID creatorId, SeriesCardDto card) {
    }
}
