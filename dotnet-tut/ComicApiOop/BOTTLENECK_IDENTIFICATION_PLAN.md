# Bottleneck Identification Plan

## 🎯 Goal
Identify why DoD version is slower in production despite being 4x faster in pure computation (benchmark results).

## 📊 Current State
- ✅ Prometheus metrics already set up
- ✅ Grafana dashboards exist
- ✅ k6 load tests available
- ✅ Docker database setup ready
- ✅ Benchmark shows DoD is 4x faster in computation

## 🚀 Recommended Approach: **Start Simple, Scale Up**

### Phase 1: Quick Wins (30 minutes) ⚡ **START HERE**

#### 1.1 Enable EF Core Query Logging (Simplest)
**Why**: See actual SQL queries, execution times, and query counts without code changes.

**Steps**:
```csharp
// In Program.cs or Startup
builder.Services.AddDbContext<ComicDbContext>(options =>
{
    options.UseMySql(connectionString, serverVersion)
        .EnableSensitiveDataLogging()  // Shows parameter values
        .LogTo(Console.WriteLine, LogLevel.Information);  // Logs all queries
});
```

**What you'll see**:
- Exact SQL queries executed
- Query execution time
- Number of queries per request
- Parameter values

**Expected findings**:
- N+1 queries?
- Missing indexes?
- Inefficient joins?
- Too many queries vs OOP?

#### 1.2 Add Detailed Operation Timing (Already Partially Done)
**Enhance existing metrics** to break down database operations:

**Add to `DatabaseQueryHelper.GetComicBatchDataAsync()`**:
```csharp
var swTotal = Stopwatch.StartNew();
var swQuery = Stopwatch.StartNew();

// Main query
var comics = await db.Comics...ToListAsync();
DbQueryDuration.WithLabels("fetch_comics").Observe(swQuery.Elapsed.TotalSeconds);

swQuery.Restart();
// Any additional queries
// ... 
DbQueryDuration.WithLabels("fetch_segments").Observe(swQuery.Elapsed.TotalSeconds);

// Total time
DbQueryDuration.WithLabels("total_fetch").Observe(swTotal.Elapsed.TotalSeconds);
```

**Metrics to add**:
- `db_query_count_total` - Number of queries per operation
- `db_query_duration_seconds` - Per-query timing (already exists, enhance it)
- `ef_change_tracker_entities` - Number of tracked entities

#### 1.3 Use .NET Diagnostic Tools (No Code Changes)
**Run during load test**:
```bash
# Monitor counters in real-time
dotnet-counters monitor --process-id <pid> \
  System.Runtime \
  Microsoft.EntityFrameworkCore

# Or use PerfView/Visual Studio Diagnostic Tools
```

**What to watch**:
- `ef-core-queries` - Query count
- `ef-core-savechanges` - SaveChanges calls
- `gc-heap-size` - Memory pressure
- `cpu-usage` - CPU bottlenecks

---

### Phase 2: Enhanced Metrics (1-2 hours) 📈

#### 2.1 Add EF Core Interceptor for Query Tracking
**Create `QueryMetricsInterceptor.cs`**:
```csharp
public class QueryMetricsInterceptor : DbCommandInterceptor
{
    private static readonly Histogram QueryDuration = Metrics.CreateHistogram(
        "ef_query_duration_seconds",
        "EF Core query duration",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.001, 2, 12),
            LabelNames = new[] { "query_type", "table" }
        });

    public override async ValueTask<InterceptionResult<DbDataReader>> ReaderExecutingAsync(
        DbCommand command, CommandEventData eventData, 
        InterceptionResult<DbDataReader> result, CancellationToken cancellationToken = default)
    {
        var sw = Stopwatch.StartNew();
        var actualResult = await base.ReaderExecutingAsync(command, eventData, result, cancellationToken);
        
        QueryDuration
            .WithLabels(ExtractQueryType(command.CommandText), ExtractTable(command.CommandText))
            .Observe(sw.Elapsed.TotalSeconds);
            
        return actualResult;
    }
}
```

**Benefits**:
- Automatic tracking of ALL queries
- No manual instrumentation needed
- See query patterns

