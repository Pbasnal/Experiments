package com.amarkatha.publishing;

import com.amarkatha.publishing.domain.ScheduleEvent;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.scheduling.ScheduleActionResult;
import com.amarkatha.scheduling.ScheduleActions;
import com.amarkatha.scheduling.ScheduleException;
import com.amarkatha.scheduling.ScheduleState;
import com.amarkatha.scheduling.ScheduleStripView;
import com.amarkatha.shared.domain.ChapterState;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeriesScheduleService {

    private final SeriesRepository seriesRepository;
    private final SeriesService seriesService;
    private final ChapterRepository chapterRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final ScheduleActions scheduleActions;

    public SeriesScheduleService(
            SeriesRepository seriesRepository,
            SeriesService seriesService,
            ChapterRepository chapterRepository,
            ScheduleEventRepository scheduleEventRepository,
            ScheduleActions scheduleActions
    ) {
        this.seriesRepository = seriesRepository;
        this.seriesService = seriesService;
        this.chapterRepository = chapterRepository;
        this.scheduleEventRepository = scheduleEventRepository;
        this.scheduleActions = scheduleActions;
    }

    @Transactional(readOnly = true)
    public ScheduleState currentState(Series series) {
        return toState(series);
    }

    @Transactional(readOnly = true)
    public ScheduleStripView stripFor(Series series) {
        return ScheduleStripView.from(toState(series));
    }

    @Transactional(readOnly = true)
    public boolean shouldPromptCadence(UUID seriesId, UUID creatorId) {
        Series series = seriesService.requireOwned(seriesId, creatorId);
        if (series.getPeriodDays() != null && series.getPeriodDays() > 0) {
            return false;
        }
        long published = chapterRepository.countBySeriesIdAndState(seriesId, ChapterState.PUBLISHED);
        return published >= 2;
    }

    @Transactional
    public Series activateCadence(
            UUID seriesId,
            UUID creatorId,
            int periodDays,
            int isoDayOfWeek,
            int releaseHourIst
    ) {
        return mutate(
                seriesId,
                creatorId,
                state -> scheduleActions.activate(state, periodDays, isoDayOfWeek, releaseHourIst)
        );
    }

    @Transactional
    public Series clearCadence(UUID seriesId, UUID creatorId) {
        return mutate(seriesId, creatorId, scheduleActions::clearCadence);
    }

    @Transactional
    public Series skip(UUID seriesId, UUID creatorId, String message) {
        return mutate(seriesId, creatorId, state -> scheduleActions.skip(state, message));
    }

    @Transactional
    public Series hiatus(UUID seriesId, UUID creatorId) {
        return mutate(seriesId, creatorId, scheduleActions::hiatus);
    }

    @Transactional
    public Series resume(UUID seriesId, UUID creatorId) {
        return mutate(seriesId, creatorId, scheduleActions::resume);
    }

    @Transactional
    public Series onChapterPublished(UUID seriesId, UUID creatorId, Instant publishedAt) {
        return mutate(seriesId, creatorId, state -> scheduleActions.onPublish(state, publishedAt));
    }

    private Series mutate(UUID seriesId, UUID creatorId, java.util.function.Function<ScheduleState, ScheduleActionResult> action) {
        Series series = seriesService.requireOwned(seriesId, creatorId);
        try {
            ScheduleActionResult result = action.apply(toState(series));
            apply(series, result.state());
            Series saved = seriesRepository.save(series);
            scheduleEventRepository.save(ScheduleEvent.create(
                    series.getId(),
                    result.eventType(),
                    result.message(),
                    result.expectedChange().previousNextExpectedAt(),
                    result.expectedChange().newNextExpectedAt(),
                    creatorId
            ));
            return saved;
        } catch (ScheduleException ex) {
            throw new ScheduleServiceException(ex.getMessage(), ex);
        } catch (OptimisticLockException | OptimisticLockingFailureException ex) {
            throw new ScheduleServiceException(
                    "Schedule was updated elsewhere. Refresh and try again.",
                    ex
            );
        }
    }

    static ScheduleState toState(Series series) {
        return new ScheduleState(
                series.getStatus(),
                series.getCadence(),
                series.getPeriodDays() == null ? null : series.getPeriodDays().intValue(),
                series.getDayOfWeek(),
                series.getReleaseHourIst(),
                series.getNextExpectedAt(),
                series.getLastPublishedAt(),
                series.getSkipMessage()
        );
    }

    private static void apply(Series series, ScheduleState state) {
        series.applySchedule(
                state.status(),
                state.cadence(),
                state.periodDays(),
                state.dayOfWeek(),
                state.releaseHourIst(),
                state.nextExpectedAt(),
                state.lastPublishedAt(),
                state.skipMessage()
        );
    }
}
