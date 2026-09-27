package com.amarkatha.engagement;

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
import com.amarkatha.engagement.dto.ReaderPortalSummaryDto;
import com.amarkatha.engagement.dto.ReaderProgressDto;
import com.amarkatha.engagement.dto.UnreadCountDto;
import com.amarkatha.identity.security.AmarKathaPrincipal;
import com.amarkatha.identity.security.AuthSessionKeys;
import com.amarkatha.identity.security.SafeReturnPath;
import com.amarkatha.shared.ReaderFeatureGate;
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
    private final ReaderFeatureGate readerFeatureGate;

    public EngagementApiController(
            SeriesFollowService seriesFollowService,
            ReaderProgressService readerProgressService,
            ReaderNotificationService readerNotificationService,
            NotificationPreferenceService notificationPreferenceService,
            ReaderFeatureGate readerFeatureGate
    ) {
        this.seriesFollowService = seriesFollowService;
        this.readerProgressService = readerProgressService;
        this.readerNotificationService = readerNotificationService;
        this.notificationPreferenceService = notificationPreferenceService;
        this.readerFeatureGate = readerFeatureGate;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public ReaderPortalSummaryDto portalSummary(@AuthenticationPrincipal AmarKathaPrincipal principal) {
        readerFeatureGate.requireProfileProgress();
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
    public List<FollowedSeriesDto> following(@AuthenticationPrincipal AmarKathaPrincipal principal) {
        readerFeatureGate.requireFollows();
        return seriesFollowService.listFollowing(requireUser(principal));
    }

    @GetMapping("/me/progress")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public List<ReaderProgressDto> progress(@AuthenticationPrincipal AmarKathaPrincipal principal) {
        readerFeatureGate.requireProfileProgress();
        return readerProgressService.listProgress(requireUser(principal));
    }

    @PostMapping("/me/progress")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public ReaderProgressDto updateProgress(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestBody ProgressUpdateRequest body
    ) {
        readerFeatureGate.requireProfileProgress();
        return readerProgressService.recordRead(requireUser(principal), body);
    }

    @GetMapping("/me/notifications")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public NotificationListResponse notifications(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam(required = false) Integer limit
    ) {
        return readerNotificationService.list(requireUser(principal), limit);
    }

    @GetMapping("/me/notifications/unread-count")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public UnreadCountDto unreadCount(@AuthenticationPrincipal AmarKathaPrincipal principal) {
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
        return readerNotificationService.markRead(requireUser(principal), id, request, response);
    }

    @PostMapping("/me/notifications/read-all")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public ReadAllResponse markAllNotificationsRead(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return readerNotificationService.markAllRead(requireUser(principal), request, response);
    }

    @GetMapping("/me/notification-preferences")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public NotificationPreferenceDto notificationPreferences(
            @AuthenticationPrincipal AmarKathaPrincipal principal
    ) {
        return notificationPreferenceService.getPreferences(requireUser(principal));
    }

    @PutMapping("/me/notification-preferences")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public NotificationPreferenceDto updateNotificationPreferences(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestBody NotificationPreferenceUpdateRequest body
    ) {
        return notificationPreferenceService.updatePreferences(requireUser(principal), body);
    }

    @GetMapping("/me/notification-capabilities")
    @PreAuthorize("hasAnyRole('READER','CREATOR','ADMIN')")
    public NotificationCapabilitiesDto notificationCapabilities(
            @AuthenticationPrincipal AmarKathaPrincipal principal
    ) {
        requireUser(principal);
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
            @AuthenticationPrincipal AmarKathaPrincipal principal
    ) {
        UUID userId = principal == null ? null : principal.getId();
        return seriesFollowService.followState(userId, slug);
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
}
