using ComicApiOop.Metrics;
using Microsoft.EntityFrameworkCore;
using System.Diagnostics;

namespace ComicApiOop.Services;

/// <summary>
/// Encapsulates metric reporting logic using a functional approach.
/// Methods accept functions/lambdas that are executed and automatically tracked.
/// </summary>
public class MetricsReporter
{
    private const string ApiType = "OOP";
    private readonly DbContext _dbContext;

    public MetricsReporter(DbContext dbContext)
    {
        _dbContext = dbContext;
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
            MetricsConfiguration.DbQueryDuration
                .WithLabels(ApiType, queryType, operationName)
                .Observe(sw.Elapsed.TotalSeconds);
            MetricsConfiguration.DbQueryCountTotal
                .WithLabels(ApiType, queryType, operationName)
                .Inc();
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
            MetricsConfiguration.DbQueryDuration
                .WithLabels(ApiType, queryType, operationName)
                .Observe(sw.Elapsed.TotalSeconds);
        }
    }

    /// <summary>
    /// Tracks change tracker entity count.
    /// </summary>
    /// <param name="operationName">Name of the operation</param>
    public void TrackChangeTracker(string operationName)
    {
        var trackedEntities = _dbContext.ChangeTracker.Entries().Count();
        MetricsConfiguration.ChangeTrackerEntities
            .WithLabels(ApiType, operationName)
            .Set(trackedEntities);
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
        MetricsConfiguration.MemoryAllocatedBytesPerOperation
            .WithLabels(ApiType, operationName)
            .Set(allocated);
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
        // var memoryBefore = GC.GetTotalMemory(false);
        
        try
        {
            var result = await operationFunc();
            
            // Track total operation time
            MetricsConfiguration.DbQueryDuration
                .WithLabels(ApiType, "total_operation", operationName)
                .Observe(swTotal.Elapsed.TotalSeconds);
            
            // Track memory allocation
            // var memoryAfter = GC.GetTotalMemory(false);
            // TrackMemoryAllocation(operationName, memoryBefore, memoryAfter);
            //
            return result;
        }
        catch
        {
            // Still track metrics even on error
            MetricsConfiguration.DbQueryDuration
                .WithLabels(ApiType, "total_operation", operationName)
                .Observe(swTotal.Elapsed.TotalSeconds);
            
            // var memoryAfter = GC.GetTotalMemory(false);
            // TrackMemoryAllocation(operationName, memoryBefore, memoryAfter);
            //
            throw;
        }
    }
}