#### 2.2 Add Change Tracker Metrics
**Track EF Core overhead**:
```csharp
private static readonly Gauge ChangeTrackerEntities = Metrics.CreateGauge(
    "ef_change_tracker_entities",
    "Number of entities being tracked",
    new GaugeConfiguration { LabelNames = new[] { "api_type" } });

// In DatabaseQueryHelper, after query:
ChangeTrackerEntities.WithLabels("DOD").Set(db.ChangeTracker.Entries().Count());
```

#### 2.3 Add Memory Allocation Tracking
**Track memory per operation**:
```csharp
var memoryBefore = GC.GetTotalMemory(false);
// ... operation ...
var memoryAfter = GC.GetTotalMemory(false);
var allocated = memoryAfter - memoryBefore;

MemoryAllocatedBytes.WithLabels("fetch_batch_data").Set(allocated);
```

---

### Phase 3: Grafana Dashboards (1 hour) 📊

#### 3.1 Create Comparison Dashboard
**Panels to add**:

1. **Query Performance Panel**
   ```
   rate(ef_query_duration_seconds_sum[5m]) / rate(ef_query_duration_seconds_count[5m])
   ```
   - Compare DoD vs OOP query times
   - Group by query_type

2. **Query Count Panel**
   ```
   sum(rate(ef_query_count_total[5m])) by (api_type)
   ```
   - See if DoD makes more queries

3. **Operation Breakdown**
   ```
   histogram_quantile(0.95, comic_visibility_operation_duration_seconds)
   ```
   - Compare each operation (fetch, compute, save)

4. **Memory Allocation**
   ```
   sum(rate(dotnet_memory_allocated_bytes[5m])) by (api_type)
   ```

5. **Change Tracker Overhead**
   ```
   ef_change_tracker_entities
   ```

#### 3.2 Side-by-Side Comparison
- Create two columns: DoD | OOP
- Same panels for each
- Easy visual comparison

---

### Phase 4: k6 Load Testing (30 minutes) 🚀

#### 4.1 Create Comparison Test
**Modify existing k6 scripts** to:
- Test both APIs simultaneously
- Use same data/parameters
- Compare response times

**k6 script structure**:
```javascript
import http from 'k6/http';
import { check } from 'k6';

export default function () {
  // Test DoD
  const dodRes = http.get('http://dod-api/api/comics/compute-visibilities?startId=1&limit=20');
  check(dodRes, { 'DoD status 200': (r) => r.status === 200 });
  
  // Test OOP
  const oopRes = http.get('http://oop-api/api/comics/compute-visibilities?startId=1&limit=20');
  check(oopRes, { 'OOP status 200': (r) => r.status === 200 });
}
```

#### 4.2 Run Tests
```bash
# Run both APIs
docker-compose up -d

# Run k6 test
k6 run --vus 10 --duration 60s load-test-comparison.js

# Check Grafana during test
```

---

## 🔍 What to Look For

### Red Flags (Likely Issues):

1. **Query Count Mismatch**
   - DoD: 2-3 queries per batch
   - OOP: 20+ queries (N+1 problem)
   - **If DoD has MORE queries → problem!**

2. **Change Tracker Overhead**
   - DoD tracking 1000+ entities → slow
   - Should be 0 with `.AsNoTracking()`

3. **Query Execution Time**
   - DoD queries taking 100ms+ → slow DB or missing indexes
   - OOP queries taking 5ms → optimized

4. **Memory Pressure**
   - High GC counts during DoD → too many allocations
   - Memory growing → memory leak

5. **Operation Breakdown**
   - If `fetch_batch_data` is slow → DB issue
   - If `compute_visibility` is slow → computation issue (unlikely based on benchmark)
   - If `save_computed_visibility` is slow → DB write issue

---

## 🎯 Recommended Execution Order

### **Option A: Quickest Path (Recommended)**
1. ✅ Enable EF Core logging (5 min)
2. ✅ Run single request, check logs
3. ✅ Compare query counts/times
4. ✅ Fix obvious issues
5. ✅ Re-test

