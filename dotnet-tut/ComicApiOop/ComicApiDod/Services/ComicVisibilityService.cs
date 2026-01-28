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

    private static readonly Histogram OperationDuration = Metrics.CreateHistogram(
        "comic_visibility_operation_duration_seconds",
        "Duration of individual operations within visibility computation",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.001, 2, 12),
            LabelNames = new[] { "operation", "status" }
        });

    private static readonly Histogram DbQueryDuration = Metrics.CreateHistogram(
        "comic_visibility_db_query_duration_seconds",
        "Duration of database queries in visibility computation",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.001, 2, 12),
            LabelNames = new[] { "query_type" }
        });

    private static readonly Counter ComputeVisibilityCounter = Metrics.CreateCounter(
        "comic_visibility_computation_total",
        "Total number of visibility computations",
        new CounterConfiguration
        {
            LabelNames = new[] { "type", "status" }
        });

    private static readonly Counter OperationCounter = Metrics.CreateCounter(
        "comic_visibility_operation_total",
        "Total number of operations within visibility computation",
        new CounterConfiguration
        {
            LabelNames = new[] { "operation", "status" }
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
            // Sort requests
            var sortSw = Stopwatch.StartNew();
            SortRequests(reqs);
            OperationDuration
                .WithLabels("sort_requests", "success")
                .Observe(sortSw.Elapsed.TotalSeconds);
            OperationCounter
                .WithLabels("sort_requests", "success")
                .Inc();

            RequestsInBatch.Set(reqs.Count);
            _metrics.RecordBatchProcessing(reqs.Count, TimeSpan.Zero, "started");

            // Validate requests (metrics recorded inside method)
            bool[] isReqValid = ValidateAllRequests(reqs, out IResult?[] validationResults);

            // Filter valid requests
            var filterSw = Stopwatch.StartNew();
            List<VisibilityComputationRequest> validatedRequests = FilterValidRequests(reqs, isReqValid,
                out IDictionary<int, int> originalIndices);
            OperationDuration
                .WithLabels("filter_requests", "success")
                .Observe(filterSw.Elapsed.TotalSeconds);
            OperationCounter
                .WithLabels("filter_requests", "success")
                .Inc();

            // Generate comic IDs
            var generateSw = Stopwatch.StartNew();
            long[][] allComicIds = GenerateAllComicIds(validatedRequests);
            OperationDuration
                .WithLabels("generate_comic_ids", "success")
                .Observe(generateSw.Elapsed.TotalSeconds);
            OperationCounter
                .WithLabels("generate_comic_ids", "success")
                .Inc();

            // Fetch batch data
            ComicBook[][] comicBatchData = await FetchBatchDataForComics(allComicIds);

            // Compute visibility
            ComicVisibilityResult[][] visibilityResults = ComputeVisibility(comicBatchData);

            // Save computed visibility
            await SaveComputedVisibility(visibilityResults);

            SetResponse(reqs, isReqValid, visibilityResults, originalIndices);
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

    private static void SetResponse(List<VisibilityComputationRequest?> reqs,
        bool[] isReqValid,
        ComicVisibilityResult[][] visibilityResults,
        IDictionary<int, int> originalIndices)
    {
        for (int i = 0; i < reqs.Count; i++)
        {
            if (isReqValid[i] && visibilityResults[originalIndices[i]] != null)
            {
                reqs[i].ResponseSrc.TrySetResult(new VisibilityComputationResponse
                {
                    Id = reqs[i].Id,
                    StartId = reqs[i].StartId,
                    Limit = reqs[i].Limit,
                    NextStartId = reqs[i].StartId + reqs[i].Limit,
                    Results = visibilityResults[originalIndices[i]]
                });
            }
            else if (reqs[i] != null)
            {
                reqs[i].ResponseSrc.TrySetResult(new VisibilityComputationResponse
                {
                    Id = reqs[i].Id,
                    StartId = reqs[i].StartId,
                    Limit = reqs[i].Limit,
                    NextStartId = reqs[i].StartId + reqs[i].Limit,
                    Results = Array.Empty<ComicVisibilityResult>()
                });
            }
        }
    }

    private static List<VisibilityComputationRequest> FilterValidRequests(List<VisibilityComputationRequest?> reqs,
        bool[] isReqValid,
        out IDictionary<int, int> originalIndices)
    {
        originalIndices = new Dictionary<int, int>(reqs.Count);
        List<VisibilityComputationRequest> validatedReqs = new List<VisibilityComputationRequest>();
        for (int i = 0; i < reqs.Count; i++)
        {
            if (isReqValid[i])
            {
                validatedReqs.Add(reqs[i]);
                originalIndices.Add(i, validatedReqs.Count - 1);
            }
        }

        return validatedReqs;
    }

    private void SortRequests(List<VisibilityComputationRequest?> reqs)
    {
        if (reqs.Count <= 1) return;

        reqs.Sort((v1, v2) =>
        {
            if (v1 == null && v2 == null) return 0;
            if (v1 == null) return 1;
            if (v2 == null) return -1;
            return v1.StartId.CompareTo(v2.StartId);
        });

        Utils.PrintCollection(reqs);
    }

    private bool[] ValidateAllRequests(List<VisibilityComputationRequest?> reqs,
        out IResult?[] validationResults)
    {
        var sw = Stopwatch.StartNew();
        validationResults = new IResult?[reqs.Count];
        bool[] isValid = new bool[reqs.Count];

        for (int i = 0; i < reqs.Count; i++)
        {
            if (reqs[i] == null)
            {
                validationResults[i] = Results.BadRequest("Req is  null");
                isValid[i] = false;
            }
            else if (!ValidateRequest(reqs[i].StartId, reqs[i].Limit, out IResult? validationResult))
            {
                validationResults[i] = validationResult;
                isValid[i] = false;
            }
            else
            {
                validationResults[i] = null;
                isValid[i] = true;
            }
        }

        // Record validation metrics
        OperationDuration
            .WithLabels("validate_all_requests", "success")
            .Observe(sw.Elapsed.TotalSeconds);
        OperationCounter
            .WithLabels("validate_all_requests", "success")
            .Inc();

        return isValid;
    }

    private long[][] GenerateAllComicIds(List<VisibilityComputationRequest> reqs)
    {
        long[][] allComicIds = new long[reqs.Count][];
        for (int i = 0; i < reqs.Count; i++)
        {
            allComicIds[i] = new long[reqs[i].Limit];
            for (int j = 0; j < reqs[i].Limit; j++) allComicIds[i][j] = reqs[i].StartId + j;
        }

        return allComicIds;
    }

    private async Task<ComicBook[][]> FetchBatchDataForComics(long[][] allComicIds)
    {
        var sw = Stopwatch.StartNew();
        string status = "success";
        try
        {
            await using ComicDbContext db = _dbFactory.CreateDbContext();
            long[] comicIds = allComicIds.SelectMany(i => i).ToArray();

            var querySw = Stopwatch.StartNew();
            IDictionary<long, ComicBook>
                batchData = await DatabaseQueryHelper.GetComicBatchDataAsync(db, comicIds);
            DbQueryDuration
                .WithLabels("fetch_batch_data")
                .Observe(querySw.Elapsed.TotalSeconds);

            ComicBook[][] comicBookArrays = new ComicBook[allComicIds.Length][];

            for (int i = 0; i < allComicIds.Length; i++)
            {
                comicBookArrays[i] = new ComicBook[allComicIds[i].Length];
                for (int j = 0; j < allComicIds[i].Length; j++)
                {
                    if (batchData.ContainsKey(allComicIds[i][j]))
                    {
                        comicBookArrays[i][j] = batchData[allComicIds[i][j]];
                    }
                    else
                    {
                        comicBookArrays[i][j] = null;
                    }
                }
            }

            return comicBookArrays;
        }
        catch (Exception ex)
        {
            status = "failure";
            _logger.LogError(ex, "Error fetching batch data for comics");
            throw;
        }
        finally
        {
            OperationDuration
                .WithLabels("fetch_batch_data", status)
                .Observe(sw.Elapsed.TotalSeconds);
            OperationCounter
                .WithLabels("fetch_batch_data", status)
                .Inc();
        }
    }

    private ComicVisibilityResult[][] ComputeVisibility(ComicBook[][] comicBooks)
    {
        var sw = Stopwatch.StartNew();
        string computationStatus = "success";
        try
        {
            ComicVisibilityResult[][] results = new ComicVisibilityResult[comicBooks.Length][];

            int processedCount = 0;
            int failedCount = 0;
            DateTime computationTime = DateTime.UtcNow;
            for (int i = 0; i < comicBooks.Length; i++)
            {
                results[i] = new ComicVisibilityResult[comicBooks[i].Length];
                for (int j = 0; j < comicBooks[i].Length; j++)
                {
                    var itemSw = Stopwatch.StartNew();
                    ComicBook comicBook = comicBooks[i][j];
                    if (comicBook == null)
                    {
                        results[i][j] = null;
                        continue;
                    }

                    ComputedVisibilityData[] computedVisibilities = VisibilityProcessor.ComputeVisibilities(
                        comicBooks[i][j],
                        computationTime);
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
                            "GeographicRules={GeoCount}, SegmentRules={SegmentCount}",
                            comicBook.Id,
                            comicBook.GeographicRules.Count,
                            comicBook.CustomerSegmentRules.Count);
                    }

                    results[i][j] = new ComicVisibilityResult
                    {
                        ComicId = comicBook.Id,
                        Success = computedVisibilities.Length > 0,
                        ErrorMessage = computedVisibilities.Length == 0
                            ? "No visibilities computed - missing or invalid geographic/segment rules"
                            : null,
                        ComputationTime = computationTime,
                        ComputedVisibilities = computedVisibilities
                    };

                    // Record per-item computation latency
                    OperationDuration
                        .WithLabels("compute_visibility_item", computedVisibilities.Length > 0 ? "success" : "failure")
                        .Observe(itemSw.Elapsed.TotalSeconds);
                }
            }

            return results;
        }
        catch (Exception ex)
        {
            computationStatus = "failure";
            _logger.LogError(ex, "Error computing visibility");
            throw;
        }
        finally
        {
            OperationDuration
                .WithLabels("compute_visibility", computationStatus)
                .Observe(sw.Elapsed.TotalSeconds);
            OperationCounter
                .WithLabels("compute_visibility", computationStatus)
                .Inc();
        }
    }

    private async Task SaveComputedVisibility(ComicVisibilityResult[][] visibilityResults)
    {
        var sw = Stopwatch.StartNew();
        string status = "success";
        try
        {
            ComputedVisibilityData[] visibilityResultsToSave = visibilityResults
                .SelectMany(results => results
                    .Where(res => res is { ComputedVisibilities: not null })
                    .SelectMany(res => res.ComputedVisibilities))
                .ToArray();

            await using ComicDbContext db = _dbFactory.CreateDbContext();

            var querySw = Stopwatch.StartNew();
            await DatabaseQueryHelper.SaveComputedVisibilitiesBulkAsync(db, visibilityResultsToSave);
            DbQueryDuration
                .WithLabels("save_computed_visibilities")
                .Observe(querySw.Elapsed.TotalSeconds);
        }
        catch (Exception ex)
        {
            status = "failure";
            _logger.LogError(ex, "Error saving computed visibility");
            throw;
        }
        finally
        {
            OperationDuration
                .WithLabels("save_computed_visibility", status)
                .Observe(sw.Elapsed.TotalSeconds);
            OperationCounter
                .WithLabels("save_computed_visibility", status)
                .Inc();
        }
    }

    private bool ValidateRequest(long startId,
        int limit,
        out IResult? validationResult)
    {
        // Validate input
        if (startId < 1)
        {
            validationResult = Results.BadRequest("startId must be greater than 0");
            _logger.LogError("Computing visibility for comics starting from ID {StartId}, limit {Limit}", startId,
                limit);
            return false;
        }

        if (limit < 1)
        {
            validationResult = Results.BadRequest("limit must be greater than 0");
            _logger.LogError("Computing visibility for comics starting from ID {StartId}, limit {Limit}", startId,
                limit);
            return false;
        }

        if (limit > 20)
        {
            validationResult = Results.BadRequest("limit cannot exceed 20 comics");
            _logger.LogError("Computing visibility for comics starting from ID {StartId}, limit {Limit}", startId,
                limit);
            return false;
        }

        validationResult = null;
        return true;
    }
}