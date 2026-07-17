package com.amarkatha.reader;

import com.amarkatha.publishing.ChapterMediaIntegrityService;
import com.amarkatha.publishing.ChapterPageRepository;
import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.ChapterPage;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.reader.dto.ChapterPageDto;
import com.amarkatha.reader.dto.ChapterReaderDto;
import com.amarkatha.reader.dto.ChapterSummaryDto;
import com.amarkatha.reader.dto.SeriesCardDto;
import com.amarkatha.reader.dto.SeriesDetailDto;
import com.amarkatha.shared.domain.ChapterState;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReaderCatalogService {

    private final SeriesRepository seriesRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final ChapterMediaIntegrityService mediaIntegrityService;
    private final JdbcTemplate jdbcTemplate;

    public ReaderCatalogService(
            SeriesRepository seriesRepository,
            ChapterRepository chapterRepository,
            ChapterPageRepository chapterPageRepository,
            ChapterMediaIntegrityService mediaIntegrityService,
            JdbcTemplate jdbcTemplate
    ) {
        this.seriesRepository = seriesRepository;
        this.chapterRepository = chapterRepository;
        this.chapterPageRepository = chapterPageRepository;
        this.mediaIntegrityService = mediaIntegrityService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<SeriesCardDto> recentlyUpdated() {
        List<Series> seriesList = seriesRepository.findDiscoverableOrderByRecent();
        Map<UUID, String> creatorNames = loadCreatorNames(
                seriesList.stream().map(Series::getCreatorId).distinct().toList()
        );
        return seriesList.stream()
                .map(series -> toCard(series, creatorNames.getOrDefault(series.getCreatorId(), "Creator")))
                .filter(card -> card.chapterCount() > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeriesDetailDto seriesBySlug(String slug) {
        Series series = seriesRepository.findBySlug(slug)
                .orElseThrow(() -> notFound("Series not found"));
        List<Chapter> chapters = listedPublishedChapters(series.getId()).stream()
                .filter(c -> mediaIntegrityService.isChapterMediaIntact(c.getId()))
                .toList();
        if (chapters.isEmpty()) {
            throw notFound("Series not found");
        }
        String creatorName = loadCreatorNames(List.of(series.getCreatorId()))
                .getOrDefault(series.getCreatorId(), "Creator");
        List<ChapterSummaryDto> summaries = chapters.stream()
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
                ReaderPresentation.scheduleLabel(series),
                series.getStatus().name(),
                lastUpdated(series),
                summaries.size(),
                summaries
        );
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
                .orElseThrow(() -> notFound("Chapter not found"));
        if (!mediaIntegrityService.isChapterMediaIntact(chapter.getId())) {
            throw notFound("Chapter not found");
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

    private SeriesCardDto toCard(Series series, String creatorName) {
        int chapterCount = (int) listedPublishedChapters(series.getId()).stream()
                .filter(c -> mediaIntegrityService.isChapterMediaIntact(c.getId()))
                .count();
        return new SeriesCardDto(
                series.getSlug(),
                series.getTitle(),
                creatorName,
                series.getDescription() == null ? "" : series.getDescription(),
                series.getGenres() == null ? List.of() : series.getGenres(),
                series.getContentLanguage(),
                ReaderPresentation.coverGradient(series.getSlug()),
                ReaderPresentation.scheduleLabel(series),
                series.getStatus().name(),
                lastUpdated(series),
                chapterCount
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

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
