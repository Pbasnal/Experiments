package com.amarkatha.engagement;

import com.amarkatha.business.ReadingEntryInput;
import com.amarkatha.business.ReadingEntryInstruction;
import com.amarkatha.business.ReadingEntryPolicy;
import com.amarkatha.engagement.dto.FollowStateDto;
import com.amarkatha.engagement.dto.FollowedSeriesDto;
import com.amarkatha.engagement.dto.MarkReadResponse;
import com.amarkatha.engagement.dto.NotificationCapabilitiesDto;
import com.amarkatha.engagement.dto.NotificationListResponse;
import com.amarkatha.engagement.dto.NotificationPreferenceDto;
import com.amarkatha.engagement.dto.NotificationPreferenceUpdateRequest;
import com.amarkatha.engagement.dto.ReadAllResponse;
import com.amarkatha.engagement.dto.ReadTargetDto;
import com.amarkatha.engagement.dto.ReaderNotificationDto;
import com.amarkatha.engagement.dto.ReaderPortalSummaryDto;
import com.amarkatha.engagement.dto.ReaderProgressDto;
import com.amarkatha.engagement.dto.UnreadCountDto;
import com.amarkatha.identity.security.AmarKathaPrincipal;
import com.amarkatha.publishing.SeriesCoverPresentation;
import com.amarkatha.shared.demo.DemoLibrary;
import com.amarkatha.shared.demo.DemoLibrary.DemoChapter;
import com.amarkatha.shared.demo.DemoLibrary.DemoNotice;
import com.amarkatha.shared.demo.DemoLibrary.DemoStory;
import com.amarkatha.shared.demo.DemoPreviewState;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reader portal data for demo mode. Changes stay in the HTTP session and never
 * touch follows, progress, notifications, or email.
 */
@Service
public class DemoEngagementService {

    private final ReadingEntryPolicy readingEntryPolicy;

    public DemoEngagementService(ReadingEntryPolicy readingEntryPolicy) {
        this.readingEntryPolicy = readingEntryPolicy;
    }

    public ReaderPortalSummaryDto portal(AmarKathaPrincipal principal, HttpSession session) {
        DemoPreviewState state = DemoPreviewState.from(session);
        List<ReaderProgressDto> progress = progress(state);
        List<FollowedSeriesDto> following = following(state);
        return new ReaderPortalSummaryDto(
                principal.getDisplayName() == null ? "" : principal.getDisplayName(),
                principal.getEmail(),
                principal.getRole().name(),
                following.size(),
                progress.stream().limit(8).toList(),
                following.stream().limit(8).toList()
        );
    }

    public List<ReaderProgressDto> progress(HttpSession session) {
        return progress(DemoPreviewState.from(session));
    }

    public List<FollowedSeriesDto> following(HttpSession session) {
        return following(DemoPreviewState.from(session));
    }

    public FollowStateDto followState(String slug, HttpSession session) {
        DemoStory story = requireStory(slug);
        DemoPreviewState state = DemoPreviewState.from(session);
        return new FollowStateDto(story.slug(), state.follows(story.slug(), story.followedByDefault()));
    }

    public FollowStateDto follow(String slug, HttpSession session) {
        DemoStory story = requireStory(slug);
        DemoPreviewState state = DemoPreviewState.from(session);
        state.follow(story.slug(), story.followedByDefault());
        return new FollowStateDto(story.slug(), true);
    }

    public FollowStateDto unfollow(String slug, HttpSession session) {
        DemoStory story = requireStory(slug);
        DemoPreviewState state = DemoPreviewState.from(session);
        state.unfollow(story.slug(), story.followedByDefault());
        return new FollowStateDto(story.slug(), false);
    }

    public ReaderProgressDto recordRead(String seriesSlug, String chapterSlug, HttpSession session) {
        DemoStory story = requireStory(seriesSlug);
        DemoChapter chapter = story.chapter(chapterSlug).orElseThrow(() -> notFound("Chapter not found"));
        DemoPreviewState state = DemoPreviewState.from(session);
        state.markProgress(story.slug(), chapter.slug());
        return progressDto(story, chapter, Instant.now());
    }

    public ReadTargetDto readTarget(String slug, HttpSession session) {
        DemoStory story = requireStory(slug);
        DemoPreviewState state = DemoPreviewState.from(session);
        String chapterSlug = state.progressChapter(story.slug(), story.defaultProgressChapterSlug());
        DemoChapter chapter = chapterSlug == null ? null : story.chapter(chapterSlug).orElse(null);
        ReadingEntryInstruction instruction = readingEntryPolicy.decide(
                new ReadingEntryInput(chapter != null)
        );
        String seriesPath = "/read/s/" + story.slug();
        if (instruction == ReadingEntryInstruction.RESUME_LAST_CHAPTER) {
            return new ReadTargetDto(seriesPath + "/c/" + chapter.slug(), true);
        }
        return new ReadTargetDto(seriesPath, false);
    }

