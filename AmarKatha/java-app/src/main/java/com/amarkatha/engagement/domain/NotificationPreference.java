package com.amarkatha.engagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preference")
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email_new_chapter", nullable = false)
    private boolean emailNewChapter;

    @Column(name = "in_app_new_chapter", nullable = false)
    private boolean inAppNewChapter = true;

    @Column(name = "email_product_updates", nullable = false)
    private boolean emailProductUpdates;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected NotificationPreference() {
    }

    public static NotificationPreference defaults(UUID userId) {
        NotificationPreference preference = new NotificationPreference();
        preference.userId = userId;
        preference.emailNewChapter = false;
        preference.inAppNewChapter = true;
        preference.emailProductUpdates = false;
        preference.updatedAt = Instant.now();
        return preference;
    }

    public void update(Boolean emailNewChapter, Boolean inAppNewChapter, Boolean emailProductUpdates) {
        if (emailNewChapter != null) {
            this.emailNewChapter = emailNewChapter;
        }
        if (inAppNewChapter != null) {
            this.inAppNewChapter = inAppNewChapter;
        }
        if (emailProductUpdates != null) {
            this.emailProductUpdates = emailProductUpdates;
        }
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isEmailNewChapter() {
        return emailNewChapter;
    }

    public boolean isInAppNewChapter() {
        return inAppNewChapter;
    }

    public boolean isEmailProductUpdates() {
        return emailProductUpdates;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
