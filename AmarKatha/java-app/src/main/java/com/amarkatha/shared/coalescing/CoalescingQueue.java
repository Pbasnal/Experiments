package com.amarkatha.shared.coalescing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Batches concurrent identical-class reads (catalog, series detail) per architecture doc.
 * V0 defaults: max 10 requests, max 5 ms wait.
 */
public final class CoalescingQueue<K, V> implements AutoCloseable {

    public record PendingRequest<K, V>(K key, CompletableFuture<V> future) {
    }

    private final int maxBatchSize;
    private final Duration maxWait;
    private final ConcurrentLinkedQueue<PendingRequest<K, V>> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "coalescing-queue");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean drainScheduled;

    public CoalescingQueue(int maxBatchSize, Duration maxWait) {
        this.maxBatchSize = maxBatchSize;
        this.maxWait = maxWait;
    }

    public static <K, V> CoalescingQueue<K, V> catalogDefaults() {
        return new CoalescingQueue<>(10, Duration.ofMillis(5));
    }

    public CompletableFuture<V> enqueue(K key, Function<List<K>, Map<K, V>> batchLoader) {
        CompletableFuture<V> future = new CompletableFuture<>();
        queue.add(new PendingRequest<>(key, future));
        scheduleDrain(batchLoader);
        return future;
    }

    private synchronized void scheduleDrain(Function<List<K>, Map<K, V>> batchLoader) {
        if (drainScheduled) {
            return;
        }
        drainScheduled = true;
        scheduler.schedule(() -> drain(batchLoader), maxWait.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void drain(Function<List<K>, Map<K, V>> batchLoader) {
        drainScheduled = false;
        List<PendingRequest<K, V>> batch = new ArrayList<>(maxBatchSize);
        PendingRequest<K, V> item;
        while (batch.size() < maxBatchSize && (item = queue.poll()) != null) {
            batch.add(item);
        }
        if (batch.isEmpty()) {
            return;
        }
        List<K> keys = batch.stream().map(PendingRequest::key).toList();
        Map<K, V> results;
        try {
            results = batchLoader.apply(keys);
        } catch (Exception ex) {
            batch.forEach(p -> p.future().completeExceptionally(ex));
            return;
        }
        for (PendingRequest<K, V> pending : batch) {
            V value = results.get(pending.key());
            if (value != null) {
                pending.future().complete(value);
            } else {
                pending.future().completeExceptionally(
                        new CoalescingKeyNotFoundException(pending.key()));
            }
        }
        if (!queue.isEmpty()) {
            scheduleDrain(batchLoader);
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
