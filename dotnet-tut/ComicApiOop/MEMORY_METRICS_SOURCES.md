# Memory Metrics Sources in Your Application

## Metrics You're Seeing

### 1. **Custom Metrics** (from your `Program.cs`)

#### `dotnet_memory_allocated_bytes`
- **Source**: Your custom code (line 109)
- **Value**: `GC.GetTotalMemory(false)`
- **What it is**: Total managed memory allocated by .NET GC
- **Scope**: Only managed .NET objects

#### `dotnet_memory_total_bytes`
- **Source**: Your custom code (line 113)
- **Value**: `gcInfo.TotalAvailableMemoryBytes`
- **What it is**: Total memory available to the GC (not what's used)
- **Scope**: GC memory pool size

### 2. **Automatic Metrics** (from `Prometheus.DotNetRuntime`)

The `DotNetRuntimeStatsBuilder.Default().StartCollecting()` (line 47) automatically exposes:

#### `dotnet_gc_memory_info`
- **Source**: Prometheus.DotNetRuntime library
- **What it includes**: Various GC memory statistics
- **Labels**: `generation`, `type`, etc.

#### Working Set Metrics
- **Source**: Prometheus.DotNetRuntime library
- **Metrics**: The library may expose process memory metrics including working set
- **Typical names**: 
  - `dotnet_process_working_set_bytes` or similar
  - Or via `dotnet_gc_memory_info` with specific labels

## How to Identify Which Metric is Which

1. **Check Prometheus targets**: `http://localhost:9090/targets`
2. **Query all metrics**: `http://localhost:9090/api/v1/label/__name__/values`
3. **Search for memory metrics**: Look for metrics starting with `dotnet_`

## Common Prometheus.DotNetRuntime Metrics

The library typically exposes:
- `dotnet_gc_collections_total` - GC collection counts
- `dotnet_gc_pause_seconds` - GC pause durations
- `dotnet_gc_memory_info` - Memory information
- `dotnet_gc_heap_size_bytes` - Heap size
- `dotnet_gc_allocated_bytes_total` - Total allocated bytes
- Process-level metrics (may include working set)

## Understanding the Difference

| Metric | Source | What It Measures |
|--------|--------|------------------|
| `dotnet_memory_allocated_bytes` | Your code | Managed memory only (GC.GetTotalMemory) |
| `dotnet_memory_total_bytes` | Your code | GC available memory pool |
| Working Set | Prometheus.DotNetRuntime | Physical RAM used by process (if exposed) |

## Recommendation

If you want to explicitly track working set with a clear name, you can still add it as a custom metric:

```csharp
var workingSetBytes = Metrics.CreateGauge(
    "dotnet_process_working_set_bytes",
    "Physical memory currently used by the process (working set)",
    new GaugeConfiguration { LabelNames = new[] { "api_type" } });

// In your timer:
workingSetBytes
    .WithLabels("OOP")
    .Set(Process.GetCurrentProcess().WorkingSet64);
```

This gives you explicit control and a clear metric name, even if Prometheus.DotNetRuntime also exposes it.


