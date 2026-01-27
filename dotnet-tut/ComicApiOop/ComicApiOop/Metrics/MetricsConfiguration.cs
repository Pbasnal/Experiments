using Prometheus;
using Prometheus.DotNetRuntime;
using System.Runtime;

namespace ComicApiOop.Metrics;

public static class MetricsConfiguration
{
    private static System.Timers.Timer? _timer;
    private static IDisposable? _collector;

    public static void ConfigureMetrics()
    {
        // Configure metrics pusher
        var metrics = new MetricPusher(new MetricPusherOptions
        {
            Endpoint = "http://prometheus:9090/api/v1/write",
            Job = "comic_api"
        });
        metrics.Start();

        // Enable collection of .NET runtime metrics
        _collector = DotNetRuntimeStatsBuilder.Default().StartCollecting();

        // Create custom metrics
        HttpRequestDuration = Prometheus.Metrics.CreateHistogram(
            "http_request_duration_seconds_oop",
            "Duration of HTTP requests in seconds (OOP API)",
            new HistogramConfiguration
            {
                Buckets = Histogram.ExponentialBuckets(0.01, 2, 10),
                LabelNames = new[] { "method", "endpoint", "status" }
            });

        DbQueryDuration = Prometheus.Metrics.CreateHistogram(
            "db_query_duration_seconds",
            "Duration of database queries in seconds",
            new HistogramConfiguration
            {
                Buckets = Histogram.ExponentialBuckets(0.001, 2, 10),
                LabelNames = new[] { "query_type" }
            });

        HttpRequestCounter = Prometheus.Metrics.CreateCounter(
            "http_requests_total_oop",
            "Total number of HTTP requests (OOP API)",
            new CounterConfiguration
            {
                LabelNames = new[] { "method", "endpoint", "status" }
            });

        // Add memory-related metrics
        MemoryAllocatedBytes = Prometheus.Metrics.CreateGauge(
            "dotnet_memory_allocated_bytes",
            "Total memory allocated by the application",
            new GaugeConfiguration
            {
                LabelNames = new[] { "api_type" }
            });

        MemoryTotalBytes = Prometheus.Metrics.CreateGauge(
            "dotnet_memory_total_bytes",
            "Total memory used by the application",
            new GaugeConfiguration
            {
                LabelNames = new[] { "api_type" }
            });

        GcCollectionCount = Prometheus.Metrics.CreateCounter(
            "dotnet_gc_collection_count",
            "Number of garbage collections",
            new CounterConfiguration
            {
                LabelNames = new[] { "api_type", "generation" }
            });

        // Start periodic metric updates
        StartPeriodicMetricUpdates();
    }

    private static void StartPeriodicMetricUpdates()
    {
        _timer = new System.Timers.Timer(5000); // Update every 5 seconds
        _timer.Elapsed += (sender, e) =>
        {
            var gcInfo = GC.GetGCMemoryInfo();

            MemoryAllocatedBytes
                .WithLabels("OOP")
                .Set(GC.GetTotalMemory(false));

            MemoryTotalBytes
                .WithLabels("OOP")
                .Set(gcInfo.TotalAvailableMemoryBytes);

            GcCollectionCount
                .WithLabels("OOP", "0")
                .Inc(GC.CollectionCount(0));

            GcCollectionCount
                .WithLabels("OOP", "1")
                .Inc(GC.CollectionCount(1));

            GcCollectionCount
                .WithLabels("OOP", "2")
                .Inc(GC.CollectionCount(2));
        };
        _timer.Start();
    }

    public static Histogram HttpRequestDuration { get; private set; } = null!;
    public static Histogram DbQueryDuration { get; private set; } = null!;
    public static Counter HttpRequestCounter { get; private set; } = null!;
    public static Gauge MemoryAllocatedBytes { get; private set; } = null!;
    public static Gauge MemoryTotalBytes { get; private set; } = null!;
    public static Counter GcCollectionCount { get; private set; } = null!;
}
