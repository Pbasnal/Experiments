namespace Common.Metrics;

/// <summary>
/// Shared metric name constants for OOP and DOD so both apps use the same logical names.
/// </summary>
public static class MetricNames
{
    /// <summary>HTTP request duration in seconds (histogram).</summary>
    public const string HttpRequestDuration = "http_request_duration_seconds";

    /// <summary>Total API HTTP requests (counter).</summary>
    public const string ApiHttpRequestsTotal = "api_http_requests_total";

    /// <summary>Database/query duration in seconds (histogram).</summary>
    public const string DbQueryDuration = "db_query_duration_seconds";

    /// <summary>.NET memory allocated bytes (gauge).</summary>
    public const string DotNetMemoryAllocatedBytes = "dotnet_memory_allocated_bytes";

    /// <summary>.NET GC collection count (counter).</summary>
    public const string DotNetGcCollectionCount = "dotnet_gc_collection_count";

    /// <summary>GC pause time ratio (gauge).</summary>
    public const string GcPauseTimeRatio = "gc_pause_time_ratio";

    /// <summary>Request wait time in seconds (histogram).</summary>
    public const string RequestWaitTimeSeconds = "request_wait_time_seconds";

    /// <summary>Requests in current batch (gauge).</summary>
    public const string RequestsInBatch = "requests_in_batch";

    /// <summary>DOD fetch phase duration (histogram).</summary>
    public const string DodFetchPhaseDuration = "dod_fetch_phase_duration_seconds";
}
