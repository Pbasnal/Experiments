package com.amarkatha.outbox;

import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Appends durable domain events in the caller's transaction.
 */
@Service
public class DomainEventPublisher {

    private final DomainEventOutboxRepository repository;

    public DomainEventPublisher(DomainEventOutboxRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void append(
            String eventType,
            String aggregateType,
            UUID aggregateId,
            String dedupeKey,
            Map<String, Object> payload
    ) {
        if (repository.findByDedupeKey(dedupeKey).isPresent()) {
            return;
        }
        try {
            repository.save(DomainEventOutbox.pending(
                    eventType,
                    aggregateType,
                    aggregateId,
                    dedupeKey,
                    payload
            ));
        } catch (DataIntegrityViolationException ignored) {
            // concurrent insert with same dedupe_key
        }
    }
}
