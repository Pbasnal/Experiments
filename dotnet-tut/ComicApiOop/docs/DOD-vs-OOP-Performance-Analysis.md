# DOD vs OOP API: Performance Analysis

This document compares the two implementations of the Comic API (Data-Oriented Design vs Object-Oriented) and explains the performance differences, addresses correctness concerns, and outlines options to reduce OOP database load.

---

## 1. Are the DOD APIs Actually Performing the Work and Returning Proper Results?

**Yes.** The DOD flow is request–response correct.

- **HTTP handler** ([ComicApiDod/Handlers/ComicRequestHandler.cs](ComicApiDod/Handlers/ComicRequestHandler.cs)): Validates input, builds a `VisibilityComputationRequest` (with a `TaskCompletionSource<VisibilityComputationResponse>`), enqueues it, then **awaits `request.ResponseSrc.Task`** (with a 10s timeout). The HTTP request does not complete until that task is set.
- **Batch processor** ([ComicApiDod/Services/ComicVisibilityService.cs](ComicApiDod/Services/ComicVisibilityService.cs)): For each batch of requests it validates, fetches comic data in bulk, computes visibilities, saves in bulk, then calls **`SetResponse(reqs, ...)`**, which for each request calls **`reqs[i].ResponseSrc.TrySetResult(new VisibilityComputationResponse { ... Results = ... })`**. So when the batch that contains a given request finishes, that request’s TCS is completed and the handler’s `await` returns.
- **Result mapping**: `SetResponse` uses `originalIndices` so each HTTP request gets the visibility results that correspond to its `(startId, limit)`.

So DOD does the real work and returns the correct results for each client. If you observe **errors rate = 100%** in tests, that likely comes from something else (e.g. timeouts, validation failures, or load-test assertions), not from “DOD not doing the work.” It’s worth checking which status codes and labels (e.g. `timeout`, `failure`) are incremented in that scenario.

---

## 2. Why Is DOD Faster Even Though Within a Batch It Processes Comics Sequentially?

The main performance gains come from **fewer database round-trips** and **parallel batch processing**, not from parallelism inside a single batch.

### 2.1 Database Round-Trips Per “Bulk” Operation

| Aspect | OOP | DOD |
|--------|-----|-----|
| **Get comic IDs** | 1 query: `Where(Id >= startId).Take(limit).Select(Id)` | 0 queries (IDs derived in memory from `startId + j`) |
| **Fetch comic data** | **N queries** (one per comic): each `ComputeVisibilityForComicAsync` calls `FetchComicWithAllDataAsync(comicId)` with a large `Include(...)` query | **1 query** for the whole batch: `GetComicBatchDataAsync(db, comicIds)` – single `Where(c => ids.Contains(c.Id)).Include(...).ToListAsync()` |
| **Save visibilities** | **N saves** (one per comic): each comic calls `SaveComputedVisibilitiesAsync` → `AddRangeAsync` + `SaveChangesAsync()` | **1 save** for the whole batch: `SaveComputedVisibilitiesBulkAsync(db, allVisibilities)` – one `AddRange` + one `SaveChangesAsync()` |
| **Total round-trips for a bulk of N comics** | **1 + 2N** | **2** |

So for e.g. **limit = 10**, OOP does **21** round-trips and DOD does **2**. That alone explains most of the latency difference.

### 2.2 Request and Batch Processing Model

- **OOP**: One HTTP request = one call to `ComputeVisibilitiesBulkAsync(startId, limit)`. That method runs entirely on that request’s thread/context: get IDs (1 query), then **sequential** `await ComputeVisibilityForComicAsync(comicId)` for each comic (2 queries each). No batching across HTTP requests; no parallelism.
- **DOD**: HTTP request only enqueues a single “logical” request `(startId, limit)` and waits on its TCS. A **background** loop dequeues **batches of such requests** (e.g. up to 10). For each batch it does:
  - 1 bulk fetch for all comics in the batch
  - Sequential in-memory computation over comics in the batch
  - 1 bulk save  
  So **per batch**, work is sequential, but **multiple batches run in parallel** (after the queue change). So total throughput is higher, and each batch is cheap because it does only 2 DB round-trips.

So:

- **Within a batch**, DOD is sequential, but that batch is already very efficient (2 DB calls).
- **Across batches**, DOD runs many batches concurrently, so more work per second.
- OOP does 2N+1 DB calls **sequentially** in a single request, so it’s both more round-trips and no parallelism.

