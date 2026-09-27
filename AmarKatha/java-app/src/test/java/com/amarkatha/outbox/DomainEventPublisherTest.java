package com.amarkatha.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private DomainEventOutboxRepository repository;

    private DomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new DomainEventPublisher(repository);
    }

    @Test
    void appendWritesPendingEvent() {
        when(repository.findByDedupeKey("CHAPTER_PUBLISHED:1")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID aggregateId = UUID.randomUUID();
        publisher.append(
                DomainEventTypes.CHAPTER_PUBLISHED,
                "chapter",
                aggregateId,
                "CHAPTER_PUBLISHED:1",
                Map.of("chapterId", aggregateId.toString())
        );

        ArgumentCaptor<DomainEventOutbox> captor = ArgumentCaptor.forClass(DomainEventOutbox.class);
        verify(repository).save(captor.capture());
        assertEquals(OutboxEventStatus.PENDING, captor.getValue().getStatus());
        assertEquals(DomainEventTypes.CHAPTER_PUBLISHED, captor.getValue().getEventType());
    }

    @Test
    void appendIsIdempotentOnDedupeKey() {
        when(repository.findByDedupeKey("CHAPTER_PUBLISHED:1"))
                .thenReturn(Optional.of(DomainEventOutbox.pending(
                        DomainEventTypes.CHAPTER_PUBLISHED,
                        "chapter",
                        UUID.randomUUID(),
                        "CHAPTER_PUBLISHED:1",
                        Map.of()
                )));

        publisher.append(
                DomainEventTypes.CHAPTER_PUBLISHED,
                "chapter",
                UUID.randomUUID(),
                "CHAPTER_PUBLISHED:1",
                Map.of()
        );

        verify(repository, never()).save(any());
    }
}
