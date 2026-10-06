package com.amarkatha.engagement;

import com.amarkatha.engagement.domain.NotificationEmailDelivery;
import com.amarkatha.engagement.domain.NotificationPreference;
import com.amarkatha.engagement.domain.ReaderNotification;
import com.amarkatha.engagement.domain.SeriesFollow;
import com.amarkatha.shared.web.AbsoluteUrlBuilder;
import com.amarkatha.identity.UserRepository;
import com.amarkatha.identity.domain.User;
import com.amarkatha.outbox.DomainEventOutbox;
import com.amarkatha.outbox.DomainEventOutboxRepository;
import com.amarkatha.outbox.DomainEventTypes;
import com.amarkatha.outbox.OutboxEventStatus;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.ReaderFeatureGate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChapterPublishedOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(ChapterPublishedOutboxProcessor.class);
    private static final String APP_NAME_MESSAGE_KEY = "app.name";
    private static final String APP_NAME_FALLBACK = "AmarKatha";

    private final DomainEventOutboxRepository outboxRepository;
    private final SeriesFollowRepository seriesFollowRepository;
    private final NotificationPreferenceService preferenceService;
    private final ReaderNotificationRepository notificationRepository;
    private final NotificationEmailDeliveryRepository emailDeliveryRepository;
    private final UserRepository userRepository;
    private final NotificationProperties properties;
    private final NotificationMetrics metrics;
    private final MessageSource messageSource;
    private final ReaderFeatureGate readerFeatureGate;
    private final NotificationMailSender mailSender;
    private final AbsoluteUrlBuilder absoluteUrlBuilder;
    private final SeriesRepository seriesRepository;

    public ChapterPublishedOutboxProcessor(
            DomainEventOutboxRepository outboxRepository,
            SeriesFollowRepository seriesFollowRepository,
            NotificationPreferenceService preferenceService,
            ReaderNotificationRepository notificationRepository,
            NotificationEmailDeliveryRepository emailDeliveryRepository,
            UserRepository userRepository,
            NotificationProperties properties,
            NotificationMetrics metrics,
            MessageSource messageSource,
            ReaderFeatureGate readerFeatureGate,
            NotificationMailSender mailSender,
            AbsoluteUrlBuilder absoluteUrlBuilder,
            SeriesRepository seriesRepository
    ) {
        this.outboxRepository = outboxRepository;
        this.seriesFollowRepository = seriesFollowRepository;
        this.preferenceService = preferenceService;
        this.notificationRepository = notificationRepository;
        this.emailDeliveryRepository = emailDeliveryRepository;
        this.userRepository = userRepository;
        this.properties = properties;
        this.metrics = metrics;
        this.messageSource = messageSource;
        this.readerFeatureGate = readerFeatureGate;
        this.mailSender = mailSender;
        this.absoluteUrlBuilder = absoluteUrlBuilder;
        this.seriesRepository = seriesRepository;
    }

    @Transactional
    public int processPendingBatch() {
        Instant now = Instant.now();
        List<DomainEventOutbox> pending = outboxRepository
                .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxEventStatus.PENDING,
                        now
                );
        int processed = 0;
        for (DomainEventOutbox event : pending) {
            if (outboxRepository.claimPending(event.getId()) == 0) {
                continue;
            }
            DomainEventOutbox claimed = outboxRepository.findById(event.getId()).orElse(null);
            if (claimed == null) {
                continue;
            }
            try {
                processClaimed(claimed);
                claimed.markProcessed(Instant.now());
                outboxRepository.save(claimed);
                metrics.outboxProcessed();
                processed++;
            } catch (Exception ex) {
                log.warn("Outbox event {} failed: {}", claimed.getId(), ex.getMessage());
                Instant next = Instant.now().plus(backoff(claimed.getAttemptCount() + 1));
                claimed.scheduleRetry(next, ex.getMessage(), properties.maxAttempts());
                outboxRepository.save(claimed);
                if (claimed.getStatus() == OutboxEventStatus.FAILED) {
                    metrics.outboxFailed();
                } else {
                    metrics.outboxRetried();
                }
            }
        }
        return processed;
    }

    private void processClaimed(DomainEventOutbox event) {
        if (DomainEventTypes.GLIMPSE_PUBLISHED.equals(event.getEventType())) {
            processGlimpse(event);
            return;
        }
        if (!DomainEventTypes.CHAPTER_PUBLISHED.equals(event.getEventType())) {
            return;
        }
        Map<String, Object> payload = event.getPayload();
        UUID seriesId = uuid(payload.get("seriesId"));
        UUID chapterId = uuid(payload.get("chapterId"));
        String seriesSlug = string(payload.get("seriesSlug"));
        String seriesTitle = string(payload.get("seriesTitle"));
        String chapterSlug = string(payload.get("chapterSlug"));
        String chapterTitle = string(payload.get("chapterTitle"));
        String href = "/read/s/" + seriesSlug + "/c/" + chapterSlug;
        String title = seriesTitle + " — new chapter";
        String message = chapterTitle == null || chapterTitle.isBlank()
                ? "A new chapter is available."
                : chapterTitle + " is now available.";

        UUID creatorId = seriesRepository.findById(seriesId).map(Series::getCreatorId).orElse(null);
        List<SeriesFollow> followers = seriesFollowRepository.findBySeriesId(seriesId);
        for (SeriesFollow follow : followers) {
            if (creatorId != null && creatorId.equals(follow.getUserId())) {
                continue;
            }
            NotificationPreference preference = preferenceService.resolve(follow.getUserId());
            boolean wantInApp = preference.isInAppNewChapter() && readerFeatureGate.inAppNotificationsEnabled();
            boolean wantEmail = preference.isEmailNewChapter()
                    && readerFeatureGate.emailNotificationsEnabled(mailSender.isEnabled());
            if (!wantInApp && !wantEmail) {
                continue;
            }

            // Always persist a row when email is wanted (FK for delivery); gate in-app APIs via inAppVisible.
            ReaderNotification notification = ensureNotification(
                    follow.getUserId(),
                    seriesId,
                    chapterId,
                    title,
                    message,
                    href,
                    wantInApp
            );
            if (notification == null) {
                continue;
            }
            if (wantEmail) {
                queueEmail(follow.getUserId(), notification, seriesTitle, chapterTitle, href);
            }
        }
    }

    private void processGlimpse(DomainEventOutbox event) {
        Map<String, Object> payload = event.getPayload();
        UUID seriesId = uuid(payload.get("seriesId"));
        UUID glimpseId = uuid(payload.get("glimpseId"));
        String seriesSlug = string(payload.get("seriesSlug"));
        String seriesTitle = string(payload.get("seriesTitle"));
        String href = "/read/s/" + seriesSlug + "#glimpse-" + glimpseId;
        String title = seriesTitle + " — new glimpse";
        String message = "A new glimpse is up.";

        UUID creatorId = seriesRepository.findById(seriesId).map(Series::getCreatorId).orElse(null);
        List<SeriesFollow> followers = seriesFollowRepository.findBySeriesId(seriesId);
        for (SeriesFollow follow : followers) {
            if (creatorId != null && creatorId.equals(follow.getUserId())) {
                continue;
            }
            NotificationPreference preference = preferenceService.resolve(follow.getUserId());
            boolean wantInApp = preference.isInAppNewChapter() && readerFeatureGate.inAppNotificationsEnabled();
            boolean wantEmail = preference.isEmailNewChapter()
                    && readerFeatureGate.emailNotificationsEnabled(mailSender.isEnabled());
            if (!wantInApp && !wantEmail) {
                continue;
            }
            ReaderNotification notification = ensureGlimpseNotification(
                    follow.getUserId(),
                    seriesId,
                    glimpseId,
                    title,
                    message,
                    href,
                    wantInApp
            );
            if (notification == null) {
                continue;
            }
            if (wantEmail) {
                queueGlimpseEmail(follow.getUserId(), notification, seriesTitle, href);
            }
        }
    }

    private ReaderNotification ensureGlimpseNotification(
            UUID userId,
            UUID seriesId,
            UUID glimpseId,
            String title,
            String message,
            String href,
            boolean inAppVisible
    ) {
        Optional<ReaderNotification> existing = notificationRepository.findByUserIdAndTypeAndGlimpseId(
                userId,
                ReaderNotification.TYPE_GLIMPSE_PUBLISHED,
                glimpseId
        );
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            ReaderNotification created = notificationRepository.save(ReaderNotification.glimpsePublished(
                    userId,
                    seriesId,
                    glimpseId,
                    title,
                    message,
                    href,
                    inAppVisible
            ));
            metrics.notificationCreated();
            return created;
        } catch (DataIntegrityViolationException ignored) {
            return notificationRepository.findByUserIdAndTypeAndGlimpseId(
                    userId,
                    ReaderNotification.TYPE_GLIMPSE_PUBLISHED,
                    glimpseId
            ).orElse(null);
        }
    }

    private void queueGlimpseEmail(
            UUID userId,
            ReaderNotification notification,
            String seriesTitle,
            String href
    ) {
        String dedupeKey = "EMAIL:" + ReaderNotification.TYPE_GLIMPSE_PUBLISHED + ":" + userId + ":"
                + notification.getGlimpseId();
        if (emailDeliveryRepository.findByDedupeKey(dedupeKey).isPresent()) {
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        String subject = "New glimpse: " + seriesTitle;
        String absoluteHref = absoluteUrlBuilder.absolute(href);
        String absolutePrefs = absoluteUrlBuilder.absolute("/read/profile");
        String body = buildGlimpseEmailBody(seriesTitle, absoluteHref, absolutePrefs, productName());
        try {
            emailDeliveryRepository.save(NotificationEmailDelivery.pending(
                    userId,
                    notification.getId(),
                    dedupeKey,
                    user.getEmail(),
                    subject,
                    body
            ));
            metrics.emailQueued();
        } catch (DataIntegrityViolationException ignored) {
            // concurrent queue
        }
    }

    static String buildGlimpseEmailBody(
            String seriesTitle,
            String absoluteHref,
            String absolutePrefsHref,
            String productName
    ) {
        String name = productName == null || productName.isBlank() ? APP_NAME_FALLBACK : productName;
        return "A new glimpse of " + seriesTitle + " is now available on " + name + ".\n\n"
                + "See it here: " + absoluteHref + "\n\n"
                + "Manage email preferences: " + absolutePrefsHref + "\n";
    }

    private ReaderNotification ensureNotification(
            UUID userId,
            UUID seriesId,
            UUID chapterId,
            String title,
            String message,
            String href,
            boolean inAppVisible
    ) {
        Optional<ReaderNotification> existing = notificationRepository.findByUserIdAndTypeAndChapterId(
                userId,
                ReaderNotification.TYPE_CHAPTER_PUBLISHED,
                chapterId
        );
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            ReaderNotification created = notificationRepository.save(ReaderNotification.chapterPublished(
                    userId,
                    seriesId,
                    chapterId,
                    title,
                    message,
                    href,
                    inAppVisible
            ));
            metrics.notificationCreated();
            return created;
        } catch (DataIntegrityViolationException ignored) {
            return notificationRepository.findByUserIdAndTypeAndChapterId(
                    userId,
                    ReaderNotification.TYPE_CHAPTER_PUBLISHED,
                    chapterId
            ).orElse(null);
        }
    }

    private void queueEmail(
            UUID userId,
            ReaderNotification notification,
            String seriesTitle,
            String chapterTitle,
            String href
    ) {
        String dedupeKey = "EMAIL:" + ReaderNotification.TYPE_CHAPTER_PUBLISHED + ":" + userId + ":"
                + notification.getChapterId();
        if (emailDeliveryRepository.findByDedupeKey(dedupeKey).isPresent()) {
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        String subject = "New chapter: " + seriesTitle;
        String absoluteHref = absoluteUrlBuilder.absolute(href);
        String absolutePrefs = absoluteUrlBuilder.absolute("/read/profile");
        String body = buildEmailBody(seriesTitle, chapterTitle, absoluteHref, absolutePrefs, productName());
        try {
            emailDeliveryRepository.save(NotificationEmailDelivery.pending(
                    userId,
                    notification.getId(),
                    dedupeKey,
                    user.getEmail(),
                    subject,
                    body
            ));
            metrics.emailQueued();
        } catch (DataIntegrityViolationException ignored) {
            // concurrent queue
        }
    }

    private String productName() {
        return messageSource.getMessage(APP_NAME_MESSAGE_KEY, null, APP_NAME_FALLBACK, Locale.ENGLISH);
    }

    static String buildEmailBody(
            String seriesTitle,
            String chapterTitle,
            String absoluteHref,
            String absolutePrefsHref,
            String productName
    ) {
        String chapterLine = chapterTitle == null || chapterTitle.isBlank()
                ? "A new chapter"
                : "\"" + chapterTitle + "\"";
        String name = productName == null || productName.isBlank() ? APP_NAME_FALLBACK : productName;
        return chapterLine + " of " + seriesTitle + " is now available on " + name + ".\n\n"
                + "Read it here: " + absoluteHref + "\n\n"
                + "Manage email preferences: " + absolutePrefsHref + "\n";
    }

    static Duration backoff(int attempt) {
        long seconds = Math.min(3600L, (long) Math.pow(2, Math.min(attempt, 10)));
        return Duration.ofSeconds(Math.max(5L, seconds));
    }

    private static UUID uuid(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing UUID in payload");
        }
        return UUID.fromString(value.toString());
    }

    private static String string(Object value) {
        return value == null ? "" : value.toString();
    }
}
