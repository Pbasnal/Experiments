# Memory Metrics Explanation: Total Memory vs Working Set

## In Your Application Context

Looking at your `Program.cs`, you're tracking two memory metrics:

### 1. `dotnet_memory_allocated_bytes` (Total Memory Allocated)
```csharp
GC.GetTotalMemory(false)
```
- **What it measures**: Total managed memory allocated by the .NET Garbage Collector
- **Scope**: Only managed memory (objects allocated in .NET heap)
- **Does NOT include**:
  - Unmanaged memory (native allocations, P/Invoke calls)
  - Memory used by the .NET runtime itself
  - Memory used by native libraries
  - Memory-mapped files
- **When it changes**: Increases when you allocate objects, decreases after GC runs
- **Use case**: Understanding your application's managed memory footprint

### 2. `dotnet_memory_total_bytes` (Total Available Memory)
```csharp
gcInfo.TotalAvailableMemoryBytes
```
- **What it measures**: Total memory available to the Garbage Collector
- **Scope**: The memory pool available to the GC (not what's actually used)
- **Note**: This is actually the **available** memory, not the **used** memory
- **Use case**: Understanding GC memory limits

## Working Set (Not Currently Tracked)

**Working Set** is the physical RAM currently being used by your process. It includes:
- ✅ Managed memory (what GC tracks)
- ✅ Unmanaged memory (native allocations)
- ✅ .NET runtime overhead
- ✅ Native libraries (e.g., MySQL connector, EF Core native code)
- ✅ Stack memory
- ✅ Code pages

**How to get it**:
```csharp
Process.GetCurrentProcess().WorkingSet64
```

## Key Differences

| Metric | What It Includes | Typical Size | Use Case |
|--------|------------------|--------------|----------|
| **Total Memory Allocated** (`GC.GetTotalMemory`) | Only managed .NET objects | Smaller | Understanding GC pressure, finding memory leaks in managed code |
| **Working Set** | Everything the process uses in RAM | Larger | Understanding actual system resource usage, Docker container limits |

## Example Scenario

If your application:
- Allocates 100 MB of managed objects
- Uses 50 MB for native libraries (MySQL connector, etc.)
- Has 20 MB of .NET runtime overhead

Then:
- `GC.GetTotalMemory()` ≈ **100 MB**
- Working Set ≈ **170 MB**

## Recommendation

If you want to track **Working Set** (actual physical memory usage), add this metric:

```csharp
var workingSetBytes = Metrics.CreateGauge(
    "dotnet_process_working_set_bytes",
    "Physical memory currently used by the process (working set)",
    new GaugeConfiguration
    {
        LabelNames = new[] { "api_type" }
    });

// In your timer:
workingSetBytes
    .WithLabels("OOP")
    .Set(Process.GetCurrentProcess().WorkingSet64);
```

This will give you the **actual RAM usage** that Docker/your system sees, which is more useful for:
- Setting Docker memory limits
- Understanding system resource consumption
- Capacity planning


