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
    private readonly ComicDbContext _db;
    private readonly ILogger<ComicVisibilityService> _logger;

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

    public ComicVisibilityService(ComicDbContext db, ILogger<ComicVisibilityService> logger)
    {
        _db = db;
        _logger = logger;
    }

    public Task<IValue[]> ComputeVisibilities(int numOfRequest, List<VisibilityComputationRequest?> reqs)
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

            List<Task<IValue>> responses = new List<Task<IValue>>();


            foreach (var req in reqs)
            {
                // req.ResponseSrc.TrySetResult(ComputeVisibilitiesAsync(req));
                responses.Add(ComputeVisibilitiesAsync(req));
            }

            var t = responses.ToArray();
            return Task.WhenAll(t);
        }
        finally
        {
            ComputeVisibilityDuration
                .WithLabels("batch_process", computationStatus)
                .Observe(sw.Elapsed.TotalSeconds);
        }
    }

    public async Task<IValue> ComputeVisibilitiesAsync(VisibilityComputationRequest req)
    {
        Stopwatch swForReqE2e = Stopwatch.StartNew();
        var sw = Stopwatch.StartNew();
        string computationStatus = "success";
        long startId = req.StartId;
        int limit = req.Limit;
        var startTime = DateTime.UtcNow;

        try
        {
            // Validate input
            if (startId < 1 || limit < 1 || limit > 20) return null;

            _logger.LogInformation("Computing visibility for comics starting from ID {StartId}, limit {Limit}", startId,
                limit);

            // Step 1: Get comic IDs (batch query)
            sw.Restart();
            var comicIds = await DatabaseQueryHelper.GetComicIdsAsync(_db, (int)startId, limit);
            ComputeVisibilityDuration
                .WithLabels("fetch_comics", computationStatus)
                .Observe(sw.Elapsed.TotalSeconds);

            if (comicIds.Length == 0)
            {
                computationStatus = "no_comics_found";
                return null;
            }

            _logger.LogInformation("Found {Count} comics to process", comicIds.Length);

            // Step 2: Process each comic (in DOD, we process data in batches)
            var results = new List<ComicVisibilityResult>();
            int processedCount = 0;
            int failedCount = 0;

            sw.Restart();
            IDictionary<long, ComicBatchData> allComicsBatchData =
                await DatabaseQueryHelper.GetComicBatchDataAsync(_db, comicIds);
            ComputeVisibilityDuration
                .WithLabels("fetch_all_comics_data", computationStatus)
                .Observe(sw.Elapsed.TotalSeconds);

            foreach (var comicId in comicIds)
            {
                try
                {
                    ComicBatchData batchData = allComicsBatchData[comicId];
                    _logger.LogInformation("Processing comic ID: {ComicId}", comicId);

                    // Fetch all data for this comic

                    // Compute visibilities using pure functions
                    sw.Restart();
                    var computedVisibilities = VisibilityProcessor.ComputeVisibilities(
                        batchData,
                        DateTime.UtcNow);
                    ComputeVisibilityDuration
                        .WithLabels("compute_visibility", computationStatus)
                        .Observe(sw.Elapsed.TotalSeconds);


                    // Save to database
                    if (computedVisibilities.Length > 0)
                    {
                        sw.Restart();
                        await DatabaseQueryHelper.SaveComputedVisibilitiesAsync(_db, computedVisibilities);
                        ComputeVisibilityDuration
                            .WithLabels("save_visibility", computationStatus)
                            .Observe(sw.Elapsed.TotalSeconds);
                    }

                    results.Add(new ComicVisibilityResult
                    {
                        ComicId = comicId,
                        Success = true,
                        ComputationTime = DateTime.UtcNow,
                        ComputedVisibilities = computedVisibilities
                    });

                    processedCount++;
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
                .WithLabels("visibility_computation", computationStatus)
                .Observe(duration.TotalSeconds);

            ComputeVisibilityCounter
                .WithLabels("visibility_computation", computationStatus)
                .Inc();

            VisibilityComputationResponse response = new()
            {
                Id = req.Id,
                StartId = startId,
                Limit = limit,
                ProcessedSuccessfully = processedCount,
                Failed = failedCount,
                DurationInSeconds = duration.TotalSeconds,
                NextStartId = startId + limit,
                Results = results.ToArray()
            };

            req.ResponseSrc.TrySetResult(response);

            return response;
        }
        catch (Exception ex)
        {
            computationStatus = "total_failure";
            _logger.LogError(ex, "Error during bulk visibility computation");

            // Update Prometheus metrics for total failure
            ComputeVisibilityCounter
                .WithLabels("visibility_computation_count", computationStatus)
                .Inc();

            return null;
        }
        finally
        {
            swForReqE2e.Stop();
            ComputeVisibilityDuration
                .WithLabels("visibility_computation", computationStatus)
                .Observe(swForReqE2e.Elapsed.TotalSeconds);
        }
    }

    public async Task<IResult> ComputeVisibilitiesAsync(long startId, int limit)
    {
        var sw = Stopwatch.StartNew();
        string computationStatus = "success";

        try
        {
            // Validate input
            if (startId < 1) return Results.BadRequest("startId must be greater than 0");
            if (limit < 1) return Results.BadRequest("limit must be greater than 0");
            if (limit > 20) return Results.BadRequest("limit cannot exceed 20 comics");

            _logger.LogInformation("Computing visibility for comics starting from ID {StartId}, limit {Limit}", startId,
                limit);

            var startTime = DateTime.UtcNow;

            // Step 1: Get comic IDs (batch query)
            var comicIds = await DatabaseQueryHelper.GetComicIdsAsync(_db, (int)startId, limit);

            if (comicIds.Length == 0)
            {
                computationStatus = "no_comics_found";
                return Results.NotFound($"No comics found starting from ID {startId}");
            }

            _logger.LogInformation("Found {Count} comics to process", comicIds.Length);

            // Step 2: Process each comic (in DOD, we process data in batches)
            var results = new List<ComicVisibilityResult>();
            int processedCount = 0;
            int failedCount = 0;

            foreach (var comicId in comicIds)
            {
                try
                {
                    _logger.LogInformation("Processing comic ID: {ComicId}", comicId);

                    // Fetch all data for this comic
                    var batchData = await DatabaseQueryHelper.GetComicBatchDataAsync(_db, comicId);

                    // Compute visibilities using pure functions
                    var computedVisibilities = VisibilityProcessor.ComputeVisibilities(
                        batchData,
                        DateTime.UtcNow);

                    // Save to database
                    if (computedVisibilities.Length > 0)
                    {
                        await DatabaseQueryHelper.SaveComputedVisibilitiesAsync(_db, computedVisibilities);
                    }

                    results.Add(new ComicVisibilityResult
                    {
                        ComicId = comicId,
                        Success = true,
                        ComputationTime = DateTime.UtcNow,
                        ComputedVisibilities = computedVisibilities
                    });

                    processedCount++;
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
                .WithLabels(computationStatus)
                .Observe(duration.TotalSeconds);

            ComputeVisibilityCounter
                .WithLabels(computationStatus)
                .Inc();

            return Results.Ok(new VisibilityComputationResponse
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
                .WithLabels(computationStatus)
                .Inc();

            return Results.Problem(
                detail: ex.ToString(),
                title: "Error computing visibilities",
                statusCode: 500
            );
        }
        finally
        {
            sw.Stop();
            ComputeVisibilityDuration
                .WithLabels(computationStatus)
                .Observe(sw.Elapsed.TotalSeconds);
        }
    }
}