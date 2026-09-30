package com.amarkatha.engagement;

import com.amarkatha.business.ExperienceSourcePolicy;
import com.amarkatha.engagement.dto.FollowStateDto;
import com.amarkatha.engagement.dto.FollowedSeriesDto;
import com.amarkatha.engagement.dto.MarkReadResponse;
import com.amarkatha.engagement.dto.NotificationCapabilitiesDto;
import com.amarkatha.engagement.dto.NotificationListResponse;
import com.amarkatha.engagement.dto.NotificationPreferenceDto;
import com.amarkatha.engagement.dto.NotificationPreferenceUpdateRequest;
import com.amarkatha.engagement.dto.PendingFollowDto;
import com.amarkatha.engagement.dto.ProgressUpdateRequest;
import com.amarkatha.engagement.dto.ReadAllResponse;
import com.amarkatha.engagement.dto.ReadTargetDto;
import com.amarkatha.engagement.dto.ReaderPortalSummaryDto;
import com.amarkatha.engagement.dto.ReaderProgressDto;
import com.amarkatha.engagement.dto.UnreadCountDto;
import com.amarkatha.identity.security.AmarKathaPrincipal;
import com.amarkatha.identity.security.AuthSessionKeys;
import com.amarkatha.identity.security.SafeReturnPath;
import com.amarkatha.shared.ReaderFeatureGate;
import com.amarkatha.shared.demo.DemoModeSignals;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reader/v1")
public class EngagementApiController {

    private static final int PORTAL_PREVIEW_LIMIT = 8;

    private final SeriesFollowService seriesFollowService;
    private final ReaderProgressService readerProgressService;
    private final ReaderNotificationService readerNotificationService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final DemoEngagementService demoEngagementService;
    private final ReaderFeatureGate readerFeatureGate;
    private final ExperienceSourcePolicy experienceSourcePolicy;

