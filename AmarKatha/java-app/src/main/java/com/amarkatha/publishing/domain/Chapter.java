package com.amarkatha.publishing.domain;

import com.amarkatha.shared.domain.ChapterState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chapter")
public class Chapter {

    @Id
    private UUID id;

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(name = "chapter_number", nullable = false)
    private double chapterNumber;

    @Column(length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChapterState state = ChapterState.DRAFT;

    @Column(nullable = false)
    private String slug;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "listed_at")
    private Instant listedAt;

    @Column(name = "list_early", nullable = false)
    private boolean listEarly;

    @Column(name = "copyright_ack_at")
    private Instant copyrightAckAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Chapter() {
    }

    public static Chapter createDraft(UUID seriesId, double chapterNumber, String slug, String title) {
        Chapter chapter = new Chapter();
        chapter.id = UUID.randomUUID();
        chapter.seriesId = seriesId;
        chapter.chapterNumber = chapterNumber;
        chapter.slug = slug;
        chapter.title = title;
        chapter.state = ChapterState.DRAFT;
        return chapter;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSeriesId() {
        return seriesId;
    }

    public double getChapterNumber() {
        return chapterNumber;
    }

    public String getTitle() {
        return title;
    }

    public ChapterState getState() {
        return state;
    }

    public String getSlug() {
        return slug;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getListedAt() {
        return listedAt;
    }

    public boolean isListEarly() {
        return listEarly;
    }

    public Instant getCopyrightAckAt() {
        return copyrightAckAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setChapterNumber(double chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void publishNow(Instant now, Instant copyrightAckAt) {
        this.state = ChapterState.PUBLISHED;
        this.publishedAt = now;
        this.scheduledAt = now;
        this.listedAt = now;
        this.listEarly = true;
        this.copyrightAckAt = copyrightAckAt;
    }

    /** Hide from reader catalog while keeping PUBLISHED (e.g. missing media files). */
    public void delist() {
        this.listedAt = null;
    }

    /** Restore catalog visibility after media is verified intact. */
    public void relist(Instant listedAt) {
        if (this.state != ChapterState.PUBLISHED) {
            throw new IllegalStateException("Only published chapters can be relisted");
        }
        this.listedAt = listedAt;
    }
}