That’s why DOD can be much faster even with “same structure, sequential within batch.”

### 2.3 Summary: Why DOD Wins

1. **Batching**: One fetch and one save per batch instead of per comic.
2. **Parallel batches**: Many batches in flight at once (queue fires callbacks without awaiting).
3. **No “get IDs” query**: DOD derives IDs from `(startId, limit)`; OOP does an extra query to resolve IDs.
4. **Single HTTP request** in OOP holds the connection for the full 1 + 2N sequence; DOD offloads to background batches and only waits on the TCS for that one logical request.

---

## 3. Are There More DB Queries Per Request in OOP? Can We Reduce Them?

**Yes.** For a bulk of N comics, OOP does **1 + 2N** DB operations (1 for IDs, N fetches, N saves). DOD does **2** per batch (1 fetch, 1 save).

### 3.1 Reducing Queries in the OOP API

You can move OOP toward the same pattern as DOD without changing the public API:

1. **Batch fetch comics (one query)**  
   After getting `comicIds` (keep the existing 1 query or derive IDs like DOD):
   - Replace the per-comic `FetchComicWithAllDataAsync(comicId)` loop with **one** query that loads all comics and related data, e.g.  
     `Where(c => comicIds.Contains(c.Id)).Include(...).AsNoTracking().ToListAsync()`  
   - Build a `Dictionary<long, ComicBook>` or similar and, in the loop, use `comic = map[comicId]` instead of a new query per comic.

2. **Batch save visibilities (one save)**  
   - Collect all `ComputedVisibilityData` (or entities) from **all** comics in the bulk.
   - Call **one** `AddRangeAsync` for the whole list and **one** `SaveChangesAsync()` after the loop, instead of calling `SaveComputedVisibilitiesAsync` inside each iteration.

3. **Optional: derive IDs in memory**  
   If you’re comfortable with the same semantics as DOD (IDs = `startId, startId+1, ... startId+limit-1`), you can skip the “get comic IDs” query and derive the list in memory. Then OOP would use **2** round-trips per bulk (1 fetch, 1 save), same as DOD from a DB perspective.

After these changes, OOP would do **2 (or 3)** round-trips per bulk instead of **1 + 2N**, which should greatly reduce latency and bring it closer to DOD for the same workload.

---

## 4. Side-by-Side Comparison

| Dimension | OOP | DOD |
|-----------|-----|-----|
| **Request model** | Synchronous: HTTP handler calls service and awaits full bulk computation | Asynchronous: handler enqueues one logical request, awaits its TCS; background batches process many such requests |
| **DB queries per bulk (N comics)** | 1 (IDs) + N (fetch comic) + N (save) = **1 + 2N** | 1 (bulk fetch) + 1 (bulk save) = **2** |
| **Parallelism** | None within a request; one request = one sequential chain | Multiple batches in parallel; within a batch, comics are processed sequentially |
| **Comic data fetch** | One query per comic (`FetchComicWithAllDataAsync`) | One query for all comics in the batch (`GetComicBatchDataAsync`) |
| **Visibility save** | One `AddRange` + `SaveChanges` per comic | One `AddRange` + `SaveChanges` per batch |
| **Computation logic** | Same semantics (visibility rules, pricing, etc.); OOP uses `VisibilityComputationService.ComputeVisibilities`, DOD uses `VisibilityProcessor.ComputeVisibilities` with pre-loaded data |
| **Correctness** | Returns `BulkVisibilityComputationResult` with per-comic results | Returns `VisibilityComputationResponse` per request via TCS; results are correctly mapped in `SetResponse` |

---

## 5. About Your Perf and Error Metrics

- **comic_visibility_computation_duration p(95)=76.54ms** and **http_req_duration p(95)=71.02ms** show that from the client’s perspective, DOD is fast; the handler’s wait on `ResponseSrc.Task` is short when the batch completes quickly.
- **errors rate = 100%** is inconsistent with “performing well” unless “errors” in your test mean something specific (e.g. failed assertions, timeouts, or a single error path). Recommend checking:
  - What the load test marks as “error” (e.g. status code, or custom threshold).
  - Prometheus labels for `request_count_total_dod` / `request_count_total_oop` and any error counters to see success vs failure and timeouts.

Once that’s clear, the same document and batching changes above still apply: DOD is doing the work and returning proper results; the main levers for OOP are reducing round-trips (batch fetch + batch save) and optionally adding concurrency or different request boundaries if needed.
