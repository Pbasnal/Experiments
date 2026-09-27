package com.amarkatha.engagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.analytics.AnalyticsEventType;
import com.amarkatha.analytics.AnalyticsIngestionService;
import com.amarkatha.engagement.domain.ReaderNotification;
import com.amarkatha.engagement.dto.MarkReadResponse;
import com.amarkatha.engagement.dto.NotificationListResponse;
import com.amarkatha.engagement.dto.ReadAllResponse;
import com.amarkatha.engagement.dto.UnreadCountDto;
import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.ReaderFeatureGate;
import com.amarkatha.shared.ReaderFeatureProperties;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReaderNotificationServiceTest {

    @Mock
    private ReaderNotificationRepository notificationRepository;
    @Mock
    private SeriesRepository seriesRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private AnalyticsIngestionService analyticsIngestionService;

    private ReaderNotificationService service;
    private UUID userId;
    private UUID seriesId;
    private UUID chapterId;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        service = new ReaderNotificationService(
                notificationRepository,
                seriesRepository,
                chapterRepository,
                new NotificationProperties(5_000, 10_000, 10, 50, 50, 100, 300_000),
                analyticsIngestionService,
                new ReaderFeatureGate(new ReaderFeatureProperties(true, true, true, true, false))
        );
        userId = UUID.randomUUID();
        seriesId = UUID.randomUUID();
        chapterId = UUID.randomUUID();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void listReturnsItemsAndUnreadCount() {
        ReaderNotification notification = ReaderNotification.chapterPublished(
                userId, seriesId, chapterId, "Title", "Message", "/read/s/demo/c/chapter-1", true
        );
        when(notificationRepository.findByUserIdAndInAppVisibleTrueOrderByCreatedAtDesc(
                eq(userId), any(Pageable.class)
        )).thenReturn(List.of(notification));
        when(notificationRepository.countByUserIdAndInAppVisibleTrueAndReadAtIsNull(userId)).thenReturn(1L);

        Series series = Series.create(UUID.randomUUID(), "demo", "Demo");
        Chapter chapter = Chapter.createDraft(seriesId, 1, "chapter-1", "Chapter 1");
        when(seriesRepository.findAllById(any())).thenReturn(List.of(series));
        when(chapterRepository.findAllById(any())).thenReturn(List.of(chapter));

        NotificationListResponse responseDto = service.list(userId, 20);

        assertEquals(1, responseDto.items().size());
        assertEquals(1L, responseDto.unreadCount());
        assertEquals("Title", responseDto.items().get(0).title());
        verify(notificationRepository).findByUserIdAndInAppVisibleTrueOrderByCreatedAtDesc(
                eq(userId), any(Pageable.class)
        );
        verify(notificationRepository).countByUserIdAndInAppVisibleTrueAndReadAtIsNull(userId);
    }

    @Test
    void listAndUnreadUseInAppVisibleQueriesOnly() {
        when(notificationRepository.findByUserIdAndInAppVisibleTrueOrderByCreatedAtDesc(
                eq(userId), any(Pageable.class)
        )).thenReturn(List.of());
        when(notificationRepository.countByUserIdAndInAppVisibleTrueAndReadAtIsNull(userId)).thenReturn(0L);

        NotificationListResponse list = service.list(userId, null);
        UnreadCountDto unread = service.unread(userId);

        assertEquals(0, list.items().size());
        assertEquals(0L, list.unreadCount());
        assertEquals(0L, unread.unreadCount());
        verify(notificationRepository).findByUserIdAndInAppVisibleTrueOrderByCreatedAtDesc(
                eq(userId), any(Pageable.class)
        );
        verify(notificationRepository, org.mockito.Mockito.atLeastOnce())
                .countByUserIdAndInAppVisibleTrueAndReadAtIsNull(userId);
    }

    @Test
    void markReadSetsTimestampAndRecordsEvent() {
        ReaderNotification notification = ReaderNotification.chapterPublished(
                userId, seriesId, chapterId, "Title", "Message", "/href", true
        );
        when(notificationRepository.findByIdAndUserIdAndInAppVisibleTrue(notification.getId(), userId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MarkReadResponse markResponse = service.markRead(userId, notification.getId(), request, response);

        assertEquals(notification.getId(), markResponse.id());
        assertNotNull(markResponse.readAt());
        verify(notificationRepository).save(notification);
        verify(analyticsIngestionService).recordServerEvent(
                eq(AnalyticsEventType.NOTIFICATION_READ),
                eq(seriesId),
                eq(chapterId),
                any(String.class),
                eq("app"),
                isNull()
        );
    }

    @Test
    void markReadSkipsEventWhenAlreadyRead() {
        ReaderNotification notification = ReaderNotification.chapterPublished(
                userId, seriesId, chapterId, "Title", "Message", "/href", true
        );
        notification.markRead(Instant.now());
        when(notificationRepository.findByIdAndUserIdAndInAppVisibleTrue(notification.getId(), userId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markRead(userId, notification.getId(), request, response);

        verify(analyticsIngestionService, never()).recordServerEvent(
                any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void markAllReadDelegatesToRepository() {
        when(notificationRepository.markAllRead(eq(userId), any(Instant.class))).thenReturn(3);
        ReadAllResponse readAll = service.markAllRead(userId, request, response);
        assertEquals(3, readAll.markedRead());
        verify(analyticsIngestionService).recordServerEvent(
                eq(AnalyticsEventType.NOTIFICATION_READ),
                isNull(),
                isNull(),
                any(String.class),
                eq("app"),
                eq("{\"bulk\":true,\"count\":3}")
        );
    }

    @Test
    void unreadCount() {
        when(notificationRepository.countByUserIdAndInAppVisibleTrueAndReadAtIsNull(userId)).thenReturn(4L);
        UnreadCountDto dto = service.unread(userId);
        assertEquals(4L, dto.unreadCount());
    }

    @Test
    void listBlockedWhenFeatureDisabled() {
        service = new ReaderNotificationService(
                notificationRepository,
                seriesRepository,
                chapterRepository,
                new NotificationProperties(5_000, 10_000, 10, 50, 50, 100, 300_000),
                analyticsIngestionService,
                new ReaderFeatureGate(new ReaderFeatureProperties(true, true, true, false, false))
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.list(userId, 10)
        );
        assertEquals(404, ex.getStatusCode().value());
    }
}
