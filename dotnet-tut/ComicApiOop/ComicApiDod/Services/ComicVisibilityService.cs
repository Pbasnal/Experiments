using ComicApiDod.Data;
using ComicApiDod.Models;
using ComicApiDod.SimpleQueue;
using Microsoft.EntityFrameworkCore;
using Prometheus;
using System.Collections.Generic;
using System.Diagnostics;
using ComicApiDod.utils;

namespace ComicApiDod.Services;

public class ComicVisibilityService
{
    private readonly IDbContextFactory<ComicDbContext> _dbFactory;
    private readonly ILogger<ComicVisibilityService> _logger;
    private readonly VisibilityMetricsService _metrics;

    // Prometheus metrics
    private static readonly Histogram ComputeVisibilityDuration = Metrics.CreateHistogram(
        "comic_visibility_computation_duration_seconds",
        "Duration of comic visibility computation",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.01, 2, 10),
            LabelNames = new[] { "type", "status" }
        });

    private static readonly Counter ComputeVisibilityCounter = Metrics.CreateCounter(
        "comic_visibility_computation_total",
        "Total number of visibility computations",
        new CounterConfiguration
        {
            LabelNames = new[] { "type", "status" }
        });

    private static readonly Gauge RequestsInBatch = Metrics.CreateGauge(
        "comic_visibility_request_count_in_batch",
        "Number of requests in the current request batch");

    public ComicVisibilityService(
        IDbContextFactory<ComicDbContext> dbFactory,
        ILogger<ComicVisibilityService> logger,
        VisibilityMetricsService metrics)
    {
        _dbFactory = dbFactory;
        _logger = logger;
        _metrics = metrics;
    }

    public async Task ComputeVisibilities(int numOfRequest, List<VisibilityComputationRequest?> reqs)
    {
        var sw = Stopwatch.StartNew();
        string computationStatus = "success";
        try
        {
            if (reqs.Count > 1)
            {
                reqs.Sort((v1, v2) =>
                {
                    if (v1 == null && v2 == null) return 0;
                    if (v1 == null) return 1;
                    if (v2 == null) return -1;
                    return v1.StartId.CompareTo(v2.StartId);
                });

                Utils.PrintCollection(reqs);
            }

            RequestsInBatch.Set(reqs.Count);
            _metrics.RecordBatchProcessing(reqs.Count, TimeSpan.Zero, "started");

            List<Task> responses = new List<Task>();

            foreach (var req in reqs)
            {
                if (req != null)
                {
                    // responses.Add(ComputeVisibilitiesAsync(req));
                    responses.Add(ComputeVisibilitiesAsync(req));
                }
            }

            await Task.WhenAll(responses);
            computationStatus = "success";
        }
        catch (Exception ex)
        {
            computationStatus = "batch_failure";
            _metrics.RecordError(ex.GetType().Name, "batch_processing");
            throw;
        }
        finally
        {
            var duration = sw.Elapsed;
            ComputeVisibilityDuration
                .WithLabels("batch_process", computationStatus)
                .Observe(duration.TotalSeconds);
            _metrics.RecordBatchProcessing(reqs.Count, duration, computationStatus);
        }
    }

    // public async Task<IValue> ComputeVisibilitiesAsync(VisibilityComputationRequest req)
    // {
    //     // Create a new DbContext instance for this concurrent operation
    //     // This prevents threading issues when multiple requests are processed concurrently
    //     await using var db = _dbFactory.CreateDbContext();
    //
    //     Stopwatch swForReqE2e = Stopwatch.StartNew();
    //     var sw = Stopwatch.StartNew();
    //     string computationStatus = "success";
    //     long startId = req.StartId;
    //     int limit = req.Limit;
    //     var startTime = DateTime.UtcNow;
    //
    //     try
    //     {
    //         // Note: Validation is now done in the handler, but we keep this as a safety check
    //         if (startId < 1 || limit < 1 || limit > 20)
    //         {
    //             computationStatus = "validation_failed";
    //             var errorResponse = new VisibilityComputationResponse
    //             {
    //                 Id = req.Id,
    //                 StartId = startId,
    //                 Limit = limit,
    //                 ProcessedSuccessfully = 0,
    //                 Failed = 0,
    //                 DurationInSeconds = 0,
    //                 NextStartId = startId,
    //                 Results = Array.Empty<ComicVisibilityResult>()
    //             };
    //             req.ResponseSrc.TrySetResult(errorResponse);
    //             return errorResponse;
    //         }
    //
    //         _logger.LogInformation("Computing visibility for comics starting from ID {StartId}, limit {Limit}", startId,
    //             limit);
    //
    //         // Step 1: Get comic IDs (batch query)
    //         sw.Restart();
    //         var comicIds = await DatabaseQueryHelper.GetComicIdsAsync(db, (int)startId, limit);
    //         ComputeVisibilityDuration
    //             .WithLabels("fetch_comics", computationStatus)
    //             .Observe(sw.Elapsed.TotalSeconds);
    //         DateTime endTime;
    //         TimeSpan duration;
    //         if (comicIds.Length == 0)
    //         {
    //             computationStatus = "no_comics_found";
    //             endTime = DateTime.UtcNow;
    //             duration = endTime - startTime;
    //
    //             var noComicsResponse = new VisibilityComputationResponse
    //             {
    //                 Id = req.Id,
    //                 StartId = startId,
    //                 Limit = limit,
    //                 ProcessedSuccessfully = 0,
    //                 Failed = 0,
    //                 DurationInSeconds = duration.TotalSeconds,
    //                 NextStartId = startId,
    //                 Results = Array.Empty<ComicVisibilityResult>()
    //             };
    //
    //             req.ResponseSrc.TrySetResult(noComicsResponse);
    //             return noComicsResponse;
    //         }
    //
    //         _logger.LogInformation("Found {Count} comics to process", comicIds.Length);
    //
    //         // Step 2: Fetch all data for batch (DOD: fetch all data first)
    //         sw.Restart();
    //         IDictionary<long, ComicBatchData> allComicsBatchData =
    //             await DatabaseQueryHelper.GetComicBatchDataAsync(db, comicIds);
    //         var dataFetchDuration = sw.Elapsed;
    //         _metrics.RecordDataFetch("fetch_all_comics_data", dataFetchDuration);
    //         ComputeVisibilityDuration
    //             .WithLabels("fetch_all_comics_data", computationStatus)
    //             .Observe(dataFetchDuration.TotalSeconds);
    //
    //         // Step 3: Process all comics together using bulk processor (True DOD approach)
    //         sw.Restart();
    //         var computationTime = DateTime.UtcNow;
    //         var bulkResult = BulkVisibilityProcessor.ProcessBatch(allComicsBatchData, computationTime);
    //         var computationDuration = sw.Elapsed;
    //
    //         _metrics.RecordComputation(
    //             bulkResult.ComicIds.Length,
    //             bulkResult.ProcessingStats.TotalVisibilities,
    //             computationDuration);
    //
    //         ComputeVisibilityDuration
    //             .WithLabels("compute_visibility", computationStatus)
    //             .Observe(computationDuration.TotalSeconds);
    //
    //         _logger.LogInformation(
    //             "Bulk processed {Count} comics: {Success} success, {Failed} failed, {TotalVisibilities} total visibilities in {Duration}ms",
    //             bulkResult.ComicIds.Length,
    //             bulkResult.ProcessingStats.SuccessCount,
    //             bulkResult.ProcessingStats.FailedCount,
    //             bulkResult.ProcessingStats.TotalVisibilities,
    //             computationDuration.TotalMilliseconds);
    //
    //         // Step 4: Save all visibilities in one bulk operation (more efficient than per-comic saves)
    //         var allVisibilities = BulkVisibilityProcessor.FlattenVisibilities(bulkResult);
    //         int processedCount = bulkResult.ProcessingStats.SuccessCount;
    //         int failedCount = bulkResult.ProcessingStats.FailedCount;
    //
    //         if (allVisibilities.Length > 0)
    //         {
    //             sw.Restart();
    //             await DatabaseQueryHelper.SaveComputedVisibilitiesBulkAsync(db, allVisibilities);
    //             var saveDuration = sw.Elapsed;
    //
    //             _metrics.RecordSave("bulk", allVisibilities.Length, saveDuration);
    //             ComputeVisibilityDuration
    //                 .WithLabels("save_visibility", computationStatus)
    //                 .Observe(saveDuration.TotalSeconds);
    //         }
    //
    //         // Step 5: Build results from bulk processing
    //         var results = new List<ComicVisibilityResult>();
    //         foreach (var comicId in comicIds)
    //         {
    //             var visibilities =
    //                 bulkResult.VisibilitiesByComic.GetValueOrDefault(comicId, Array.Empty<ComputedVisibilityData>());
    //             var error = bulkResult.ProcessingStats.Errors.FirstOrDefault(e => e.ComicId == comicId);
    //
    //             _metrics.RecordComputedVisibilities(comicId, visibilities.Length);
    //
    //             if (visibilities.Length == 0 && error == null)
    //             {
    //                 _logger.LogWarning(
    //                     "Comic {ComicId} has no computed visibilities. This may indicate missing or invalid rules.",
    //                     comicId);
    //             }
    //
    //             results.Add(new ComicVisibilityResult
    //             {
    //                 ComicId = comicId,
    //                 Success = visibilities.Length > 0 && error == null,
    //                 ErrorMessage = error?.ErrorMessage ?? (visibilities.Length == 0
    //                     ? "No visibilities computed - missing or invalid geographic/segment rules"
    //                     : null),
    //                 ComputationTime = computationTime,
    //                 ComputedVisibilities = visibilities
    //             });
    //         }
    //
    //         endTime = DateTime.UtcNow;
    //         duration = endTime - startTime;
    //
    //         _logger.LogInformation(
    //             "Completed processing. Success: {Success}, Failed: {Failed}, Duration: {Duration}s, Throughput: {Throughput} comics/sec",
    //             processedCount, failedCount, duration.TotalSeconds,
    //             duration.TotalSeconds > 0 ? processedCount / duration.TotalSeconds : 0);
    //
    //         // Record batch-level metrics
    //         _metrics.RecordBatchProcessing(comicIds.Length, duration, computationStatus);
    //         _metrics.RecordThroughput(processedCount, bulkResult.ProcessingStats.TotalVisibilities, duration);
    //
    //         // Update Prometheus metrics
    //         ComputeVisibilityDuration
    //             .WithLabels("visibility_computation", computationStatus)
    //             .Observe(duration.TotalSeconds);
    //
    //         ComputeVisibilityCounter
    //             .WithLabels("visibility_computation", computationStatus)
    //             .Inc();
    //
    //         VisibilityComputationResponse response = new()
    //         {
    //             Id = req.Id,
    //             StartId = startId,
    //             Limit = limit,
    //             ProcessedSuccessfully = processedCount,
    //             Failed = failedCount,
    //             DurationInSeconds = duration.TotalSeconds,
    //             NextStartId = startId + limit,
    //             Results = results.ToArray()
    //         };
    //
    //         req.ResponseSrc.TrySetResult(response);
    //
    //         return response;
    //     }
    //     catch (Exception ex)
    //     {
    //         computationStatus = "total_failure";
    //         _logger.LogError(ex, "Error during bulk visibility computation");
    //
    //         _metrics.RecordError(ex.GetType().Name, "computation");
    //
    //         // Update Prometheus metrics for total failure
    //         ComputeVisibilityCounter
    //             .WithLabels("visibility_computation_count", computationStatus)
    //             .Inc();
    //
    //         // Always set a response, even on failure
    //         var errorResponse = new VisibilityComputationResponse
    //         {
    //             Id = req.Id,
    //             StartId = startId,
    //             Limit = limit,
    //             ProcessedSuccessfully = 0,
    //             Failed = 1,
    //             DurationInSeconds = swForReqE2e.Elapsed.TotalSeconds,
    //             NextStartId = startId,
    //             Results = new[]
    //             {
    //                 new ComicVisibilityResult
    //                 {
    //                     ComicId = startId,
    //                     Success = false,
    //                     ErrorMessage = ex.Message,
    //                     ComputationTime = DateTime.UtcNow,
    //                     ComputedVisibilities = Array.Empty<ComputedVisibilityData>()
    //                 }
    //             }
    //         };
    //
    //         req.ResponseSrc.TrySetResult(errorResponse);
    //         return errorResponse;
    //     }
    //     finally
    //     {
    //         swForReqE2e.Stop();
    //         ComputeVisibilityDuration
    //             .WithLabels("visibility_computation", computationStatus)
    //             .Observe(swForReqE2e.Elapsed.TotalSeconds);
    //     }
    // }

    private async Task ComputeVisibilitiesAsync(VisibilityComputationRequest req)
    {
        long startId = req.StartId;
        int limit = req.Limit;

        if (!ValidateRequest(startId, limit, out IResult? validationResult))
        {
            _logger.LogError("Computing visibility for comics starting from ID {StartId}, limit {Limit}", startId,
                limit);
            req.ResponseSrc.TrySetResult(null);
            return;
        }

        // Create a new DbContext instance for this operation
        await using var db = _dbFactory.CreateDbContext();

        var sw = Stopwatch.StartNew();
        string computationStatus = "success";

        try
        {
            _logger.LogInformation("Computing visibility for comics starting from ID {StartId}, limit {Limit}", startId,
                limit);

            DateTime startTime = DateTime.UtcNow;

            // Step 1: Get comic IDs (batch query)
            long[] comicIds = await DatabaseQueryHelper.GetComicIdsAsync(db, (int)startId, limit);

            if (comicIds.Length == 0)
            {
                computationStatus = "no_comics_found";
                _logger.LogError("No comics found between {StartId}, limit {Limit}", startId,
                    limit);
                req.ResponseSrc.TrySetResult(null);

                return;
            }

            _logger.LogInformation("Found {Count} comics to process", comicIds.Length);

            // Step 2: Process each comic (in DOD, we process data in batches)
            List<ComicVisibilityResult> results = new();
            int processedCount = 0;
            int failedCount = 0;

            foreach (var comicId in comicIds)
            {
                try
                {
                    _logger.LogInformation("Processing comic ID: {ComicId}", comicId);

                    // Fetch all data for this comic
                    var batchData = await DatabaseQueryHelper.GetComicBatchDataAsync(db, comicId);

                    // Compute visibilities using pure functions
                    var computedVisibilities = VisibilityProcessor.ComputeVisibilities(
                        batchData,
                        DateTime.UtcNow);

                    // Save to database
                    if (computedVisibilities.Length > 0)
                    {
                        await DatabaseQueryHelper.SaveComputedVisibilitiesAsync(db, computedVisibilities);
                    }

                    results.Add(new ComicVisibilityResult
                    {
                        ComicId = comicId,
                        Success = computedVisibilities.Length > 0, // Mark as failed if no visibilities computed
                        ErrorMessage = computedVisibilities.Length == 0
                            ? "No visibilities computed - missing or invalid geographic/segment rules"
                            : null,
                        ComputationTime = DateTime.UtcNow,
                        ComputedVisibilities = computedVisibilities
                    });

                    if (computedVisibilities.Length > 0)
                    {
                        processedCount++;
                    }
                    else
                    {
                        failedCount++;
                        computationStatus = "partial_failure";
                        _logger.LogWarning(
                            "Comic {ComicId} has no computed visibilities. This may indicate missing or invalid rules. " +
                            "GeographicRules={GeoCount}, SegmentRules={SegmentCount}, Segments={SegmentsCount}",
                            comicId,
                            batchData.GeographicRules.Length,
                            batchData.SegmentRules.Length,
                            batchData.Segments.Length);
                    }
                }
                catch (Exception ex)
                {
                    failedCount++;
                    computationStatus = "partial_failure";
                    _logger.LogError(ex, "Error processing comic {ComicId}", comicId);
                    results.Add(new ComicVisibilityResult
                    {
                        ComicId = comicId,
                        Success = false,
                        ErrorMessage = ex.Message,
                        ComputationTime = DateTime.UtcNow,
                        ComputedVisibilities = Array.Empty<ComputedVisibilityData>()
                    });
                }
            }

            var endTime = DateTime.UtcNow;
            var duration = endTime - startTime;

            _logger.LogInformation(
                "Completed processing. Success: {Success}, Failed: {Failed}, Duration: {Duration}s",
                processedCount, failedCount, duration.TotalSeconds);

            // Update Prometheus metrics
            ComputeVisibilityDuration
                .WithLabels("visibility_computation_count", computationStatus)
                .Observe(duration.TotalSeconds);

            ComputeVisibilityCounter
                .WithLabels("visibility_computation_count", computationStatus)
                .Inc();

            req.ResponseSrc.TrySetResult(new VisibilityComputationResponse
            {
                StartId = startId,
                Limit = limit,
                ProcessedSuccessfully = processedCount,
                Failed = failedCount,
                DurationInSeconds = duration.TotalSeconds,
                NextStartId = startId + limit,
                Results = results.ToArray()
            });
        }
        catch (Exception ex)
        {
            computationStatus = "total_failure";
            _logger.LogError(ex, "Error during bulk visibility computation");

            // Update Prometheus metrics for total failure
            ComputeVisibilityCounter
                .WithLabels("visibility_computation_count", computationStatus)
                .Inc();
            req.ResponseSrc.TrySetResult(null);
        }
        finally
        {
            sw.Stop();
            ComputeVisibilityDuration
                .WithLabels("visibility_computation", computationStatus)
                .Observe(sw.Elapsed.TotalSeconds);
        }
    }

    private static bool ValidateRequest(long startId,
        int limit,
        out IResult? validationResult)
    {
        // Validate input
        if (startId < 1)
        {
            validationResult = Results.BadRequest("startId must be greater than 0");
            return false;
        }

        if (limit < 1)
        {
            validationResult = Results.BadRequest("limit must be greater than 0");
            return false;
        }

        if (limit > 20)
        {
            validationResult = Results.BadRequest("limit cannot exceed 20 comics");
            return false;
        }

        validationResult = null;
        return true;
    }
}