# DOD ComicVisibilityService: Cache-Oriented Optimization Assessment

Critical analysis of `ComicApiDod/Services/ComicVisibilityService.cs` and related code for **in-memory, CPU-cache-friendly** processing. The goal is to identify changes that can actually improve performance, and to call out where “more DOD” would be premature or harmful.

---

## 1. Current Data Flow (In-Memory Hot Path)

1. **Fetch**: `GetComicBatchDataAsync(db, comicIds)` returns `IDictionary<long, ComicBook>`.
2. **Layout**: Service builds `ComicBook[][]` by looking up each `allComicIds[i][j]` in that dictionary. So we have a 2D array of **references** to EF-loaded entities.
3. **Compute**: `ComputeVisibility(ComicBook[][] comicBooks)` loops over `(i, j)` and calls `VisibilityProcessor.ComputeVisibilities(comicBooks[i][j], computationTime)` for each comic.
4. **Inside ComputeVisibilities** (the real hot path):
   - `comicBook.Chapters.Count(c => c.IsFree)` → follows `ComicBook` → `List<Chapter>` → each `Chapter` is a separate object.
   - `comicBook.Chapters.Max(c => c.ReleaseTime)` → same.
   - `comicBook.ComicTags.Select(t => t.Tag.Name)` → `ComicBook` → `List<ComicTag>` → each `ComicTag` → `Tag` (multiple indirections).
   - `foreach (var geoRule in comicBook.GeographicRules)` → `List<GeographicRule>` (reference types).
   - `foreach (CustomerSegmentRule segmentRule in comicBook.CustomerSegmentRules)` → `List<CustomerSegmentRule>` and `segmentRule.Segment` (another object).
   - `comicBook.RegionalPricing.FirstOrDefault(...)` → `List<ComicPricing>`.

So the in-memory workload is **pointer-chasing over EF entity graphs**: each comic touch involves many small, non-contiguous objects. That is cache-unfriendly regardless of the fact that we use arrays at the “batch of requests” level (`ComicBook[][]`).

---

## 2. Why “SOA and Arrays” Alone Don’t Guarantee a Win

- **Current**: We already use arrays in places (`long[][] allComicIds`, `ComicBook[][]`, `bool[] isReqValid`). The bottleneck in the hot path is not the shape of the **container** (array vs list) but the **layout of the data inside each comic**: `ComicBook` and its navigation properties are reference types and scattered in memory.
- **Risks of naive DOD**:
  - **Extra copy**: Converting EF → some “DOD” representation after load adds a one-time cost. For very small batches (e.g. 1–2 comics per batch), that copy can dominate and **reduce** performance.
  - **Over-SOA**: Splitting into many tiny arrays (e.g. one array per field per request) can increase metadata, index arithmetic, and code complexity without improving locality.
  - **Wrong bottleneck**: If the dominant cost is DB I/O, Prometheus, or serialization, improving cache layout of the compute step will show little gain.

So: **cache-oriented changes should focus on the actual hot path (per-comic computation) and be validated with measurements.**

---

## 3. What Would Actually Help (Prioritized)

### 3.1 High value: Use struct/array data in the compute hot path

**Observation**: `DatabaseQueryHelper.GetComicBatchDataAsync` for **bulk** currently returns `IDictionary<long, ComicBook>`. There is already a **single-comic** path that projects to `ComicBatchData` (flat arrays: `ChapterData[]`, `GeographicRuleData[]`, `PricingData[]`, etc.), and that bulk path has **commented-out** code that would produce `IDictionary<long, ComicBatchData>`.

**Recommendation**:

- **After** the bulk EF query, project each `ComicBook` to `ComicBatchData` (or a similar struct-friendly type) in one pass.
- Change the pipeline so that the service works with `ComicBatchData[][]` (or a flat list keyed by request) instead of `ComicBook[][]`.
- Add `VisibilityProcessor.ComputeVisibilities(ComicBatchData batch, DateTime computationTime)` that:
  - Uses **existing** struct-based helpers: `EvaluateGeographicVisibility(in GeographicRuleData, ...)`, `DetermineContentFlags(..., ReadOnlySpan<ChapterData>, in PricingData?)`, and an overload for segment visibility that takes `CustomerSegmentRuleData` + lookup into `CustomerSegmentData[]` for `IsActive`.
  - Iterates over `batch.Chapters`, `batch.GeographicRules`, `batch.SegmentRules`, `batch.RegionalPricing` as **contiguous arrays** (or `ReadOnlySpan<>`), avoiding `List<>` and navigation property dereferences.

