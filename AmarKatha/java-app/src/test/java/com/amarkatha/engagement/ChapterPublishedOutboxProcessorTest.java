package com.amarkatha.engagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.shared.web.AbsoluteUrlBuilder;
import com.amarkatha.engagement.domain.NotificationEmailDelivery;
import com.amarkatha.engagement.domain.NotificationPreference;
import com.amarkatha.engagement.domain.ReaderNotification;
import com.amarkatha.engagement.domain.SeriesFollow;
import com.amarkatha.identity.UserRepository;
import com.amarkatha.identity.domain.User;
import com.amarkatha.outbox.DomainEventOutbox;
import com.amarkatha.outbox.DomainEventOutboxRepository;
import com.amarkatha.outbox.DomainEventTypes;
import com.amarkatha.outbox.OutboxEventStatus;
import com.amarkatha.publishing.SeriesRepository;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.ReaderFeatureGate;
import com.amarkatha.shared.ReaderFeatureProperties;
import com.amarkatha.shared.domain.UserRole;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class ChapterPublishedOutboxProcessorTest {

    @Mock
    private DomainEventOutboxRepository outboxRepository;
    @Mock
    private SeriesFollowRepository seriesFollowRepository;
    @Mock
    private NotificationPreferenceService preferenceService;
    @Mock
    private ReaderNotificationRepository notificationRepository;
    @Mock
    private NotificationEmailDeliveryRepository emailDeliveryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageSource messageSource;
    @Mock
    private NotificationMailSender mailSender;
    @Mock
    private AbsoluteUrlBuilder absoluteUrlBuilder;
    @Mock
    private SeriesRepository seriesRepository;

    private NotificationMetrics metrics;
    private ChapterPublishedOutboxProcessor processor;
    private NotificationProperties properties;

    @BeforeEach
    void setUp() {
        metrics = new NotificationMetrics(new SimpleMeterRegistry());
        properties = new NotificationProperties(5_000, 10_000, 10, 50, 50, 100, 300_000);
        org.mockito.Mockito.lenient().when(mailSender.isEnabled()).thenReturn(true);
        org.mockito.Mockito.lenient().when(absoluteUrlBuilder.absolute(any(String.class)))
                .thenAnswer(inv -> "https://amarkatha.example.com" + inv.getArgument(0));
        org.mockito.Mockito.lenient().when(seriesRepository.findById(any())).thenReturn(Optional.empty());
        processor = new ChapterPublishedOutboxProcessor(
                outboxRepository,
                seriesFollowRepository,
                preferenceService,
                notificationRepository,
                emailDeliveryRepository,
                userRepository,
                properties,
                metrics,
                messageSource,
                new ReaderFeatureGate(new ReaderFeatureProperties(true, true, true, true, true)),
                mailSender,
                absoluteUrlBuilder,
                seriesRepository
        );
    }

    @Test
    void processCreatesOneNotificationPerFollowerAndQueuesEmailForOptIn() {
        UUID seriesId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID followerA = UUID.randomUUID();
        UUID followerB = UUID.randomUUID();

        DomainEventOutbox event = DomainEventOutbox.pending(
                DomainEventTypes.CHAPTER_PUBLISHED,
                "chapter",
                chapterId,
                DomainEventTypes.CHAPTER_PUBLISHED + ":" + chapterId,
                payload(seriesId, chapterId)
        );
        when(outboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING), any(Instant.class)
        )).thenReturn(List.of(event));
        when(outboxRepository.claimPending(event.getId())).thenReturn(1);
        when(outboxRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(seriesFollowRepository.findBySeriesId(seriesId)).thenReturn(List.of(
                SeriesFollow.create(followerA, seriesId),
                SeriesFollow.create(followerB, seriesId)
        ));

        NotificationPreference inAppOnly = NotificationPreference.defaults(followerA);
        NotificationPreference emailOptIn = NotificationPreference.defaults(followerB);
        emailOptIn.update(true, true, false);
        when(preferenceService.resolve(followerA)).thenReturn(inAppOnly);
        when(preferenceService.resolve(followerB)).thenReturn(emailOptIn);

        when(notificationRepository.findByUserIdAndTypeAndChapterId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(ReaderNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        User userB = User.create("sub-b", "b@example.com", "B", UserRole.READER);
        when(userRepository.findById(followerB)).thenReturn(Optional.of(userB));
        when(emailDeliveryRepository.findByDedupeKey(any())).thenReturn(Optional.empty());
        when(emailDeliveryRepository.save(any(NotificationEmailDelivery.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(messageSource.getMessage(eq("app.name"), any(), any(), eq(Locale.ENGLISH)))
                .thenReturn("Ripple");

        int processed = processor.processPendingBatch();

        assertEquals(1, processed);
        ArgumentCaptor<ReaderNotification> notificationCaptor = ArgumentCaptor.forClass(ReaderNotification.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        assertTrue(notificationCaptor.getAllValues().stream().allMatch(ReaderNotification::isInAppVisible));

        ArgumentCaptor<NotificationEmailDelivery> emailCaptor =
                ArgumentCaptor.forClass(NotificationEmailDelivery.class);
        verify(emailDeliveryRepository, times(1)).save(emailCaptor.capture());
        assertEquals("b@example.com", emailCaptor.getValue().getToEmail());
        assertTrue(emailCaptor.getValue().getBodyText().contains("on Ripple."));
        assertTrue(emailCaptor.getValue().getBodyText().contains("https://amarkatha.example.com/read/s/demo/c/chapter-1"));
        assertTrue(emailCaptor.getValue().getBodyText().contains("https://amarkatha.example.com/read/profile"));
        verify(userRepository, never()).findById(followerA);

        ArgumentCaptor<DomainEventOutbox> outboxCaptor = ArgumentCaptor.forClass(DomainEventOutbox.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertEquals(OutboxEventStatus.PROCESSED, outboxCaptor.getValue().getStatus());
    }

    @Test
    void processSkipsTheSeriesCreator() {
        UUID seriesId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID readerId = UUID.randomUUID();

        DomainEventOutbox event = DomainEventOutbox.pending(
                DomainEventTypes.CHAPTER_PUBLISHED,
                "chapter",
                chapterId,
                DomainEventTypes.CHAPTER_PUBLISHED + ":" + chapterId,
                payload(seriesId, chapterId)
        );
        when(outboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING), any(Instant.class)
        )).thenReturn(List.of(event));
        when(outboxRepository.claimPending(event.getId())).thenReturn(1);
        when(outboxRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(Series.create(creatorId, "demo", "Demo")));
        when(seriesFollowRepository.findBySeriesId(seriesId)).thenReturn(List.of(
                SeriesFollow.create(creatorId, seriesId),
                SeriesFollow.create(readerId, seriesId)
        ));
        when(preferenceService.resolve(readerId)).thenReturn(NotificationPreference.defaults(readerId));
        when(notificationRepository.findByUserIdAndTypeAndChapterId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(ReaderNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        processor.processPendingBatch();

        ArgumentCaptor<ReaderNotification> notificationCaptor = ArgumentCaptor.forClass(ReaderNotification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        assertEquals(readerId, notificationCaptor.getValue().getUserId());
        verify(preferenceService, never()).resolve(creatorId);
    }

    @Test
    void emailOnlyFollowerCreatesHiddenNotificationAndQueuesEmail() {
        UUID seriesId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID follower = UUID.randomUUID();

        DomainEventOutbox event = DomainEventOutbox.pending(
                DomainEventTypes.CHAPTER_PUBLISHED,
                "chapter",
                chapterId,
                DomainEventTypes.CHAPTER_PUBLISHED + ":" + chapterId,
                payload(seriesId, chapterId)
        );
        when(outboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING), any(Instant.class)
        )).thenReturn(List.of(event));
        when(outboxRepository.claimPending(event.getId())).thenReturn(1);
        when(outboxRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(seriesFollowRepository.findBySeriesId(seriesId))
                .thenReturn(List.of(SeriesFollow.create(follower, seriesId)));

        NotificationPreference emailOnly = NotificationPreference.defaults(follower);
        emailOnly.update(true, false, false);
        when(preferenceService.resolve(follower)).thenReturn(emailOnly);

        when(notificationRepository.findByUserIdAndTypeAndChapterId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(ReaderNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = User.create("sub", "r@example.com", "R", UserRole.READER);
        when(userRepository.findById(follower)).thenReturn(Optional.of(user));
        when(emailDeliveryRepository.findByDedupeKey(any())).thenReturn(Optional.empty());
        when(emailDeliveryRepository.save(any(NotificationEmailDelivery.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(messageSource.getMessage(eq("app.name"), any(), any(), eq(Locale.ENGLISH)))
                .thenReturn("Ripple");

        processor.processPendingBatch();

        ArgumentCaptor<ReaderNotification> notificationCaptor = ArgumentCaptor.forClass(ReaderNotification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertFalse(notificationCaptor.getValue().isInAppVisible());
        verify(emailDeliveryRepository).save(any(NotificationEmailDelivery.class));
    }

    @Test
    void processIsIdempotentWhenNotificationAlreadyExists() {
        UUID seriesId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID follower = UUID.randomUUID();

        DomainEventOutbox event = DomainEventOutbox.pending(
                DomainEventTypes.CHAPTER_PUBLISHED,
                "chapter",
                chapterId,
                DomainEventTypes.CHAPTER_PUBLISHED + ":" + chapterId,
                payload(seriesId, chapterId)
        );
        when(outboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING), any(Instant.class)
        )).thenReturn(List.of(event));
        when(outboxRepository.claimPending(event.getId())).thenReturn(1);
        when(outboxRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(seriesFollowRepository.findBySeriesId(seriesId))
                .thenReturn(List.of(SeriesFollow.create(follower, seriesId)));
        when(preferenceService.resolve(follower)).thenReturn(NotificationPreference.defaults(follower));

        ReaderNotification existing = ReaderNotification.chapterPublished(
                follower, seriesId, chapterId, "t", "m", "/href", true
        );
        when(notificationRepository.findByUserIdAndTypeAndChapterId(
                follower, ReaderNotification.TYPE_CHAPTER_PUBLISHED, chapterId
        )).thenReturn(Optional.of(existing));

        processor.processPendingBatch();

        verify(notificationRepository, never()).save(any());
        verify(emailDeliveryRepository, never()).save(any());
    }

    @Test
    void processSkipsFollowersWithAllChannelsDisabled() {
        UUID seriesId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID follower = UUID.randomUUID();

        DomainEventOutbox event = DomainEventOutbox.pending(
                DomainEventTypes.CHAPTER_PUBLISHED,
                "chapter",
                chapterId,
                DomainEventTypes.CHAPTER_PUBLISHED + ":" + chapterId,
                payload(seriesId, chapterId)
        );
        when(outboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING), any(Instant.class)
        )).thenReturn(List.of(event));
        when(outboxRepository.claimPending(event.getId())).thenReturn(1);
        when(outboxRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(seriesFollowRepository.findBySeriesId(seriesId))
                .thenReturn(List.of(SeriesFollow.create(follower, seriesId)));

        NotificationPreference disabled = NotificationPreference.defaults(follower);
        disabled.update(false, false, false);
        when(preferenceService.resolve(follower)).thenReturn(disabled);

        processor.processPendingBatch();

        verify(notificationRepository, never()).save(any());
        verify(emailDeliveryRepository, never()).save(any());
        assertTrue(true);
    }

    @Test
    void processRetriesTransientFailures() {
        UUID chapterId = UUID.randomUUID();
        DomainEventOutbox event = DomainEventOutbox.pending(
                DomainEventTypes.CHAPTER_PUBLISHED,
                "chapter",
                chapterId,
                DomainEventTypes.CHAPTER_PUBLISHED + ":" + chapterId,
                payload(UUID.randomUUID(), chapterId)
        );
        when(outboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING), any(Instant.class)
        )).thenReturn(List.of(event));
        when(outboxRepository.claimPending(event.getId())).thenReturn(1);
        when(outboxRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(seriesFollowRepository.findBySeriesId(any()))
                .thenThrow(new RuntimeException("db blip"));

        processor.processPendingBatch();

        ArgumentCaptor<DomainEventOutbox> outboxCaptor = ArgumentCaptor.forClass(DomainEventOutbox.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertEquals(OutboxEventStatus.PENDING, outboxCaptor.getValue().getStatus());
        assertEquals(1, outboxCaptor.getValue().getAttemptCount());
    }

    @Test
    void buildEmailBodyUsesAbsoluteLinksAndInjectedProductName() {
        String body = ChapterPublishedOutboxProcessor.buildEmailBody(
                "Demo",
                "Chapter 1",
                "https://amarkatha.example.com/read/s/demo/c/1",
                "https://amarkatha.example.com/read/profile",
                "Ripple"
        );
        assertTrue(body.contains("on Ripple."));
        assertFalse(body.contains("AmarKatha"));
        assertTrue(body.contains("https://amarkatha.example.com/read/s/demo/c/1"));
        assertTrue(body.contains("https://amarkatha.example.com/read/profile"));
        assertFalse(body.contains("Read it here: /read/"));
    }

    @Test
    void processNotifiesAFollowerOnceAndSkipsTheSeriesCreatorForAGlimpse() {
        UUID seriesId = UUID.randomUUID();
        UUID glimpseId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID readerId = UUID.randomUUID();

        Map<String, Object> glimpsePayload = new HashMap<>();
        glimpsePayload.put("seriesId", seriesId.toString());
        glimpsePayload.put("glimpseId", glimpseId.toString());
        glimpsePayload.put("seriesSlug", "monsoon-market");
        glimpsePayload.put("seriesTitle", "Monsoon Market");
        glimpsePayload.put("tag", "CHARACTER");
        glimpsePayload.put("postedAt", Instant.now().toString());

        DomainEventOutbox event = DomainEventOutbox.pending(
                DomainEventTypes.GLIMPSE_PUBLISHED,
                "glimpse",
                glimpseId,
                DomainEventTypes.GLIMPSE_PUBLISHED + ":" + glimpseId,
                glimpsePayload
        );
        when(outboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING), any(Instant.class)
        )).thenReturn(List.of(event));
        when(outboxRepository.claimPending(event.getId())).thenReturn(1);
        when(outboxRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(
                Series.create(creatorId, "monsoon-market", "Monsoon Market")
        ));
        when(seriesFollowRepository.findBySeriesId(seriesId)).thenReturn(List.of(
                SeriesFollow.create(creatorId, seriesId),
                SeriesFollow.create(readerId, seriesId)
        ));
        when(preferenceService.resolve(readerId)).thenReturn(NotificationPreference.defaults(readerId));
        when(notificationRepository.findByUserIdAndTypeAndGlimpseId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(ReaderNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        int processed = processor.processPendingBatch();

        assertEquals(1, processed);
        ArgumentCaptor<ReaderNotification> notificationCaptor = ArgumentCaptor.forClass(ReaderNotification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        ReaderNotification saved = notificationCaptor.getValue();
        assertEquals(readerId, saved.getUserId());
        assertEquals(ReaderNotification.TYPE_GLIMPSE_PUBLISHED, saved.getType());
        assertEquals(glimpseId, saved.getGlimpseId());
        assertEquals(null, saved.getChapterId());
        assertTrue(saved.getHref().contains("#glimpse-" + glimpseId));
        verify(preferenceService, never()).resolve(creatorId);
        verify(emailDeliveryRepository, never()).save(any());
    }

    private static Map<String, Object> payload(UUID seriesId, UUID chapterId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("seriesId", seriesId.toString());
        payload.put("chapterId", chapterId.toString());
        payload.put("seriesSlug", "demo");
        payload.put("seriesTitle", "Demo");
        payload.put("chapterSlug", "chapter-1");
        payload.put("chapterTitle", "Chapter 1");
        payload.put("publishedAt", Instant.now().toString());
        return payload;
    }
}
