package com.amarkatha.engagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reader_progress")
public class ReaderProgress {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(name = "last_chapter_id", nullable = false)
    private UUID lastChapterId;

    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt = Instant.now();

    protected ReaderProgress() {
    }

    public static ReaderProgress create(UUID userId, UUID seriesId, UUID lastChapterId) {
        ReaderProgress progress = new ReaderProgress();
        progress.id = UUID.randomUUID();
        progress.userId = userId;
        progress.seriesId = seriesId;
        progress.lastChapterId = lastChapterId;
        progress.lastReadAt = Instant.now();
        return progress;
    }

    public void markRead(UUID chapterId, Instant at) {
        this.lastChapterId = chapterId;
        this.lastReadAt = at;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSeriesId() {
        return seriesId;
    }

    public UUID getLastChapterId() {
        return lastChapterId;
    }

    public Instant getLastReadAt() {
        return lastReadAt;
    }
}
