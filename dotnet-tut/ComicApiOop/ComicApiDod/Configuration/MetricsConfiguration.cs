using Prometheus;
using System.Threading;

namespace ComicApiDod.Configuration;

public static class MetricsConfiguration
{
    private static readonly Histogram HttpRequestDuration;
    private static readonly Counter HttpRequestCounter;
    private static readonly Gauge MemoryAllocatedBytes;
    private static readonly Gauge MemoryTotalBytes;
    private static readonly Counter GcCollectionCount;
    private static readonly System.Timers.Timer MetricsUpdateTimer;

    // Thread pool metrics (from CLR: ThreadPool.ThreadCount, PendingWorkItemCount, CompletedWorkItemCount)
    private static readonly Gauge ThreadPoolThreadCount;
    private static readonly Gauge ThreadPoolQueueLength;
    private static readonly Counter ThreadPoolCompletedWorkItemsTotal;
    private static long _lastCompletedWorkItemCount;

    // Public metrics for database operations
    public static readonly Counter DbQueryCountTotal;
    public static readonly Histogram DbQueryDuration;
    public static readonly Gauge ChangeTrackerEntities;
    public static readonly Gauge MemoryAllocatedBytesPerOperation;

    static MetricsConfiguration()
    {
        HttpRequestDuration = Metrics.CreateHistogram(
            "http_request_duration_seconds_dod",
            "Duration of HTTP requests in seconds (DOD API)",
            new HistogramConfiguration
            {
                Buckets = Histogram.ExponentialBuckets(0.01, 2, 10),
                LabelNames = new[] { "method", "endpoint", "status" }
            });

        HttpRequestCounter = Metrics.CreateCounter(
            "http_requests_total_dod",
            "Total number of HTTP requests (DOD API)",
            new CounterConfiguration
            {
                LabelNames = new[] { "method", "endpoint", "status" }
            });

        MemoryAllocatedBytes = Metrics.CreateGauge(
            "dotnet_memory_allocated_bytes",
            "Total memory allocated by the application",
            new GaugeConfiguration
            {
                LabelNames = new[] { "api_type" }
            });

        MemoryTotalBytes = Metrics.CreateGauge(
            "dotnet_memory_total_bytes",
            "Total memory used by the application",
            new GaugeConfiguration
            {
                LabelNames = new[] { "api_type" }
            });

        GcCollectionCount = Metrics.CreateCounter(
            "dotnet_gc_collection_count",
            "Number of garbage collections",
            new CounterConfiguration
            {
                LabelNames = new[] { "api_type", "generation" }
            });

        // Database query metrics
        DbQueryCountTotal = Metrics.CreateCounter(
            "db_query_count_total",
            "Total number of database queries",
            new CounterConfiguration
            {
                LabelNames = new[] { "api_type", "query_type", "operation" }
            });

        DbQueryDuration = Metrics.CreateHistogram(
            "db_query_duration_seconds",
            "Duration of database queries in seconds",
            new HistogramConfiguration
            {
                Buckets = Histogram.ExponentialBuckets(0.001, 2, 12),
                LabelNames = new[] { "api_type", "query_type", "operation" }
            });

        // EF Core change tracker metrics
        ChangeTrackerEntities = Metrics.CreateGauge(
            "ef_change_tracker_entities",
            "Number of entities being tracked by EF Core",
            new GaugeConfiguration
            {
                LabelNames = new[] { "api_type", "operation" }
            });

        // Memory allocation tracking
        MemoryAllocatedBytesPerOperation = Metrics.CreateGauge(
            "memory_allocated_bytes_per_operation",
            "Memory allocated per operation",
            new GaugeConfiguration
            {
                LabelNames = new[] { "api_type", "operation" }
            });

        // Thread pool metrics (CLR-native: no custom tracking; we sample ThreadPool.* and export to Prometheus)
        ThreadPoolThreadCount = Metrics.CreateGauge(
            "dotnet_threadpool_thread_count",
            "Number of thread pool threads (ThreadPool.ThreadCount)",
            new GaugeConfiguration { LabelNames = new[] { "api_type" } });
        ThreadPoolQueueLength = Metrics.CreateGauge(
            "dotnet_threadpool_queue_length",
            "Number of work items queued and not yet started (ThreadPool.PendingWorkItemCount)",
            new GaugeConfiguration { LabelNames = new[] { "api_type" } });
        ThreadPoolCompletedWorkItemsTotal = Metrics.CreateCounter(
            "dotnet_threadpool_completed_work_items_total",
            "Total work items completed by the thread pool (delta of ThreadPool.CompletedWorkItemCount)",
            new CounterConfiguration { LabelNames = new[] { "api_type" } });

        MetricsUpdateTimer = new System.Timers.Timer(5000); // Update every 5 seconds
        MetricsUpdateTimer.Elapsed += UpdateMetrics;
    }

    public static void ConfigureMetrics(WebApplication app)
    {
        app.Use(HandleMetricsMiddleware);
        MetricsUpdateTimer.Start();
    }

    private static async Task HandleMetricsMiddleware(HttpContext context, Func<Task> next)
    {
        var path = context.Request.Path.Value ?? "/";
        var method = context.Request.Method;
        var sw = System.Diagnostics.Stopwatch.StartNew();
        string status = "200";

        try
        {
            await next();

            status = context.Response.StatusCode.ToString();
            HttpRequestCounter
                .WithLabels(method, path, status)
                .Inc();

            HttpRequestDuration
                .WithLabels(method, path, status)
                .Observe(sw.Elapsed.TotalSeconds);
        }
        catch
        {
            status = "500";
            HttpRequestCounter
                .WithLabels(method, path, status)
                .Inc();

            HttpRequestDuration
                .WithLabels(method, path, status)
                .Observe(sw.Elapsed.TotalSeconds);
            throw;
        }
    }

    private static void UpdateMetrics(object? sender, System.Timers.ElapsedEventArgs e)
    {
        var gcInfo = GC.GetGCMemoryInfo();
        
        MemoryAllocatedBytes
            .WithLabels("DOD")
            .Set(GC.GetTotalMemory(false));
        
        MemoryTotalBytes
            .WithLabels("DOD")
            .Set(gcInfo.TotalAvailableMemoryBytes);
        
        GcCollectionCount
            .WithLabels("DOD", "0")
            .Inc(GC.CollectionCount(0));
        
        GcCollectionCount
            .WithLabels("DOD", "1")
            .Inc(GC.CollectionCount(1));
        
        GcCollectionCount
            .WithLabels("DOD", "2")
            .Inc(GC.CollectionCount(2));

        // Thread pool (CLR-native APIs only)
        ThreadPoolThreadCount.WithLabels("DOD").Set(ThreadPool.ThreadCount);
        ThreadPoolQueueLength.WithLabels("DOD").Set(ThreadPool.PendingWorkItemCount);
        var completed = ThreadPool.CompletedWorkItemCount;
        var delta = completed - _lastCompletedWorkItemCount;
        if (delta > 0)
            ThreadPoolCompletedWorkItemsTotal.WithLabels("DOD").Inc(delta);
        _lastCompletedWorkItemCount = completed;
    }
}
