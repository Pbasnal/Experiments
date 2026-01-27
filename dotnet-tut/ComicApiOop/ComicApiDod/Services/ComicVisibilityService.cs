using ComicApiDod.Data;
using ComicApiDod.Models;
using ComicApiDod.SimpleQueue;
using Microsoft.EntityFrameworkCore;
using Prometheus;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.InteropServices.JavaScript;
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
            SortRequests(reqs);

            RequestsInBatch.Set(reqs.Count);
            _metrics.RecordBatchProcessing(reqs.Count, TimeSpan.Zero, "started");

            List<Task> responses = new List<Task>();
            bool[] isReqValid = ValidateAllRequests(reqs, out IResult?[] validationResults);

            List<VisibilityComputationRequest> validatedRequests = FilterValidRequests(reqs, isReqValid,
                out IDictionary<int, int> originalIndices);

            long[][] allComicIds = GenerateAllComicIds(validatedRequests);
            ComicBatchData[][] comicBatchData = await FetchBatchDataForComics(allComicIds);

            ComicVisibilityResult[][] visibilityResults = ComputeVisibility(comicBatchData);
            await SaveComputedVisibility(visibilityResults);

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
                        Results = []
                    });
                }
            }
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

    private async Task<ComicBatchData[][]> FetchBatchDataForComics(long[][] allComicIds)
    {
        await using ComicDbContext db = _dbFactory.CreateDbContext();
        long[] comicIds = allComicIds.SelectMany(i => i).ToArray();
        IDictionary<long, ComicBatchData> batchData = await DatabaseQueryHelper.GetComicBatchDataAsync(db, comicIds);

        ComicBatchData[][] comicBatchDataArrays = new ComicBatchData[allComicIds.Length][];

        for (int i = 0; i < allComicIds.Length; i++)
        {
            comicBatchDataArrays[i] = new ComicBatchData[allComicIds[i].Length];
            for (int j = 0; j < allComicIds[i].Length; j++)
            {
                if (batchData.ContainsKey(allComicIds[i][j]))
                {
                    comicBatchDataArrays[i][j] = batchData[allComicIds[i][j]];
                }
                else
                {
                    comicBatchDataArrays[i][j] = null;
                }
            }
        }

        return comicBatchDataArrays;
    }

    private ComicVisibilityResult[][] ComputeVisibility(ComicBatchData[][] comicBatchData)
    {
        ComicVisibilityResult[][] results = new ComicVisibilityResult[comicBatchData.Length][];

        int processedCount = 0;
        int failedCount = 0;
        string computationStatus = "success";
        DateTime computationTime = DateTime.UtcNow;
        for (int i = 0; i < comicBatchData.Length; i++)
        {
            results[i] = new ComicVisibilityResult[comicBatchData[i].Length];
            for (int j = 0; j < comicBatchData[i].Length; j++)
            {
                ComicBatchData batchData = comicBatchData[i][j];
                if (batchData == null)
                {
                    results[i][j] = null;
                    continue;
                }

                ComputedVisibilityData[] computedVisibilities = VisibilityProcessor.ComputeVisibilities(
                    comicBatchData[i][j],
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
                        "GeographicRules={GeoCount}, SegmentRules={SegmentCount}, Segments={SegmentsCount}",
                        batchData.ComicId,
                        batchData.GeographicRules.Length,
                        batchData.SegmentRules.Length,
                        batchData.Segments.Length);
                }

                results[i][j] = new ComicVisibilityResult
                {
                    ComicId = batchData.ComicId,
                    Success = computedVisibilities.Length > 0,
                    ErrorMessage = computedVisibilities.Length == 0
                        ? "No visibilities computed - missing or invalid geographic/segment rules"
                        : null,
                    ComputationTime = computationTime,
                    ComputedVisibilities = computedVisibilities
                };
            }
        }

        return results;
    }

    private async Task SaveComputedVisibility(ComicVisibilityResult[][] visibilityResults)
    {
        ComputedVisibilityData[] visibilityResultsToSave = visibilityResults
            .SelectMany(results => results
                .Where(res => res is { ComputedVisibilities: not null })
                .SelectMany(res => res.ComputedVisibilities))
            .ToArray();

        await using ComicDbContext db = _dbFactory.CreateDbContext();
        await DatabaseQueryHelper.SaveComputedVisibilitiesBulkAsync(db, visibilityResultsToSave);
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