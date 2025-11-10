using ComicApiDod.Data;
using ComicApiDod.Models;
using Microsoft.EntityFrameworkCore;
using Prometheus;
using System.Diagnostics;

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
            LabelNames = new[] { "status" }
        });

    private static readonly Counter ComputeVisibilityCounter = Metrics.CreateCounter(
        "comic_visibility_computation_total",
        "Total number of visibility computations",
        new CounterConfiguration
        {
            LabelNames = new[] { "status" }
        });

    private static readonly Gauge ComputedComicsGauge = Metrics.CreateGauge(
        "comic_visibility_computed_comics",
        "Number of comics processed in the last computation",
        new GaugeConfiguration
        {
            LabelNames = new[] { "status" }
        });

    public ComicVisibilityService(ComicDbContext db, ILogger<ComicVisibilityService> logger)
    {
        _db = db;
        _logger = logger;
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

            _logger.LogInformation("Computing visibility for comics starting from ID {StartId}, limit {Limit}", startId, limit);

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

            ComputedComicsGauge
                .WithLabels("processed")
                .Set(processedCount);

            ComputedComicsGauge
                .WithLabels("failed")
                .Set(failedCount);

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