    public EngagementApiController(
            SeriesFollowService seriesFollowService,
            ReaderProgressService readerProgressService,
            ReaderNotificationService readerNotificationService,
            NotificationPreferenceService notificationPreferenceService,
            DemoEngagementService demoEngagementService,
            ReaderFeatureGate readerFeatureGate,
            ExperienceSourcePolicy experienceSourcePolicy
    ) {
        this.seriesFollowService = seriesFollowService;
        this.readerProgressService = readerProgressService;
        this.readerNotificationService = readerNotificationService;
        this.notificationPreferenceService = notificationPreferenceService;
        this.demoEngagementService = demoEngagementService;
        this.readerFeatureGate = readerFeatureGate;
        this.experienceSourcePolicy = experienceSourcePolicy;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public ReaderPortalSummaryDto portalSummary(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpServletRequest request
    ) {
        readerFeatureGate.requireProfileProgress();
        if (demo(request)) {
            return demoEngagementService.portal(principal, request.getSession());
        }
        UUID userId = requireUser(principal);
        List<ReaderProgressDto> progress = readerProgressService.listProgress(userId);
        List<FollowedSeriesDto> following = seriesFollowService.listFollowing(userId);
        return new ReaderPortalSummaryDto(
                principal.getDisplayName() == null ? "" : principal.getDisplayName(),
                principal.getEmail(),
                principal.getRole().name(),
                seriesFollowService.followingCount(userId),
                progress.stream().limit(PORTAL_PREVIEW_LIMIT).toList(),
                following.stream().limit(PORTAL_PREVIEW_LIMIT).toList()
        );
    }

    @GetMapping("/me/following")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public List<FollowedSeriesDto> following(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpServletRequest request
    ) {
        readerFeatureGate.requireFollows();
        if (demo(request)) {
            return demoEngagementService.following(request.getSession());
        }
        return seriesFollowService.listFollowing(requireUser(principal));
    }

    @GetMapping("/me/progress")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public List<ReaderProgressDto> progress(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpServletRequest request
    ) {
        readerFeatureGate.requireProfileProgress();
        if (demo(request)) {
            return demoEngagementService.progress(request.getSession());
        }
        return readerProgressService.listProgress(requireUser(principal));
    }

    @PostMapping("/me/progress")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public ReaderProgressDto updateProgress(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestBody ProgressUpdateRequest body,
            HttpServletRequest request
    ) {
        readerFeatureGate.requireProfileProgress();
        if (demo(request)) {
            if (body == null || body.seriesSlug() == null || body.chapterSlug() == null) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "seriesSlug and chapterSlug required"
                );
            }
            return demoEngagementService.recordRead(body.seriesSlug(), body.chapterSlug(), request.getSession());
        }
        return readerProgressService.recordRead(requireUser(principal), body);
    }

    @GetMapping("/me/notifications")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public NotificationListResponse notifications(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request
    ) {
        if (demo(request)) {
            return demoEngagementService.notifications(request.getSession(), limit);
        }
        return readerNotificationService.list(requireUser(principal), limit);
    }

    @GetMapping("/me/notifications/unread-count")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public UnreadCountDto unreadCount(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpServletRequest request
    ) {
        if (demo(request)) {
            return demoEngagementService.unread(request.getSession());
        }
        return readerNotificationService.unread(requireUser(principal));
    }

    @PostMapping("/me/notifications/{id}/read")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public MarkReadResponse markNotificationRead(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @PathVariable UUID id,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (demo(request)) {
            return demoEngagementService.markRead(id, request.getSession());
        }
        return readerNotificationService.markRead(requireUser(principal), id, request, response);
    }

    @PostMapping("/me/notifications/read-all")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public ReadAllResponse markAllNotificationsRead(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (demo(request)) {
            return demoEngagementService.markAllRead(request.getSession());
        }
        return readerNotificationService.markAllRead(requireUser(principal), request, response);
    }

    @GetMapping("/me/notification-preferences")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public NotificationPreferenceDto notificationPreferences(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpServletRequest request
    ) {
        if (demo(request)) {
            return demoEngagementService.preferences(request.getSession());
        }
        return notificationPreferenceService.getPreferences(requireUser(principal));
    }

    @PutMapping("/me/notification-preferences")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public NotificationPreferenceDto updateNotificationPreferences(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestBody NotificationPreferenceUpdateRequest body,
            HttpServletRequest request
    ) {
        if (demo(request)) {
            return demoEngagementService.updatePreferences(body, request.getSession());
        }
        return notificationPreferenceService.updatePreferences(requireUser(principal), body);
    }

    @GetMapping("/me/notification-capabilities")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public NotificationCapabilitiesDto notificationCapabilities(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpServletRequest request
    ) {
        requireUser(principal);
        if (demo(request)) {
            return demoEngagementService.capabilities();
        }
        return notificationPreferenceService.capabilities();
    }

    /**
     * Peeks the OAuth pending-follow slug stored in session (does not clear it).
     * The SPA verifies the slug against the current series and completes via POST /follow.
     */
    @GetMapping("/me/pending-follow")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public PendingFollowDto pendingFollow(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpSession session
    ) {
        requireUser(principal);
        return new PendingFollowDto(readPendingFollowSlug(session));
    }

    @GetMapping("/series/{slug}/follow")
    public FollowStateDto followState(
            @PathVariable String slug,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpServletRequest request
    ) {
        UUID userId = principal == null ? null : principal.getId();
        if (demo(request)) {
            return demoEngagementService.followState(slug, request.getSession());
        }
        return seriesFollowService.followState(userId, slug);
    }

    @GetMapping("/series/{slug}/read-target")
    public ReadTargetDto readTarget(
            @PathVariable String slug,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpServletRequest request
    ) {
        if (demo(request)) {
            return demoEngagementService.readTarget(slug, request.getSession());
        }
        UUID userId = principal == null ? null : principal.getId();
        return readerProgressService.readTarget(userId, slug);
    }

    @PostMapping("/series/{slug}/follow")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public FollowStateDto follow(
            @PathVariable String slug,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session
    ) {
        if (demo(request)) {
            return demoEngagementService.follow(slug, session);
        }
        FollowStateDto state = seriesFollowService.follow(requireUser(principal), slug, request, response);
        clearPendingFollowIfMatches(session, slug);
        return state;
    }

    @DeleteMapping("/series/{slug}/follow")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public FollowStateDto unfollow(
            @PathVariable String slug,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (demo(request)) {
            return demoEngagementService.unfollow(slug, request.getSession());
        }
        return seriesFollowService.unfollow(requireUser(principal), slug, request, response);
    }

    private static String readPendingFollowSlug(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object raw = session.getAttribute(AuthSessionKeys.PENDING_FOLLOW_SERIES_SLUG);
        if (raw == null) {
            return null;
        }
        return SafeReturnPath.normalizeSeriesSlug(raw.toString()).orElse(null);
    }

    private static void clearPendingFollowIfMatches(HttpSession session, String slug) {
        String pending = readPendingFollowSlug(session);
        if (pending != null && pending.equalsIgnoreCase(slug) && session != null) {
            session.removeAttribute(AuthSessionKeys.PENDING_FOLLOW_SERIES_SLUG);
        }
    }

    private static UUID requireUser(AmarKathaPrincipal principal) {
        if (principal == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }
        return principal.getId();
    }

    private boolean demo(HttpServletRequest request) {
        return experienceSourcePolicy.demo(DemoModeSignals.admin(request), DemoModeSignals.requested(request));
    }
}
