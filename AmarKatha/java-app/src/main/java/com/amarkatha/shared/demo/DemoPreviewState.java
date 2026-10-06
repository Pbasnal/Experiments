package com.amarkatha.shared.demo;

import jakarta.servlet.http.HttpSession;
import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Session-only changes made while previewing demo content.
 * The baseline catalog stays in {@link DemoLibrary}.
 */
public final class DemoPreviewState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "amarkatha.demoPreview";

    private final Set<String> extraFollows = new LinkedHashSet<>();
    private final Set<String> removedFollows = new LinkedHashSet<>();
    private final Map<String, String> progressBySeries = new LinkedHashMap<>();
    private final Set<UUID> readNotificationIds = new LinkedHashSet<>();
    private boolean allNotificationsRead;
    private Boolean inAppNewChapter;
    private Boolean emailNewChapter;
    private Boolean emailProductUpdates;
    private Set<UUID> reactedGlimpseImages = new LinkedHashSet<>();

    public static DemoPreviewState from(HttpSession session) {
        if (session == null) {
            return new DemoPreviewState();
        }
        Object raw = session.getAttribute(SESSION_KEY);
        if (raw instanceof DemoPreviewState state) {
            return state;
        }
        DemoPreviewState created = new DemoPreviewState();
        session.setAttribute(SESSION_KEY, created);
        return created;
    }

    public static void clear(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
        }
    }

    public boolean follows(String slug, boolean followedByDefault) {
        if (extraFollows.contains(slug)) {
            return true;
        }
        if (removedFollows.contains(slug)) {
            return false;
        }
        return followedByDefault;
    }

    public void follow(String slug, boolean followedByDefault) {
        removedFollows.remove(slug);
        if (!followedByDefault) {
            extraFollows.add(slug);
        }
    }

    public void unfollow(String slug, boolean followedByDefault) {
        extraFollows.remove(slug);
        if (followedByDefault) {
            removedFollows.add(slug);
        }
    }

    public String progressChapter(String slug, String defaultChapterSlug) {
        return progressBySeries.getOrDefault(slug, defaultChapterSlug);
    }

    public void markProgress(String seriesSlug, String chapterSlug) {
        progressBySeries.put(seriesSlug, chapterSlug);
    }

    public boolean notificationRead(UUID id, boolean unreadByDefault) {
        return allNotificationsRead || readNotificationIds.contains(id) || !unreadByDefault;
    }

    public void markNotificationRead(UUID id) {
        readNotificationIds.add(id);
    }

    public void markAllNotificationsRead() {
        allNotificationsRead = true;
    }

    public boolean inAppNewChapter(boolean defaultValue) {
        return inAppNewChapter == null ? defaultValue : inAppNewChapter;
    }

    public boolean emailNewChapter(boolean defaultValue) {
        return emailNewChapter == null ? defaultValue : emailNewChapter;
    }

    public boolean emailProductUpdates(boolean defaultValue) {
        return emailProductUpdates == null ? defaultValue : emailProductUpdates;
    }

    public boolean glimpseReacted(UUID imageId) {
        return reactedGlimpseImages != null && reactedGlimpseImages.contains(imageId);
    }

    public void setGlimpseReaction(UUID imageId, boolean reacted) {
        if (reactedGlimpseImages == null) {
            reactedGlimpseImages = new LinkedHashSet<>();
        }
        if (reacted) {
            reactedGlimpseImages.add(imageId);
        } else {
            reactedGlimpseImages.remove(imageId);
        }
    }

    public void updatePreferences(Boolean inApp, Boolean emailChapter, Boolean emailProduct) {
        if (inApp != null) {
            inAppNewChapter = inApp;
        }
        if (emailChapter != null) {
            emailNewChapter = emailChapter;
        }
        if (emailProduct != null) {
            emailProductUpdates = emailProduct;
        }
    }
}
