package com.amarkatha.engagement;

import com.amarkatha.analytics.AnalyticsEventType;
import com.amarkatha.analytics.AnalyticsIngestionService;
import com.amarkatha.engagement.domain.SeriesFollow;
import com.amarkatha.engagement.dto.FollowStateDto;
import com.amarkatha.engagement.dto.FollowedSeriesDto;
import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.SeriesCoverPresentation;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.SeriesScheduleService;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.ReaderFeatureGate;
import com.amarkatha.shared.domain.ChapterState;
import com.amarkatha.shared.web.ReaderIdCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SeriesFollowService {

    private final SeriesFollowRepository seriesFollowRepository;
    private final SeriesRepository seriesRepository;
    private final ChapterRepository chapterRepository;
    private final ReaderProgressRepository readerProgressRepository;
    private final SeriesScheduleService seriesScheduleService;
    private final AnalyticsIngestionService analyticsIngestionService;
    private final ReaderFeatureGate readerFeatureGate;

    public SeriesFollowService(
            SeriesFollowRepository seriesFollowRepository,
            SeriesRepository seriesRepository,
            ChapterRepository chapterRepository,
            ReaderProgressRepository readerProgressRepository,
            SeriesScheduleService seriesScheduleService,
            AnalyticsIngestionService analyticsIngestionService,
            ReaderFeatureGate readerFeatureGate
    ) {
        this.seriesFollowRepository = seriesFollowRepository;
        this.seriesRepository = seriesRepository;
        this.chapterRepository = chapterRepository;
        this.readerProgressRepository = readerProgressRepository;
        this.seriesScheduleService = seriesScheduleService;
        this.analyticsIngestionService = analyticsIngestionService;
        this.readerFeatureGate = readerFeatureGate;
    }

    @Transactional(readOnly = true)
    public FollowStateDto followState(UUID userIdOrNull, String seriesSlug) {
        Series series = requireSeries(seriesSlug);
        boolean followed = userIdOrNull != null
                && seriesFollowRepository.existsByUserIdAndSeriesId(userIdOrNull, series.getId());
        return new FollowStateDto(series.getSlug(), followed);
    }

    @Transactional
    public FollowStateDto follow(
            UUID userId,
            String seriesSlug,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        readerFeatureGate.requireFollows();
        Series series = requireSeries(seriesSlug);
        if (userId.equals(series.getCreatorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can't follow your own series");
        }
        if (seriesFollowRepository.existsByUserIdAndSeriesId(userId, series.getId())) {
            return new FollowStateDto(series.getSlug(), true);
        }
        boolean created = false;
        try {
            seriesFollowRepository.save(SeriesFollow.create(userId, series.getId()));
            created = true;
        } catch (DataIntegrityViolationException ignored) {
            // concurrent idempotent create
        }
        if (created) {
            analyticsIngestionService.recordServerEvent(
                    AnalyticsEventType.FOLLOW,
                    series.getId(),
                    null,
                    ReaderIdCookie.ensure(request, response),
                    "app",
                    null
            );
        }
        return new FollowStateDto(series.getSlug(), true);
    }

    @Transactional
    public FollowStateDto unfollow(
            UUID userId,
            String seriesSlug,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        readerFeatureGate.requireFollows();
        Series series = requireSeries(seriesSlug);
        boolean wasFollowing = seriesFollowRepository.existsByUserIdAndSeriesId(userId, series.getId());
        seriesFollowRepository.deleteByUserIdAndSeriesId(userId, series.getId());
        if (wasFollowing) {
            analyticsIngestionService.recordServerEvent(
                    AnalyticsEventType.UNFOLLOW,
                    series.getId(),
                    null,
                    ReaderIdCookie.ensure(request, response),
                    "app",
                    null
            );
        }
        return new FollowStateDto(series.getSlug(), false);
    }

    @Transactional(readOnly = true)
    public long followingCount(UUID userId) {
        if (!readerFeatureGate.followsEnabled()) {
            return 0L;
        }
        return seriesFollowRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(follow -> !ownsSeries(userId, follow.getSeriesId()))
                .count();
    }

    @Transactional(readOnly = true)
    public List<FollowedSeriesDto> listFollowing(UUID userId) {
        if (!readerFeatureGate.followsEnabled()) {
            return List.of();
        }
        List<SeriesFollow> follows = seriesFollowRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (follows.isEmpty()) {
            return List.of();
        }
        List<FollowedSeriesDto> result = new ArrayList<>(follows.size());
        for (SeriesFollow follow : follows) {
            Optional<Series> seriesOpt = seriesRepository.findById(follow.getSeriesId());
            if (seriesOpt.isEmpty()) {
                continue;
            }
            Series series = seriesOpt.get();
            if (userId.equals(series.getCreatorId())) {
                continue;
            }
            Chapter latest = latestListedChapter(series.getId()).orElse(null);
            Chapter lastRead = readerProgressRepository.findByUserIdAndSeriesId(userId, series.getId())
                    .flatMap(progress -> chapterRepository.findById(progress.getLastChapterId()))
                    .orElse(null);
            boolean hasUnread = latest != null && (lastRead == null
                    || latest.getChapterNumber() > lastRead.getChapterNumber());
            var strip = seriesScheduleService.stripFor(series);
            result.add(new FollowedSeriesDto(
                    series.getSlug(),
                    series.getTitle(),
                    SeriesCoverPresentation.coverGradient(series.getSlug()),
                    SeriesCoverPresentation.coverUrl(series.getCoverStorageKey(), series.getVersion()),
                    strip.scheduleLabel(),
                    series.getStatus().name(),
                    latest == null ? null : latest.getSlug(),
                    latest == null ? null : latest.getTitle(),
                    latest == null ? null : latest.getChapterNumber(),
                    lastRead == null ? null : lastRead.getSlug(),
                    hasUnread,
                    follow.getCreatedAt()
            ));
        }
        return result;
    }

    private Optional<Chapter> latestListedChapter(UUID seriesId) {
        List<Chapter> chapters = chapterRepository
                .findBySeriesIdAndStateAndListedAtIsNotNullOrderByChapterNumberAsc(
                        seriesId,
                        ChapterState.PUBLISHED
                );
        if (chapters.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(chapters.get(chapters.size() - 1));
    }

    private boolean ownsSeries(UUID userId, UUID seriesId) {
        return seriesRepository.findById(seriesId)
                .map(series -> userId.equals(series.getCreatorId()))
                .orElse(false);
    }

    private Series requireSeries(String seriesSlug) {
        if (seriesSlug == null || seriesSlug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seriesSlug required");
        }
        return seriesRepository.findBySlug(seriesSlug.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Series not found"));
    }
}
