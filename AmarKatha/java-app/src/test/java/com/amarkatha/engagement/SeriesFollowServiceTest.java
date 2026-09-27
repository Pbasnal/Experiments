package com.amarkatha.engagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.analytics.AnalyticsEventType;
import com.amarkatha.analytics.AnalyticsIngestionService;
import com.amarkatha.engagement.domain.SeriesFollow;
import com.amarkatha.engagement.dto.FollowStateDto;
import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.SeriesScheduleService;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.ReaderFeatureGate;
import com.amarkatha.shared.ReaderFeatureProperties;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SeriesFollowServiceTest {

    @Mock
    private SeriesFollowRepository seriesFollowRepository;
    @Mock
    private SeriesRepository seriesRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private ReaderProgressRepository readerProgressRepository;
    @Mock
    private SeriesScheduleService seriesScheduleService;
    @Mock
    private AnalyticsIngestionService analyticsIngestionService;

    private SeriesFollowService seriesFollowService;
    private UUID userId;
    private Series series;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        seriesFollowService = new SeriesFollowService(
                seriesFollowRepository,
                seriesRepository,
                chapterRepository,
                readerProgressRepository,
                seriesScheduleService,
                analyticsIngestionService,
                new ReaderFeatureGate(new ReaderFeatureProperties(true, true, true, true, false))
        );
        userId = UUID.randomUUID();
        series = Series.create(UUID.randomUUID(), "demo-series", "Demo");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void followIsIdempotentWhenAlreadyFollowing() {
        when(seriesRepository.findBySlug("demo-series")).thenReturn(Optional.of(series));
        when(seriesFollowRepository.existsByUserIdAndSeriesId(userId, series.getId())).thenReturn(true);

        FollowStateDto state = seriesFollowService.follow(userId, "demo-series", request, response);

        assertTrue(state.followed());
        assertEquals("demo-series", state.seriesSlug());
        verify(seriesFollowRepository, never()).save(any());
        verify(analyticsIngestionService, never()).recordServerEvent(
                any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void followCreatesRowAndRecordsEventWhenMissing() {
        when(seriesRepository.findBySlug("demo-series")).thenReturn(Optional.of(series));
        when(seriesFollowRepository.existsByUserIdAndSeriesId(userId, series.getId())).thenReturn(false);

        FollowStateDto state = seriesFollowService.follow(userId, "demo-series", request, response);

        assertTrue(state.followed());
        ArgumentCaptor<SeriesFollow> captor = ArgumentCaptor.forClass(SeriesFollow.class);
        verify(seriesFollowRepository).save(captor.capture());
        assertEquals(userId, captor.getValue().getUserId());
        assertEquals(series.getId(), captor.getValue().getSeriesId());
        verify(analyticsIngestionService).recordServerEvent(
                eq(AnalyticsEventType.FOLLOW),
                eq(series.getId()),
                isNull(),
                any(String.class),
                eq("app"),
                isNull()
        );
    }

    @Test
    void unfollowRecordsEventOnlyWhenWasFollowing() {
        when(seriesRepository.findBySlug("demo-series")).thenReturn(Optional.of(series));
        when(seriesFollowRepository.existsByUserIdAndSeriesId(userId, series.getId())).thenReturn(true);

        FollowStateDto state = seriesFollowService.unfollow(userId, "demo-series", request, response);

        assertFalse(state.followed());
        verify(seriesFollowRepository).deleteByUserIdAndSeriesId(userId, series.getId());
        verify(analyticsIngestionService).recordServerEvent(
                eq(AnalyticsEventType.UNFOLLOW),
                eq(series.getId()),
                isNull(),
                any(String.class),
                eq("app"),
                isNull()
        );
    }

    @Test
    void unfollowSkipsEventWhenNotFollowing() {
        when(seriesRepository.findBySlug("demo-series")).thenReturn(Optional.of(series));
        when(seriesFollowRepository.existsByUserIdAndSeriesId(userId, series.getId())).thenReturn(false);

        seriesFollowService.unfollow(userId, "demo-series", request, response);

        verify(analyticsIngestionService, never()).recordServerEvent(
                any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void followStateAnonymousIsFalse() {
        when(seriesRepository.findBySlug("demo-series")).thenReturn(Optional.of(series));

        FollowStateDto state = seriesFollowService.followState(null, "demo-series");

        assertFalse(state.followed());
    }

    @Test
    void followRejectsOwnSeries() {
        Series owned = Series.create(userId, "demo-series", "Demo");
        when(seriesRepository.findBySlug("demo-series")).thenReturn(Optional.of(owned));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> seriesFollowService.follow(userId, "demo-series", request, response)
        );

        assertEquals(403, ex.getStatusCode().value());
        verify(seriesFollowRepository, never()).save(any());
    }

    @Test
    void followRejectsUnknownSeries() {
        when(seriesRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> seriesFollowService.follow(userId, "missing", request, response)
        );
    }

    @Test
    void followBlockedWhenFeatureDisabled() {
        seriesFollowService = new SeriesFollowService(
                seriesFollowRepository,
                seriesRepository,
                chapterRepository,
                readerProgressRepository,
                seriesScheduleService,
                analyticsIngestionService,
                new ReaderFeatureGate(new ReaderFeatureProperties(true, true, false, true, false))
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> seriesFollowService.follow(userId, "demo-series", request, response)
        );
        assertEquals(404, ex.getStatusCode().value());
    }
}
