# Quick Diagnostic Guide - Find the Bottleneck in 15 Minutes

## 🎯 Fastest Way to Identify the Issue

### Step 1: Enable EF Core Query Logging (2 minutes)

**For DoD API** (`ComicApiDod/ProgramDod.cs`):
```csharp
builder.Services.AddDbContextFactory<ComicDbContext>(options =>
{
    options.UseMySql(connectionString, serverVersion)
        .EnableSensitiveDataLogging()  // Shows SQL parameters
        .LogTo(Console.WriteLine, LogLevel.Information);  // Logs all queries
});
```

**For OOP API** (`ComicApiOop/Program.cs` or `Extensions/ServiceCollectionExtensions.cs`):
```csharp
services.AddDbContext<ComicDbContext>(options =>
{
    options.UseMySql(connectionString, serverVersion,
        mysqlOptions => { /* ... */ })
        .EnableSensitiveDataLogging()
        .LogTo(Console.WriteLine, LogLevel.Information);
});
```

### Step 2: Run Single Request (1 minute)

```bash
# Start DoD API
cd ComicApiDod
dotnet run

# In another terminal, make request
curl "http://localhost:5000/api/comics/compute-visibilities?startId=1&limit=20"

# Check console output for query logs
```

### Step 3: Compare Outputs (5 minutes)

**Look for**:
1. **Number of queries**
   - Count `Executing DbCommand` lines
   - DoD should have 2-3 queries
   - OOP should have 2-3 queries per comic (or batched)

2. **Query execution time**
   - Look for `[Parameters=[...], CommandType='Text', CommandTimeout='30']`
   - Time shown in logs

3. **Query complexity**
   - Check JOIN counts
   - Check if queries are batched or individual

### Step 4: Check Change Tracking (2 minutes)

**Add temporary logging**:
```csharp
// In DatabaseQueryHelper.GetComicBatchDataAsync, after query:
_logger.LogInformation($"Change tracker entries: {db.ChangeTracker.Entries().Count()}");
```

**Expected**: Should be 0 if `.AsNoTracking()` is working

### Step 5: Analyze Results (5 minutes)

**Compare**:
- Query count: DoD vs OOP
- Query time: DoD vs OOP  
- Change tracker: Should be 0 for both

**Most Likely Findings**:
1. **DoD has more queries** → Need to batch better
2. **DoD queries are slower** → Missing indexes or inefficient SQL
3. **Change tracker has entries** → `.AsNoTracking()` not working
4. **DoD queries are same but overall slower** → Entity materialization overhead

---

## 🔍 What Each Finding Means

### Finding: DoD Makes More Queries
**Problem**: Not batching properly
**Solution**: Combine queries, use better Include() strategy

### Finding: DoD Queries Are Slower
**Problem**: Database/indexes or query complexity
**Solution**: 
- Check MySQL slow query log
- Add indexes
- Simplify queries
- Use raw SQL for complex queries

### Finding: Change Tracker Has Entries
**Problem**: `.AsNoTracking()` not applied
**Solution**: Ensure it's before `.ToListAsync()`

### Finding: Same Queries, Different Performance
**Problem**: Entity materialization or relationship fixup
**Solution**: 
- Use projection instead of full entities
- Reduce navigation properties
- Use Dapper for read-heavy operations

---

## 📊 Quick Metrics Check

If you want to see existing metrics:

```bash
# Check current query duration metrics
curl "http://localhost:9090/api/v1/query?query=comic_visibility_db_query_duration_seconds"

# Check operation breakdown
curl "http://localhost:9090/api/v1/query?query=comic_visibility_operation_duration_seconds"

# Compare DoD vs OOP (if both running)
curl "http://localhost:9090/api/v1/query?query=rate(comic_visibility_db_query_duration_seconds_count[5m])"
```

---

## ✅ Expected Output Example

**Good DoD Output** (what you want to see):
```
Executing DbCommand [Parameters=[@__p_0='?'], CommandType='Text', CommandTimeout='30']
SELECT c.Id, c.Title, ... FROM Comics c WHERE c.Id IN (1,2,3,...)  -- Single query for all comics

Executing DbCommand [Parameters=[...], CommandType='Text', CommandTimeout='30']  
SELECT c0.Id, c0.ComicId, ... FROM Chapters c0 WHERE c0.ComicId IN (1,2,3,...)  -- Batched chapters

Change tracker entries: 0  -- ✅ Good!
```

**Bad DoD Output** (problem indicators):
```
Executing DbCommand ... SELECT * FROM Comics WHERE Id = 1  -- Individual queries
Executing DbCommand ... SELECT * FROM Comics WHERE Id = 2
Executing DbCommand ... SELECT * FROM Comics WHERE Id = 3
... (20 queries for 20 comics)  -- ❌ N+1 problem!

Change tracker entries: 5000  -- ❌ Should be 0!
```

---

## 🎯 Next Steps Based on Findings

| Finding | Action |
|---------|--------|
| More queries in DoD | Implement query batching |
| Slower queries in DoD | Check indexes, optimize SQL |
| Change tracker > 0 | Fix `.AsNoTracking()` placement |
| Same queries, slower | Profile entity materialization |
| Memory issues | Check allocations, GC pressure |

---

**This 15-minute diagnostic will likely reveal 80% of performance issues!**
