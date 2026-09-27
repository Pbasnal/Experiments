package com.amarkatha.engagement;

import com.amarkatha.engagement.domain.ReaderProgress;
import com.amarkatha.engagement.dto.ProgressUpdateRequest;
import com.amarkatha.engagement.dto.ReaderProgressDto;
import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.SeriesCoverPresentation;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.domain.ChapterState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReaderProgressService {

    private final ReaderProgressRepository readerProgressRepository;
    private final SeriesRepository seriesRepository;
    private final ChapterRepository chapterRepository;

    public ReaderProgressService(
            ReaderProgressRepository readerProgressRepository,
            SeriesRepository seriesRepository,
            ChapterRepository chapterRepository
    ) {
        this.readerProgressRepository = readerProgressRepository;
        this.seriesRepository = seriesRepository;
        this.chapterRepository = chapterRepository;
    }

    @Transactional(readOnly = true)
    public List<ReaderProgressDto> listProgress(UUID userId) {
        List<ReaderProgress> rows = readerProgressRepository.findByUserIdOrderByLastReadAtDesc(userId);
        List<ReaderProgressDto> result = new ArrayList<>(rows.size());
        for (ReaderProgress progress : rows) {
            Series series = seriesRepository.findById(progress.getSeriesId()).orElse(null);
            Chapter chapter = chapterRepository.findById(progress.getLastChapterId()).orElse(null);
            if (series == null || chapter == null) {
                continue;
            }
            result.add(toDto(series, chapter, progress.getLastReadAt()));
        }
        return result;
    }

    @Transactional
    public ReaderProgressDto recordRead(UUID userId, ProgressUpdateRequest request) {
        if (request == null
                || request.seriesSlug() == null || request.seriesSlug().isBlank()
                || request.chapterSlug() == null || request.chapterSlug().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seriesSlug and chapterSlug required");
        }
        return recordRead(userId, request.seriesSlug().trim(), request.chapterSlug().trim());
    }

    @Transactional
    public ReaderProgressDto recordRead(UUID userId, String seriesSlug, String chapterSlug) {
        Series series = seriesRepository.findBySlug(seriesSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Series not found"));
        Chapter chapter = chapterRepository
                .findBySeriesIdAndSlugAndStateAndListedAtIsNotNull(
                        series.getId(),
                        chapterSlug,
                        ChapterState.PUBLISHED
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));

        Instant now = Instant.now();
        ReaderProgress progress = readerProgressRepository.findByUserIdAndSeriesId(userId, series.getId())
                .orElseGet(() -> ReaderProgress.create(userId, series.getId(), chapter.getId()));
        progress.markRead(chapter.getId(), now);
        readerProgressRepository.save(progress);
        return toDto(series, chapter, now);
    }

    private static ReaderProgressDto toDto(Series series, Chapter chapter, Instant lastReadAt) {
        return new ReaderProgressDto(
                series.getSlug(),
                series.getTitle(),
                SeriesCoverPresentation.coverGradient(series.getSlug()),
                SeriesCoverPresentation.coverUrl(series.getCoverStorageKey(), series.getVersion()),
                chapter.getSlug(),
                chapter.getTitle(),
                chapter.getChapterNumber(),
                lastReadAt
        );
    }
}
