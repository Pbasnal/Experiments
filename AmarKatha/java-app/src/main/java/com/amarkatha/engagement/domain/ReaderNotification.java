package com.amarkatha.engagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reader_notification")
public class ReaderNotification {

    public static final String TYPE_CHAPTER_PUBLISHED = "CHAPTER_PUBLISHED";
    public static final String TYPE_GLIMPSE_PUBLISHED = "GLIMPSE_PUBLISHED";

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(name = "chapter_id")
    private UUID chapterId;

    @Column(name = "glimpse_id")
    private UUID glimpseId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, length = 500)
    private String href;

    @Column(name = "in_app_visible", nullable = false)
    private boolean inAppVisible = true;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ReaderNotification() {
    }

    public static ReaderNotification chapterPublished(
            UUID userId,
            UUID seriesId,
            UUID chapterId,
            String title,
            String message,
            String href,
            boolean inAppVisible
    ) {
        ReaderNotification notification = new ReaderNotification();
        notification.id = UUID.randomUUID();
        notification.userId = userId;
        notification.type = TYPE_CHAPTER_PUBLISHED;
        notification.seriesId = seriesId;
        notification.chapterId = chapterId;
        notification.title = title;
        notification.message = message;
        notification.href = href;
        notification.inAppVisible = inAppVisible;
        notification.createdAt = Instant.now();
        return notification;
    }

    public static ReaderNotification glimpsePublished(
            UUID userId,
            UUID seriesId,
            UUID glimpseId,
            String title,
            String message,
            String href,
            boolean inAppVisible
    ) {
        ReaderNotification notification = new ReaderNotification();
        notification.id = UUID.randomUUID();
        notification.userId = userId;
        notification.type = TYPE_GLIMPSE_PUBLISHED;
        notification.seriesId = seriesId;
        notification.glimpseId = glimpseId;
        notification.title = title;
        notification.message = message;
        notification.href = href;
        notification.inAppVisible = inAppVisible;
        notification.createdAt = Instant.now();
        return notification;
    }

    public void markRead(Instant now) {
        if (this.readAt == null) {
            this.readAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public UUID getSeriesId() {
        return seriesId;
    }

    public UUID getChapterId() {
        return chapterId;
    }

    public UUID getGlimpseId() {
        return glimpseId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getHref() {
        return href;
    }

    public boolean isInAppVisible() {
        return inAppVisible;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
