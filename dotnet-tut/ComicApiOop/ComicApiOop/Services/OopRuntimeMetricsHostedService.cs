using Common.Metrics;

namespace ComicApiOop.Services;

public sealed class OopRuntimeMetricsHostedService : BackgroundService
{
    private readonly IAppMetrics _metrics;
    private long _lastGen0;
    private long _lastGen1;
    private long _lastGen2;

    public OopRuntimeMetricsHostedService(IAppMetrics metrics)
    {
        _metrics = metrics;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        // Initialize so the first loop reports only deltas.
        _lastGen0 = GC.CollectionCount(0);
        _lastGen1 = GC.CollectionCount(1);
        _lastGen2 = GC.CollectionCount(2);

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                var gcInfo = GC.GetGCMemoryInfo();

                _metrics.Set(MetricNames.DotNetMemoryAllocatedBytes, GC.GetTotalMemory(false));
                _metrics.Set("dotnet_memory_total_bytes", gcInfo.TotalAvailableMemoryBytes);

                var gen0 = GC.CollectionCount(0);
                var gen1 = GC.CollectionCount(1);
                var gen2 = GC.CollectionCount(2);

                var d0 = gen0 - _lastGen0;
                var d1 = gen1 - _lastGen1;
                var d2 = gen2 - _lastGen2;

                if (d0 > 0) _metrics.Inc(MetricNames.DotNetGcCollectionCount, d0, new Dictionary<string, string> { ["generation"] = "0" });
                if (d1 > 0) _metrics.Inc(MetricNames.DotNetGcCollectionCount, d1, new Dictionary<string, string> { ["generation"] = "1" });
                if (d2 > 0) _metrics.Inc(MetricNames.DotNetGcCollectionCount, d2, new Dictionary<string, string> { ["generation"] = "2" });

                _lastGen0 = gen0;
                _lastGen1 = gen1;
                _lastGen2 = gen2;

                _metrics.Set(MetricNames.GcPauseTimeRatio, gcInfo.PauseTimePercentage / 100.0);
            }
            catch
            {
                // Never crash the host due to sampling/metrics.
            }

            await Task.Delay(TimeSpan.FromSeconds(5), stoppingToken);
        }
    }
}

