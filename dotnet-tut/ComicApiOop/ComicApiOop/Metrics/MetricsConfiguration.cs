using Prometheus;
using Prometheus.DotNetRuntime;
using System.Runtime;

namespace ComicApiOop.Metrics;

public static class MetricsConfiguration
{
    private static System.Timers.Timer? _timer;
    private static IDisposable? _collector;

    // Previous GC collection counts for delta-based reporting (so rate() in Prometheus is correct)
    private static long _lastGen0, _lastGen1, _lastGen2;

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
                LabelNames = new[] { "api_type", "query_type", "operation" }
            });

        // Database query count metrics
        DbQueryCountTotal = Prometheus.Metrics.CreateCounter(
            "db_query_count_total",
            "Total number of database queries",
            new CounterConfiguration
            {
                LabelNames = new[] { "api_type", "query_type", "operation" }
            });

        // EF Core change tracker metrics
        ChangeTrackerEntities = Prometheus.Metrics.CreateGauge(
            "ef_change_tracker_entities",
            "Number of entities being tracked by EF Core",
            new GaugeConfiguration
            {
                LabelNames = new[] { "api_type", "operation" }
            });

        // Memory allocation tracking per operation
        MemoryAllocatedBytesPerOperation = Prometheus.Metrics.CreateGauge(
            "memory_allocated_bytes_per_operation",
            "Memory allocated per operation",
            new GaugeConfiguration
            {
                LabelNames = new[] { "api_type", "operation" }
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

        // Unified request counter for OOP vs DOD comparison (same metric name in both apps, label api_type)
        ApiHttpRequestsTotal = Prometheus.Metrics.CreateCounter(
            "api_http_requests_total",
            "Total HTTP requests by API type (for GC-per-request comparison)",
            new CounterConfiguration
            {
                LabelNames = new[] { "api_type", "status" }
            });

        // GC pause time ratio (fraction of time in GC pause); same metric in both apps for comparison
        GcPauseTimeRatio = Prometheus.Metrics.CreateGauge(
            "gc_pause_time_ratio",
            "Fraction of time spent in GC pause (0-1), from GCMemoryInfo.PauseTimePercentage",
            new GaugeConfiguration
            {
                LabelNames = new[] { "api_type" }
            });

        // Request Wait Time: time from request arrival (middleware) to start of processing (service)
        RequestWaitTimeSeconds = Prometheus.Metrics.CreateHistogram(
            "comic_visibility_oop_request_wait_seconds",
            "Time from request receipt to start of visibility computation (OOP API)",
            new HistogramConfiguration
            {
                Buckets = Histogram.ExponentialBuckets(0.001, 2, 14),
                LabelNames = Array.Empty<string>()
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

            var gen0 = GC.CollectionCount(0);
            var gen1 = GC.CollectionCount(1);
            var gen2 = GC.CollectionCount(2);
            if (gen0 - _lastGen0 > 0) GcCollectionCount.WithLabels("OOP", "0").Inc(gen0 - _lastGen0);
            if (gen1 - _lastGen1 > 0) GcCollectionCount.WithLabels("OOP", "1").Inc(gen1 - _lastGen1);
            if (gen2 - _lastGen2 > 0) GcCollectionCount.WithLabels("OOP", "2").Inc(gen2 - _lastGen2);
            _lastGen0 = gen0;
            _lastGen1 = gen1;
            _lastGen2 = gen2;

            GcPauseTimeRatio.WithLabels("OOP").Set(gcInfo.PauseTimePercentage / 100.0);
        };
        _timer.Start();
    }

    /// <summary>Record an HTTP request for unified OOP vs DOD comparison (api_http_requests_total).</summary>
    public static void RecordApiRequest(string apiType, string status)
    {
        ApiHttpRequestsTotal.WithLabels(apiType, status).Inc();
    }

    public static Histogram HttpRequestDuration { get; private set; } = null!;
    public static Histogram DbQueryDuration { get; private set; } = null!;
    public static Counter HttpRequestCounter { get; private set; } = null!;
    public static Gauge MemoryAllocatedBytes { get; private set; } = null!;
    public static Gauge MemoryTotalBytes { get; private set; } = null!;
    public static Counter GcCollectionCount { get; private set; } = null!;
    public static Counter ApiHttpRequestsTotal { get; private set; } = null!;
    public static Gauge GcPauseTimeRatio { get; private set; } = null!;

    // Public metrics for database operations
    public static Counter DbQueryCountTotal { get; private set; } = null!;
    public static Gauge ChangeTrackerEntities { get; private set; } = null!;
    public static Gauge MemoryAllocatedBytesPerOperation { get; private set; } = null!;

    /// <summary>Request Wait Time: time from request receipt to start of processing (emitted in service).</summary>
    public static Histogram RequestWaitTimeSeconds { get; private set; } = null!;
}