**Why this helps**: When processing one comic, all chapter/rule/pricing data for that comic are in a few contiguous regions instead of many small heap objects. That improves cache line utilization and reduces pointer chasing. The cost is a **single projection pass** per batch after the DB round-trip.

**When it might not pay off**: Very small batches (e.g. 1–2 comics) where projection + extra allocation outweigh the compute savings. **Measure** (e.g. batch size 1 vs 10 vs 50) before committing.

---

### 3.2 High value: Remove or sample per-item metrics in the inner loop

**Current**: In `ComputeVisibility`, for **every** comic we do:

```csharp
var itemSw = Stopwatch.StartNew();
// ... compute ...
OperationDuration
    .WithLabels("compute_visibility_item", ...)
    .Observe(itemSw.Elapsed.TotalSeconds);
```

That is **Stopwatch + Prometheus label lookup + Observe** in the innermost loop. For batches of tens/hundreds of comics, this can be a large share of CPU and can dwarf gains from better memory layout.

**Recommendation**:

- Either **remove** per-item duration from the hot path and keep only the **per-batch** `compute_visibility` metric, or
- **Sample** (e.g. record every Nth item or only on first/last item) so the hot path cost is negligible.

This is low-risk and often gives a clear win without any DOD changes.

---

### 3.3 Medium value: Single-pass flatten in SaveComputedVisibility

**Current**:

```csharp
ComputedVisibilityData[] visibilityResultsToSave = visibilityResults
    .SelectMany(results => results
        .Where(res => res is { ComputedVisibilities: not null })
        .SelectMany(res => res.ComputedVisibilities))
    .ToArray();
```

Multiple enumerations and a large allocation at the end. For big batches this can add avoidable allocations and passes.

**Recommendation**: One pass over `visibilityResults` (nested loops), appending to a `List<ComputedVisibilityData>` or a pre-sized array. Minor improvement, but keeps the pipeline tidy.

---

### 3.4 Lower priority: Flatten comic processing to a single list

Processing order is currently “by request, then by comic within request” (`comicBooks[i][j]`). The main cache cost is **per-comic** (touching `ComicBook` and its children); switching to a single flat list of comics might slightly improve locality when combined with struct-based comic data, but the gain is secondary compared to (3.1). Only consider if (3.1) is already in place and profiling still shows cache pressure.

---

## 4. What to Avoid

- **Blind SOA at the “batch of requests” level**: e.g. one giant array of all comic IDs, one array of all StartIds, etc. The current structure (per-request arrays, `originalIndices`) is already reasonable; the pain is inside **per-comic** data layout, not the shape of the batch.
- **Converting to SOA without measuring**: Any change (projection to `ComicBatchData`, new overloads in `VisibilityProcessor`) should be **benchmarked** (e.g. same batch sizes, same DB) before and after. Otherwise we risk premature optimization or regressions for small batches.
- **Over-optimizing cold paths**: Validation, sorting, filtering, and response assembly are not in the same league as the double loop + `ComputeVisibilities`. Focus on the hot path first.

---

## 5. Summary

| Change | Expected effect | Risk |
|--------|------------------|------|
| Use **ComicBatchData** (or equivalent) after bulk fetch and **ComputeVisibilities(batch)** with struct/array iteration | Better cache locality in per-comic compute | Extra projection cost; can hurt for tiny batches — measure. |
| **Remove or sample** per-item `OperationDuration` in the inner loop | Lower CPU and less overhead in hot path | Low. |
| **Single-pass** flatten when building `visibilityResultsToSave` | Fewer allocations and passes | Low. |
| Flatten processing to one list of comics | Possible small locality gain | Low; do after the above. |
| More SOA at request level (e.g. big global arrays) | Unclear benefit, higher complexity | Skip unless data shows need. |

**Bottom line**: The service can be made more data-oriented and cache-friendly by (1) **projecting EF results to a struct/array representation** and (2) **running the visibility computation on that representation** using the existing struct-based helpers, and by (3) **reducing per-item metrics** in the inner loop. Doing “more SOA” without fixing the per-comic layout and without measuring is likely to add complexity without a real performance gain, or even to regress for small batches.
