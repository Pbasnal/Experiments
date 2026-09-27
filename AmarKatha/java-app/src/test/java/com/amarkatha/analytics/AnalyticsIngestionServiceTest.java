package com.amarkatha.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.SeriesRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Map;
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
class AnalyticsIngestionServiceTest {

    @Mock
    private AnalyticsEventRepository analyticsEventRepository;
    @Mock
    private SeriesRepository seriesRepository;
    @Mock
    private ChapterRepository chapterRepository;

    private AnalyticsIngestionService service;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        service = new AnalyticsIngestionService(
                analyticsEventRepository,
                seriesRepository,
                chapterRepository,
                new ObjectMapper()
        );
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        org.mockito.Mockito.lenient()
                .when(analyticsEventRepository.save(any(AnalyticsEvent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void clientLandingViewPersistsWithoutSeries() {
        service.recordClientEvent(
                new ProductEventRequest("LANDING_VIEW", null, null, "homepage", null),
                request,
                response
        );

        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsEventRepository).save(captor.capture());
        AnalyticsEvent event = captor.getValue();
        assertEquals(AnalyticsEventType.LANDING_VIEW, event.getType());
        assertNull(event.getSeriesId());
        assertNull(event.getChapterId());
        assertEquals("homepage", event.getReferrer());
        assertTrue(event.getReaderId().length() == 36);
    }

    @Test
    void clientRejectsServerOwnedFollowType() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.recordClientEvent(
                        new ProductEventRequest("FOLLOW", "demo", null, "app", null),
                        request,
                        response
                )
        );
        assertEquals(400, ex.getStatusCode().value());
        verify(analyticsEventRepository, never()).save(any());
    }

    @Test
    void webVitalRequiresNumericValue() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.recordClientEvent(
                        new ProductEventRequest("WEB_VITAL_LCP", null, null, "direct", Map.of("rating", "good")),
                        request,
                        response
                )
        );
        verify(analyticsEventRepository, never()).save(any());
    }

    @Test
    void webVitalPersistsAllowlistedMeta() {
        service.recordClientEvent(
                new ProductEventRequest(
                        "WEB_VITAL_INP",
                        null,
                        null,
                        "direct",
                        Map.of("value", 120.5, "rating", "good", "email", "leak@example.com")
                ),
                request,
                response
        );

        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsEventRepository).save(captor.capture());
        String meta = captor.getValue().getMetaJson();
        assertTrue(meta.contains("120.5"));
        assertTrue(meta.contains("good"));
        assertFalse(meta.contains("email"));
        assertFalse(meta.contains("leak"));
    }

    @Test
    void frontendExceptionRedactsEmailLikeText() {
        service.recordClientEvent(
                new ProductEventRequest(
                        "FRONTEND_EXCEPTION",
                        null,
                        null,
                        "direct",
                        Map.of("name", "TypeError", "message", "failed for user@example.com")
                ),
                request,
                response
        );

        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsEventRepository).save(captor.capture());
        assertTrue(captor.getValue().getMetaJson().contains("[redacted]"));
        assertFalse(captor.getValue().getMetaJson().contains("user@example.com"));
    }

    @Test
    void frontendExceptionRedactsUrlsTokensPhonesAndLongIds() {
        String longId = "a".repeat(40);
        service.recordClientEvent(
                new ProductEventRequest(
                        "FRONTEND_EXCEPTION",
                        null,
                        null,
                        "direct",
                        Map.of(
                                "name", "Error",
                                "message",
                                "boom https://x.test/cb?token=abc Bearer eyJhbGciOiJIUz phone +1 (555) 123-4567 id=" + longId
                        )
                ),
                request,
                response
        );

        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsEventRepository).save(captor.capture());
        String meta = captor.getValue().getMetaJson();
        assertTrue(meta.contains("[redacted-url]"));
        assertTrue(meta.contains("[redacted-secret]"));
        assertTrue(meta.contains("[redacted-phone]"));
        assertTrue(meta.contains("[redacted-id]"));
        assertFalse(meta.contains("https://x.test"));
        assertFalse(meta.contains("eyJhbGciOiJIUz"));
        assertFalse(meta.contains("555) 123"));
        assertFalse(meta.contains(longId));
    }

    @Test
    void unknownTypeRejected() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.recordClientEvent(
                        new ProductEventRequest("NOT_A_REAL_EVENT", null, null, null, null),
                        request,
                        response
                )
        );
    }

    @Test
    void reusesExistingReaderCookie() {
        String readerId = UUID.randomUUID().toString();
        request.setCookies(new Cookie("reader_id", readerId));

        service.recordClientEvent(
                new ProductEventRequest("SIGNUP_CLICK", null, null, "direct", null),
                request,
                response
        );

        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsEventRepository).save(captor.capture());
        assertEquals(readerId, captor.getValue().getReaderId());
    }

    @Test
    void seriesSlugResolvedWhenProvided() {
        var series = com.amarkatha.publishing.domain.Series.create(UUID.randomUUID(), "demo", "Demo");
        when(seriesRepository.findBySlug("demo")).thenReturn(Optional.of(series));

        service.recordClientEvent(
                new ProductEventRequest("NOTIFICATION_CLICK", "demo", null, "app", null),
                request,
                response
        );

        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsEventRepository).save(captor.capture());
        assertEquals(series.getId(), captor.getValue().getSeriesId());
    }
}
