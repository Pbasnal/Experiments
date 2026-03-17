using Common.Metrics;
using Microsoft.EntityFrameworkCore;
using System.Diagnostics;

namespace ComicApiOop.Services;

/// <summary>
/// Encapsulates metric reporting logic using a functional approach.
/// Methods accept functions/lambdas that are executed and automatically tracked.
/// Uses Common.Metrics.IAppMetrics for latency, count, and gauges.
/// </summary>
public class MetricsReporter
{
    private readonly DbContext _dbContext;
    private readonly IAppMetrics _appMetrics;

    public MetricsReporter(DbContext dbContext, IAppMetrics appMetrics)
    {
        _dbContext = dbContext;
        _appMetrics = appMetrics;
    }

    /// <summary>
    /// Tracks a database query operation with duration and count metrics.
    /// </summary>
    /// <typeparam name="T">The return type of the query</typeparam>
    /// <param name="queryType">Type of query (e.g., "fetch_comic", "save_visibilities")</param>
    /// <param name="operationName">Name of the operation (e.g., "ComputeVisibilityForComicAsync")</param>
    /// <param name="queryFunc">The async query function to execute</param>
    /// <returns>The result of the query function</returns>
    public async Task<T> TrackQueryAsync<T>(
        string queryType,
        string operationName,
        Func<Task<T>> queryFunc)
    {
        var sw = Stopwatch.StartNew();
        try
        {
            var result = await queryFunc();
            return result;
        }
        finally
        {
            sw.Stop();
            var process = $"oop_db_query_{queryType}";
            var attrs = new Dictionary<string, string> { ["status"] = "ok" };
            _appMetrics.RecordLatency(process, sw.Elapsed.TotalSeconds, attrs);
            _appMetrics.CaptureCount(process, 1, attrs);
        }
    }

    /// <summary>
    /// Tracks a synchronous operation with duration metrics.
    /// </summary>
    /// <typeparam name="T">The return type of the operation</typeparam>
    /// <param name="queryType">Type of operation (e.g., "computation")</param>
    /// <param name="operationName">Name of the operation</param>
    /// <param name="operationFunc">The operation function to execute</param>
    /// <returns>The result of the operation function</returns>
    public T TrackOperation<T>(
        string queryType,
        string operationName,
        Func<T> operationFunc)
    {
        var sw = Stopwatch.StartNew();
        try
        {
            return operationFunc();
        }
        finally
        {
            sw.Stop();
            var process = $"oop_operation_{queryType}";
            var attrs = new Dictionary<string, string> { ["status"] = "ok" };
            _appMetrics.RecordLatency(process, sw.Elapsed.TotalSeconds, attrs);
            _appMetrics.CaptureCount(process, 1, attrs);
        }
    }

    /// <summary>
    /// Tracks change tracker entity count.
    /// </summary>
    /// <param name="operationName">Name of the operation</param>
    public void TrackChangeTracker(string operationName)
    {
        var trackedEntities = _dbContext.ChangeTracker.Entries().Count();
        _appMetrics.Set(
            "ef_change_tracker_entities",
            trackedEntities,
            new Dictionary<string, string> { ["operation"] = operationName });
    }

    /// <summary>
    /// Tracks memory allocation for an operation.
    /// </summary>
    /// <param name="operationName">Name of the operation</param>
    /// <param name="memoryBefore">Memory before the operation</param>
    /// <param name="memoryAfter">Memory after the operation</param>
    public void TrackMemoryAllocation(string operationName, long memoryBefore, long memoryAfter)
    {
        var allocated = memoryAfter - memoryBefore;
        _appMetrics.Set(
            "memory_allocated_bytes_per_operation",
            allocated,
            new Dictionary<string, string> { ["operation"] = operationName });
    }

    /// <summary>
    /// Tracks a complete operation with total duration and memory allocation.
    /// </summary>
    /// <typeparam name="T">The return type of the operation</typeparam>
    /// <param name="operationName">Name of the operation</param>
    /// <param name="operationFunc">The async operation function to execute</param>
    /// <returns>The result of the operation function</returns>
    public async Task<T> TrackCompleteOperationAsync<T>(
        string operationName,
        Func<Task<T>> operationFunc)
    {
        var swTotal = Stopwatch.StartNew();
        var memoryBefore = GC.GetTotalMemory(false);
        
        try
        {
            var result = await operationFunc();
            var attrs = new Dictionary<string, string> { ["status"] = "ok" };
            _appMetrics.RecordLatency("oop_operation_total", swTotal.Elapsed.TotalSeconds, attrs);
            _appMetrics.CaptureCount("oop_operation_total", 1, attrs);

            var memoryAfter = GC.GetTotalMemory(false);
            TrackMemoryAllocation(operationName, memoryBefore, memoryAfter);
            return result;
        }
        catch
        {
            var attrs = new Dictionary<string, string> { ["status"] = "error" };
            _appMetrics.RecordLatency("oop_operation_total", swTotal.Elapsed.TotalSeconds, attrs);
            _appMetrics.CaptureCount("oop_operation_total", 1, attrs);

            var memoryAfter = GC.GetTotalMemory(false);
            TrackMemoryAllocation(operationName, memoryBefore, memoryAfter);
            throw;
        }
    }
}