    public NotificationListResponse notifications(HttpSession session, Integer limit) {
        DemoPreviewState state = DemoPreviewState.from(session);
        int cap = limit == null || limit < 1 ? 20 : Math.min(limit, 50);
        List<ReaderNotificationDto> items = DemoLibrary.notices().stream()
                .limit(cap)
                .map(notice -> notification(notice, state))
                .toList();
        return new NotificationListResponse(items, unread(state));
    }

    public UnreadCountDto unread(HttpSession session) {
        return new UnreadCountDto(unread(DemoPreviewState.from(session)));
    }

    public MarkReadResponse markRead(UUID id, HttpSession session) {
        DemoNotice notice = DemoLibrary.notice(id).orElseThrow(() -> notFound("Notification not found"));
        DemoPreviewState state = DemoPreviewState.from(session);
        state.markNotificationRead(notice.id());
        return new MarkReadResponse(notice.id(), Instant.now());
    }

    public ReadAllResponse markAllRead(HttpSession session) {
        DemoPreviewState state = DemoPreviewState.from(session);
        int unread = (int) unread(state);
        state.markAllNotificationsRead();
        return new ReadAllResponse(unread);
    }

    public NotificationPreferenceDto preferences(HttpSession session) {
        DemoPreviewState state = DemoPreviewState.from(session);
        return new NotificationPreferenceDto(
                state.emailNewChapter(true),
                state.inAppNewChapter(true),
                state.emailProductUpdates(false),
                Instant.now()
        );
    }

    public NotificationPreferenceDto updatePreferences(
            NotificationPreferenceUpdateRequest request,
            HttpSession session
    ) {
        DemoPreviewState state = DemoPreviewState.from(session);
        if (request != null) {
            state.updatePreferences(
                    request.inAppNewChapter(),
                    request.emailNewChapter(),
                    request.emailProductUpdates()
            );
        }
        return preferences(session);
    }

    public NotificationCapabilitiesDto capabilities() {
        return new NotificationCapabilitiesDto(true, true, false);
    }

    private List<ReaderProgressDto> progress(DemoPreviewState state) {
        return DemoLibrary.stories().stream()
                .map(story -> {
                    String chapterSlug = state.progressChapter(story.slug(), story.defaultProgressChapterSlug());
                    if (chapterSlug == null) {
                        return null;
                    }
                    return story.chapter(chapterSlug)
                            .map(chapter -> progressDto(story, chapter, agoDays(story.updatedDaysAgo())))
                            .orElse(null);
                })
                .filter(item -> item != null)
                .toList();
    }

    private List<FollowedSeriesDto> following(DemoPreviewState state) {
        return DemoLibrary.stories().stream()
                .filter(story -> state.follows(story.slug(), story.followedByDefault()))
                .map(story -> followed(story, state))
                .toList();
    }

    private FollowedSeriesDto followed(DemoStory story, DemoPreviewState state) {
        DemoChapter latest = story.latestChapter();
        String lastRead = state.progressChapter(story.slug(), story.defaultProgressChapterSlug());
        boolean unread = lastRead == null || !lastRead.equals(latest.slug());
        return new FollowedSeriesDto(
                story.slug(),
                story.title(),
                SeriesCoverPresentation.coverGradient(story.slug()),
                null,
                story.scheduleLabel(),
                story.status(),
                latest.slug(),
                latest.title(),
                latest.number(),
                lastRead,
                unread,
                agoDays(Math.max(story.updatedDaysAgo(), 1))
        );
    }

    private static ReaderProgressDto progressDto(DemoStory story, DemoChapter chapter, Instant readAt) {
        return new ReaderProgressDto(
                story.slug(),
                story.title(),
                SeriesCoverPresentation.coverGradient(story.slug()),
                null,
                chapter.slug(),
                chapter.title(),
                chapter.number(),
                readAt
        );
    }

    private static ReaderNotificationDto notification(DemoNotice notice, DemoPreviewState state) {
        DemoStory story = DemoLibrary.find(notice.seriesSlug()).orElseThrow();
        DemoChapter chapter = story.chapter(notice.chapterSlug()).orElseThrow();
        boolean read = state.notificationRead(notice.id(), notice.unreadByDefault());
        Instant createdAt = Instant.now().minus(notice.ageHours(), ChronoUnit.HOURS);
        return new ReaderNotificationDto(
                notice.id(),
                "CHAPTER_PUBLISHED",
                story.slug(),
                story.title(),
                chapter.slug(),
                chapter.title(),
                notice.title(),
                notice.message(),
                "/read/s/" + story.slug() + "/c/" + chapter.slug(),
                read ? createdAt.plus(1, ChronoUnit.HOURS) : null,
                createdAt
        );
    }

    private static long unread(DemoPreviewState state) {
        return DemoLibrary.notices().stream()
                .filter(notice -> !state.notificationRead(notice.id(), notice.unreadByDefault()))
                .count();
    }

    private static DemoStory requireStory(String slug) {
        return DemoLibrary.find(slug).orElseThrow(() -> notFound("Series not found"));
    }

    private static Instant agoDays(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
