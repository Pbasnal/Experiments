package com.amarkatha.engagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amarkatha.engagement.domain.NotificationEmailDelivery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class NotificationEmailDeliveryProcessorTest {

    @Mock
    private NotificationEmailDeliveryRepository deliveryRepository;
    @Mock
    private NotificationMailSender mailSender;
    @Mock
    private PlatformTransactionManager transactionManager;

    private NotificationEmailDeliveryProcessor processor;

    @BeforeEach
    void setUp() {
        NotificationMetrics metrics = new NotificationMetrics(new SimpleMeterRegistry());
        NotificationProperties properties = new NotificationProperties(5_000, 10_000, 3, 50, 50, 100, 300_000);
        // Execute callbacks synchronously without a real transaction.
        org.mockito.Mockito.lenient()
                .when(transactionManager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus());
        processor = new NotificationEmailDeliveryProcessor(
                deliveryRepository,
                mailSender,
                properties,
                metrics,
                transactionManager
        );
    }

    @Test
    void leavesPendingWhenMailDisabled() throws Exception {
        when(mailSender.isEnabled()).thenReturn(false);

        int processed = processor.processPendingBatch();

        assertEquals(0, processed);
        verify(deliveryRepository, never()).claimPending(any(), any());
        verify(mailSender, never()).send(any(), any(), any());
    }

    @Test
    void claimCommitsBeforeSendAndMarksSentSeparately() throws Exception {
        NotificationEmailDelivery delivery = NotificationEmailDelivery.pending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "EMAIL:key",
                "a@example.com",
                "Subject",
                "Body"
        );
        when(deliveryRepository.recoverStaleProcessing(any(Instant.class))).thenReturn(0);
        when(deliveryRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(NotificationEmailDelivery.Status.PENDING), any(Instant.class)
        )).thenReturn(List.of(delivery));
        when(deliveryRepository.claimPending(eq(delivery.getId()), any(Instant.class))).thenReturn(1);
        when(deliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(mailSender.isEnabled()).thenReturn(true);

        processor.processPendingBatch();

        var inOrder = org.mockito.Mockito.inOrder(deliveryRepository, mailSender);
        inOrder.verify(deliveryRepository).claimPending(eq(delivery.getId()), any(Instant.class));
        inOrder.verify(mailSender).send("a@example.com", "Subject", "Body");
        ArgumentCaptor<NotificationEmailDelivery> captor =
                ArgumentCaptor.forClass(NotificationEmailDelivery.class);
        inOrder.verify(deliveryRepository).save(captor.capture());
        assertEquals(NotificationEmailDelivery.Status.SENT, captor.getValue().getStatus());
    }

    @Test
    void recoversStaleProcessingBeforeClaiming() throws Exception {
        when(mailSender.isEnabled()).thenReturn(true);
        when(deliveryRepository.recoverStaleProcessing(any(Instant.class))).thenReturn(2);
        when(deliveryRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(NotificationEmailDelivery.Status.PENDING), any(Instant.class)
        )).thenReturn(List.of());

        processor.processPendingBatch();

        verify(deliveryRepository).recoverStaleProcessing(any(Instant.class));
        verify(deliveryRepository, never()).claimPending(any(), any());
    }

    @Test
    void retriesTransientSendFailures() throws Exception {
        NotificationEmailDelivery delivery = NotificationEmailDelivery.pending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "EMAIL:key",
                "a@example.com",
                "Subject",
                "Body"
        );
        when(deliveryRepository.recoverStaleProcessing(any(Instant.class))).thenReturn(0);
        when(deliveryRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(NotificationEmailDelivery.Status.PENDING), any(Instant.class)
        )).thenReturn(List.of(delivery));
        when(deliveryRepository.claimPending(eq(delivery.getId()), any(Instant.class))).thenReturn(1);
        when(deliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(mailSender.isEnabled()).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
                .when(mailSender).send(any(), any(), any());

        processor.processPendingBatch();

        ArgumentCaptor<NotificationEmailDelivery> captor =
                ArgumentCaptor.forClass(NotificationEmailDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertEquals(NotificationEmailDelivery.Status.PENDING, captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getAttemptCount());
        assertTrue(captor.getValue().getLastError().contains("smtp down"));
    }
}