**Time**: 30-60 minutes  
**Likely to find**: Query count issues, missing `.AsNoTracking()`, N+1 queries

### **Option B: Comprehensive (If Option A doesn't reveal issues)**
1. ✅ Phase 1 (EF logging + timing)
2. ✅ Phase 2 (Enhanced metrics)
3. ✅ Phase 3 (Grafana dashboards)
4. ✅ Phase 4 (k6 load test)
5. ✅ Analyze all data

**Time**: 3-4 hours  
**Likely to find**: Subtle issues, memory pressure, scaling problems

---

## 🛠️ Implementation Priority

### Must Have (Do First):
1. **EF Core Query Logging** - See what's actually happening
2. **Operation Timing Breakdown** - Already partially done, enhance it
3. **Query Count Metrics** - Compare DoD vs OOP

### Nice to Have (If needed):
4. EF Core Interceptor - Automatic query tracking
5. Change Tracker Metrics - Track EF overhead
6. Grafana Dashboards - Visual comparison
7. k6 Load Tests - Real-world comparison

---

## 📝 Expected Findings Based on Benchmark

Since **computation is 4x faster**, the bottleneck is likely:

1. **Database Queries** (80% probability)
   - More queries in DoD
   - Slower queries in DoD
   - Missing `.AsNoTracking()`
   - Inefficient Include() chains

2. **EF Core Overhead** (15% probability)
   - Change tracking overhead
   - Entity materialization overhead
   - Relationship fixup overhead

3. **Memory/GC Pressure** (5% probability)
   - Too many allocations
   - GC pauses
   - Memory leaks

---

## 🚨 Quick Diagnostic Commands

```bash
# 1. Check query logs (if EF logging enabled)
docker logs comic-api-dod | grep "Executing"

# 2. Check Prometheus metrics
curl http://localhost:9090/api/v1/query?query=comic_visibility_db_query_duration_seconds

# 3. Monitor in real-time
watch -n 1 'curl -s http://localhost:9090/api/v1/query?query=rate\(comic_visibility_db_query_duration_seconds_count\[1m\]\)'

# 4. Compare query counts
# DoD
curl http://localhost:9090/api/v1/query?query=sum\(rate\(ef_query_count_total\{api_type=\"DOD\"\}\[5m\]\)\)
# OOP  
curl http://localhost:9090/api/v1/query?query=sum\(rate\(ef_query_count_total\{api_type=\"OOP\"\}\[5m\]\)\)
```

---

## ✅ Success Criteria

You've found the bottleneck when you can answer:
1. ✅ How many database queries does DoD make vs OOP?
2. ✅ How long does each query take?
3. ✅ How much time is spent in DB vs computation?
4. ✅ Is change tracking enabled? (Should be NO)
5. ✅ Are there N+1 queries?
6. ✅ Is memory pressure causing GC pauses?

---

## 🎓 Next Steps After Identification

Once you identify the bottleneck:

1. **If queries are slow**: Optimize SQL, add indexes, use raw SQL
2. **If too many queries**: Batch queries, use compiled queries, reduce Includes
3. **If change tracking**: Ensure `.AsNoTracking()` everywhere
4. **If memory**: Reduce allocations, optimize data structures
5. **If computation**: Already optimized (unlikely based on benchmark)

---

## 💡 Pro Tips

1. **Start with EF logging** - It's the fastest way to see what's happening
2. **Compare side-by-side** - Run both APIs with same data simultaneously
3. **Use realistic data volumes** - Test with production-like data sizes
4. **Profile during load** - Don't just test single requests
5. **Check database logs too** - MySQL slow query log can reveal issues

---

## 📚 Tools Reference

- **EF Core Logging**: Built-in, no dependencies
- **Prometheus**: Already set up
- **Grafana**: Already set up  
- **dotnet-counters**: Built-in .NET tool
- **PerfView**: Free Microsoft profiler
- **Visual Studio Diagnostic Tools**: Built-in profiler

---

**Recommendation**: Start with **Phase 1.1 (EF Core Logging)** - it will likely reveal the issue immediately with zero code changes!
