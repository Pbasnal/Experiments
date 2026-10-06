package com.amarkatha.engagement;

import com.amarkatha.analytics.AnalyticsEventType;
import com.amarkatha.analytics.AnalyticsIngestionService;
import com.amarkatha.engagement.domain.ReaderNotification;
import com.amarkatha.engagement.dto.MarkReadResponse;
import com.amarkatha.engagement.dto.NotificationListResponse;
import com.amarkatha.engagement.dto.ReadAllResponse;
import com.amarkatha.engagement.dto.ReaderNotificationDto;
import com.amarkatha.engagement.dto.UnreadCountDto;
import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.ReaderFeatureGate;
import com.amarkatha.shared.web.ReaderIdCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReaderNotificationService {

    private final ReaderNotificationRepository notificationRepository;
    private final SeriesRepository seriesRepository;
    private final ChapterRepository chapterRepository;
    private final NotificationProperties properties;
    private final AnalyticsIngestionService analyticsIngestionService;
    private final ReaderFeatureGate readerFeatureGate;

    public ReaderNotificationService(
            ReaderNotificationRepository notificationRepository,
            SeriesRepository seriesRepository,
            ChapterRepository chapterRepository,
            NotificationProperties properties,
            AnalyticsIngestionService analyticsIngestionService,
            ReaderFeatureGate readerFeatureGate
    ) {
        this.notificationRepository = notificationRepository;
        this.seriesRepository = seriesRepository;
        this.chapterRepository = chapterRepository;
        this.properties = properties;
        this.analyticsIngestionService = analyticsIngestionService;
        this.readerFeatureGate = readerFeatureGate;
    }

    @Transactional(readOnly = true)
    public NotificationListResponse list(UUID userId, Integer limit) {
        readerFeatureGate.requireInAppNotifications();
        int pageSize = clampLimit(limit);
        List<ReaderNotification> notifications = notificationRepository
                .findByUserIdAndInAppVisibleTrueOrderByCreatedAtDesc(userId, PageRequest.of(0, pageSize));
        return new NotificationListResponse(toDtos(notifications), unreadCount(userId));
    }

    @Transactional(readOnly = true)
    public UnreadCountDto unread(UUID userId) {
        if (!readerFeatureGate.inAppNotificationsEnabled()) {
            return new UnreadCountDto(0L);
        }
        return new UnreadCountDto(unreadCount(userId));
    }

    @Transactional
    public MarkReadResponse markRead(
            UUID userId,
            UUID notificationId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        readerFeatureGate.requireInAppNotifications();
        ReaderNotification notification = notificationRepository
                .findByIdAndUserIdAndInAppVisibleTrue(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        boolean wasUnread = notification.getReadAt() == null;
        Instant now = Instant.now();
        notification.markRead(now);
        notificationRepository.save(notification);
        if (wasUnread) {
            analyticsIngestionService.recordServerEvent(
                    AnalyticsEventType.NOTIFICATION_READ,
                    notification.getSeriesId(),
                    notification.getChapterId(),
                    ReaderIdCookie.ensure(request, response),
                    "app",
                    null
            );
        }
        return new MarkReadResponse(notification.getId(), notification.getReadAt());
    }

    @Transactional
    public ReadAllResponse markAllRead(
            UUID userId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        readerFeatureGate.requireInAppNotifications();
        int marked = notificationRepository.markAllRead(userId, Instant.now());
        if (marked > 0) {
            analyticsIngestionService.recordServerEvent(
                    AnalyticsEventType.NOTIFICATION_READ,
                    null,
                    null,
                    ReaderIdCookie.ensure(request, response),
                    "app",
                    "{\"bulk\":true,\"count\":" + marked + "}"
            );
        }
        return new ReadAllResponse(marked);
    }

    private long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndInAppVisibleTrueAndReadAtIsNull(userId);
    }

    private int clampLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return properties.listLimitDefault();
        }
        return Math.min(limit, properties.listLimitMax());
    }

    private List<ReaderNotificationDto> toDtos(List<ReaderNotification> notifications) {
        if (notifications.isEmpty()) {
            return List.of();
        }
        Map<UUID, Series> seriesById = seriesRepository.findAllById(
                        notifications.stream().map(ReaderNotification::getSeriesId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(Series::getId, Function.identity()));
        List<UUID> chapterIds = notifications.stream()
                .map(ReaderNotification::getChapterId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Chapter> chaptersById = chapterIds.isEmpty()
                ? Map.of()
                : chapterRepository.findAllById(chapterIds).stream()
                        .collect(Collectors.toMap(Chapter::getId, Function.identity()));

        List<ReaderNotificationDto> items = new ArrayList<>(notifications.size());
        for (ReaderNotification notification : notifications) {
            Series series = seriesById.get(notification.getSeriesId());
            Chapter chapter = chaptersById.get(notification.getChapterId());
            items.add(new ReaderNotificationDto(
                    notification.getId(),
                    notification.getType(),
                    series == null ? null : series.getSlug(),
                    series == null ? null : series.getTitle(),
                    chapter == null ? null : chapter.getSlug(),
                    chapter == null ? null : chapter.getTitle(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getHref(),
                    notification.getReadAt(),
                    notification.getCreatedAt()
            ));
        }
        return items;
    }
}
