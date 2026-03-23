# Production readiness: request coalescing

This note lists features and improvements that would move the current **queue + batch listener + fan-out** design from an experiment toward something you could run with confidence in production. It is scoped to the coalescing layer (queue, batch worker, handler integration, and batch processing contract), not to the whole API surface.

---

## Reliability and correctness

- **Await the batch callback.** The batch processor should `await` the async callback so batches are processed sequentially (or under explicit concurrency control) and exceptions are observed. Fire-and-forget batch work causes races and hides failures.

- **No silent catch in the dequeue loop.** Swallowing all exceptions in the inner batch loop leaves callers waiting with no completion signal. Failures should be logged, metered, and every waiter in the batch should get an explicit failure (exception on `TaskCompletionSource`, or a structured error result), not hang until an HTTP timeout.

- **Guaranteed completion on every path.** If `ComputeVisibilities` throws before `SetResponse`, every enqueued request in that batch must still be completed (success, validation error, or failure). Today, a thrown exception can strand waiters.

- **Propagate cancellation.** Wire `HttpContext.RequestAborted` (and/or a linked token from shutdown) into the visibility computation and DB calls so clients that disconnect do not keep doing full batch work when possible, and so shutdown can cancel in-flight work safely.

- **Idempotency and side effects.** Batched persistence (e.g. saves after compute) should be safe under retries and partial failures: clear transactional boundaries, deduplication keys, or “at-most-once” documentation where applicable.

---

## Backpressure and capacity

- **Bounded queue.** An unbounded `ConcurrentQueue` accepts arbitrary memory growth under spikes. Production systems need a maximum depth; when full, reject fast with **429** or **503** (and metrics), or shed load according to policy.

- **Admission control.** Optional limits per tenant, per route, or per API key so one noisy neighbor cannot fill the coalescing queue for everyone.

- **Timeouts aligned with SLAs.** Per-request timeouts (e.g. handler wait vs. batch processing time) should be configurable and consistent: batch work must not routinely exceed what the outer timeout promises.

---

## Horizontal scaling and deployment

- **Single-process coalescing.** The current design coalesces only within one process instance. Multiple replicas each have their own queue, so you do not get cross-instance batching. For scale-out, document this limitation or move to a **shared work queue** (message broker, Redis streams, etc.) with a defined partitioning strategy.

- **Sticky routing (if staying in-process).** If you keep per-instance queues, load balancers may need session affinity for workloads that assume locality—only where the product requires it; often a shared queue is simpler.

- **Rolling deploys.** During deploys, in-flight batches and queued items need a defined story: drain, fail with retryable error, or migrate to another consumer.

---

## Observability

- **Queue depth and wait time metrics.** Export current queue length, time-from-enqueue-to-batch-start, batch size distribution, and batch duration. You already measure some wait time; surface **saturation** (how often the queue is near its cap).

- **Distributed tracing.** Propagate trace/span context from each HTTP request into the batch worker so one trace can show “many requests → one batch → fan-out.”

- **Structured logging per logical request.** Batch logs should correlate child request IDs, not only “batch N processed.”

- **Alerting.** Alerts on sustained queue growth, rising batch failure rate, growth in timed-out waiters, and worker task crashes.

---

## Configuration and operability

- **Externalize tuning knobs.** Batch size, max batching wait (`Dequeue` window), empty-queue poll delay, handler timeout, and shutdown drain timeout should live in configuration with safe defaults and documented trade-offs (latency vs. throughput).

- **Feature flags.** Ability to disable coalescing per environment or percentage of traffic for safe rollout and A/B comparison.

- **Health checks.** Liveness: batch listener task is running. Readiness: optionally require queue depth below a threshold and DB reachable.

---

## Graceful shutdown

- **Stop accepting new work** while allowing a bounded drain of the queue, or fail fast with a clear **503** during shutdown.

- **Cancel listeners and wait** for the in-flight batch to finish or cancel cooperatively, with a hard cap (you already use a timeout on `StopAsync`; extend with explicit “fail all pending waiters” after the cap).

- **Avoid `CancellationToken.None` placeholders** in domain code paths where real shutdown or client abort tokens should flow.

---

## Security and multi-tenancy

- **Batch mixing policy.** Document whether unrelated tenants may appear in the same batch; if not, partition queues or batch keys by tenant.

- **Least privilege** on the DB user used for batched queries; validate that batch SQL cannot widen access across tenants by mistake.

---

## Performance and cost

- **Allocation control.** Reuse buffers or pools for per-batch lists where profiling shows GC pressure (your article noted GC wins; keep that profile under load).

- **Adaptive batching (optional).** Under low load, smaller batches or shorter waits; under high load, larger batches within latency budgets—requires metrics-driven tuning.

- **Key-based coalescing (optional).** If many concurrent requests ask for the **same** keys, deduplicate fetches and fan out results (different from “pack next N requests from the queue”).

---

## Testing and safety nets

- **Unit tests** for batch mapping: ordering after sort, validation fan-out, and failure paths that must complete every `TaskCompletionSource`.

- **Integration tests** under concurrency: many callers, empty queue, full batch, single-item batch, timeout, and worker crash simulation.

- **Load and chaos tests** with queue near capacity, slow DB, and replica restarts.

- **Property or fuzz tests** for ranges of `startId` / `limit` and null-slot handling if the queue can ever yield sparse batches.

---

## API and client contract

- **Document added latency variance.** Clients should expect slightly higher tail latency under batching rules and know when to use a non-coalesced path if you offer one.

- **Error semantics.** Distinguish **timeouts** (504), **overload** (429/503), **validation** (400), and **internal batch failure** (500) so clients can retry appropriately.

---

## Code quality and maintainability

- **Thread-safe registration.** If queues or listeners can be registered from multiple threads at startup, protect shared dictionaries or enforce single-threaded startup ordering.

- **Replace generic `Exception` throws** with specific exception types or result types for “queue not registered” and similar programmer errors.

- **Typed callbacks.** Tighten the `IValue` / callback contract if it is mostly historical so batch processors have a clear, testable API.

---

## Summary

The highest-impact items for production are usually: **bounded queues with backpressure**, **never leaving waiters hanging**, **awaiting and surfacing batch errors**, **cancellation and shutdown behavior**, and **metrics plus tracing** so you can see queue health and batch behavior under real traffic. Horizontal scale and tenant isolation follow from product requirements once the single-node path is robust.
