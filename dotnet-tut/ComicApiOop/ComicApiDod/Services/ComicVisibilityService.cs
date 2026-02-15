using ComicApiDod.Data;
using Microsoft.EntityFrameworkCore;
using Prometheus;
using System.Diagnostics;
using ComicApiDod.utils;
using Common.Models;

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

    private static readonly Histogram RequestWaitTimeSeconds = Metrics.CreateHistogram(
        "comic_visibility_request_wait_seconds",
        "Time from request creation (enqueue) to start of processing",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.001, 2, 14),
            LabelNames = Array.Empty<string>()
        });

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
        var processingStartUtc = DateTime.UtcNow;
        foreach (var req in reqs)
        {
            if (req != null)
            {
                var waitSeconds = (processingStartUtc - req.RequestStartTimeUtc).TotalSeconds;
                RequestWaitTimeSeconds.Observe(waitSeconds);
            }
        }

        CancellationToken tkn = new CancellationTokenSource().Token;
        var sw = Stopwatch.StartNew();
        string computationStatus = "success";
        try
        {
            // Sort requests
            Stopwatch sortSw = Stopwatch.StartNew();
            SortRequests(reqs);
            Metric.RecordReqMetrics(reqs, OperationDuration, OperationCounter, RequestsInBatch, _metrics, sortSw);

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
            // ComicBook[][] comicBatchData = await FetchBatchDataForComics(allComicIds);
            //
            // // Compute visibility
            // ComicVisibilityResult[][] visibilityResults = ComputeVisibility(comicBatchData);

            await using ComicDbContext db = _dbFactory.CreateDbContext();
            long[] comicIds = allComicIds.SelectMany(i => i).ToHashSet().ToArray();
            DodSqlHelper.DodVisibilityBatch dodVisibilityBatch = await DodSqlHelper.FetchVisibilityBatchAsync(db,
                comicIds, tkn);

            ComicVisibilityResult[][] visibilityResults = ComputeVisibilityDod2(allComicIds, dodVisibilityBatch);

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

    // private ComicVisibilityResult[][] ComputeVisibility(ComicBook[][] comicBooks)
    // {
    //     // return computeVisibilityOopStyle(comicBooks);
    //     return ComputeVisibilityDodStyle(comicBooks);
    // }

    // private ComicVisibilityResult[][] ComputeVisibilityDodStyle(ComicBook[][] comicBooks)
    // {
    //     DateTime computationTime = DateTime.UtcNow;
    //     // comicBooks -> Req x Comics per req
    //
    //     // compute pre-filters - Geo and Segment
    //     // ComicGeoSegFilter[Req][Comic] 
    //     ComicGeoSegFilter[][] geoSegFilters = ComicGeoSegFilter.GenerateGeoSegFilters(comicBooks, computationTime);
    //
    //     // ComicGeoSegFilter[Req][Comic] 
    //     ComicGeoPricing[][] geoPricingOfComics = ComicGeoPricing.GenerateGeoPricingRules(comicBooks, geoSegFilters);
    //
    //     // ComicMeta[Req][Comic]
    //     ComicMeta[][] comicMetas = ComicMeta.GenerateComicMeta(comicBooks, geoPricingOfComics);
    //
    //     ComputedVisibilityData emptyData = new ComputedVisibilityData();
    //     ComicVisibilityResult[][] results = new ComicVisibilityResult[comicBooks.Length][];
    //     for (int reqIdx = 0; reqIdx < comicBooks.Length; reqIdx++)
    //     {
    //         results[reqIdx] = new ComicVisibilityResult[comicBooks[reqIdx].Length];
    //         for (int comicIdx = 0; comicIdx < comicBooks[reqIdx].Length; comicIdx++)
    //         {
    //             ComicBook comic = comicBooks[reqIdx][comicIdx];
    //             ComputedVisibilityData[] computedVisibilityList =
    //                 new ComputedVisibilityData[geoSegFilters[reqIdx][comicIdx].totalVisibleCount];
    //             int visIdx = 0;
    //             for (int geoIdx = 0; geoIdx < comic.GeographicRules.Count; geoIdx++)
    //             {
    //                 if (!geoSegFilters[reqIdx][comicIdx].geoFilter[geoIdx]) continue;
    //                 string countryCode = comic.GeographicRules[geoIdx].CountryCodes.FirstOrDefault() ?? string.Empty;
    //                 for (int segIdx = 0; segIdx < comic.CustomerSegmentRules.Count; segIdx++)
    //                 {
    //                     if (!geoSegFilters[reqIdx][comicIdx].segmentFilter[segIdx]) continue;
    //
    //                     computedVisibilityList[visIdx++] = new ComputedVisibilityData
    //                     {
    //                         ComicId = comic.Id,
    //                         CountryCode = countryCode,
    //                         CustomerSegmentId = comic.CustomerSegmentRules[segIdx].SegmentId,
    //                         FreeChaptersCount = comicMetas[reqIdx][comicIdx].freeChapterCount,
    //                         LastChapterReleaseTime = comicMetas[reqIdx][comicIdx].lastChapterReleaseTime,
    //                         GenreId = comic.GenreId,
    //                         PublisherId = comic.PublisherId,
    //                         AverageRating = comic.AverageRating,
    //                         SearchTags = comicMetas[reqIdx][comicIdx].searchTags,
    //                         IsVisible = true,
    //                         ComputedAt = computationTime,
    //                         LicenseType = comic.GeographicRules[geoIdx].LicenseType,
    //                         CurrentPrice = geoPricingOfComics[reqIdx][comicIdx].pricing[geoIdx]?.BasePrice ?? 0m,
    //                         IsFreeContent =
    //                             geoPricingOfComics[reqIdx][comicIdx].pricing[geoIdx]?.IsFreeContent ?? false,
    //                         IsPremiumContent =
    //                             geoPricingOfComics[reqIdx][comicIdx].pricing[geoIdx]?.IsPremiumContent ??
    //                             false,
    //                         AgeRating = comic.ContentRating?.AgeRating ?? AgeRating.AllAges,
    //                         ContentFlags = comicMetas[reqIdx][comicIdx].contentFlag[geoIdx],
    //                         ContentWarning = comic.ContentRating?.ContentWarning ?? string.Empty
    //                     };
    //                 }
    //             }
    //
    //             results[reqIdx][comicIdx] = new ComicVisibilityResult
    //             {
    //                 ComicId = comicMetas[reqIdx][comicIdx].comicId,
    //                 Success = true,
    //                 ErrorMessage = null,
    //                 ComputationTime = computationTime,
    //                 ComputedVisibilities = computedVisibilityList
    //             };
    //         }
    //     }
    //
    //     return results;
    // }

    private ComicVisibilityResult[][] ComputeVisibilityDod2(
        long[][] requestComicIds,
        DodSqlHelper.DodVisibilityBatch comicBatch)
    {
        try
        {
            IDictionary<long, ComicVisibilityResult> visibilities = ComputeVisibilityDodStyle2(comicBatch);

            ComicVisibilityResult[][] results = new ComicVisibilityResult[requestComicIds.Length][];
            for (int i = 0; i < requestComicIds.Length; i++)
            {
                int numOfComics = requestComicIds[i].Length;
                results[i] = new ComicVisibilityResult[numOfComics];
                for (int comicIdx = 0; comicIdx < numOfComics; comicIdx++)
                {
                    long comicId = requestComicIds[i][comicIdx];
                    results[i][comicIdx] = visibilities[comicId];
                }
            }

            return results;
        }
        catch (Exception ex)
        {
            Console.WriteLine(ex.Message);
            return new ComicVisibilityResult[0][];
        }
    }

    private IDictionary<long, ComicVisibilityResult> ComputeVisibilityDodStyle2(
        DodSqlHelper.DodVisibilityBatch comicBatch)
    {
        DateTime computationTime = DateTime.UtcNow;
        // comicBooks -> Req x Comics per req

        // compute pre-filters - Geo and Segment
        // ComicGeoSegFilter[Comic] 
        ComicGeoSegFilter geoSegFilters = ComicGeoSegFilter.GenerateGeoSegFilters(
            comicBatch.GeoRules,
            comicBatch.SegmentRules,
            computationTime);

        // ComicGeoSegFilter[Comic] 
        DodSqlHelper.PricingRow[] pricingRows = comicBatch.Pricings;
        IDictionary<PricingRegionKey, int> pricingIndex = new Dictionary<PricingRegionKey, int>();
        for (int pricingIdx = 0; pricingIdx < pricingRows.Length; pricingIdx++)
        {
            PricingRegionKey key = PricingRegionKey.of(pricingRows[pricingIdx].ComicId,
                pricingRows[pricingIdx].RegionCode);

            if (pricingIndex.ContainsKey(key)) continue;

            pricingIndex.Add(key, pricingIdx);
        }


        // ComicMeta[Comic] without content flags
        IDictionary<long, ComicMeta> comicMetaMap = ComicMeta.GenerateComicMeta(comicBatch);

        for (int pricingIdx = 0; pricingIdx < pricingRows.Length; pricingIdx++)
        {
            long comicId = pricingRows[pricingIdx].ComicId;
            comicMetaMap[comicId].contentFlag[pricingIdx] = VisibilityProcessor.DetermineContentFlags(
                (ContentFlag)comicBatch.ContentRatings[comicId].ContentFlags,
                comicMetaMap[comicId].allChaptersFree,
                comicMetaMap[comicId].freeChapterCount > 0,
                pricingRows[pricingIdx].BasePrice
            );
        }

        IDictionary<long, ComicVisibilityResult> results = new Dictionary<long, ComicVisibilityResult>();
        for (int comicIdx = 0; comicIdx < comicBatch.Comics.Length; comicIdx++)
        {
            DodSqlHelper.ComicRow comic = comicBatch.Comics[comicIdx];
            if (results.ContainsKey(comic.Id)) continue;

            IList<int> geoRuleIdxList = geoSegFilters.comicToGeoIndex[comic.Id];
            IList<int> segRuleIdxList = geoSegFilters.comicToSegIndex[comic.Id];
            long numberOfVisibilities = geoSegFilters.totalVisibleCount[comic.Id];

            ComputedVisibilityData[] computedVisibilityList = new ComputedVisibilityData[numberOfVisibilities];
            int visIdx = 0;
            foreach (int geoRuleId in geoRuleIdxList)
            {
                if (!geoSegFilters.geoFilter[geoRuleId]) continue;

                // CountryCodes can't be empty since we set filter to false if it is.
                // string countryCode = comicBatch.GeoRules[geoRuleId].CountryCodes.FirstOrDefault();
                foreach (string countryCode in comicBatch.GeoRules[geoRuleId].CountryCodes)
                {
                    foreach (int segRuleId in segRuleIdxList)
                    {
                        if (!geoSegFilters.segmentFilter[segRuleId]) continue;
                        PricingRegionKey pricingRegionKey = PricingRegionKey.of(comic.Id, countryCode);
                        if (!pricingIndex.ContainsKey(pricingRegionKey)) continue;
                        try
                        {
                            computedVisibilityList[visIdx++] = new ComputedVisibilityData
                            {
                                ComicId = comic.Id,
                                CountryCode = countryCode,
                                CustomerSegmentId = comicBatch.SegmentRules[segRuleId].SegmentId,
                                FreeChaptersCount = comicMetaMap[comic.Id].freeChapterCount,
                                LastChapterReleaseTime = comicMetaMap[comic.Id].lastChapterReleaseTime,
                                GenreId = comic.GenreId,
                                PublisherId = comic.PublisherId,
                                AverageRating = comic.AverageRating,
                                SearchTags = comicMetaMap[comic.Id].searchTags,
                                IsVisible = true,
                                ComputedAt = computationTime,
                                LicenseType = (LicenseType)comicBatch.GeoRules[geoRuleId].LicenseType,
                                CurrentPrice = pricingRows[pricingIndex[pricingRegionKey]].BasePrice,
                                IsFreeContent = pricingRows[pricingIndex[pricingRegionKey]].IsFreeContent,
                                IsPremiumContent = pricingRows[pricingIndex[pricingRegionKey]].IsPremiumContent,
                                AgeRating = (AgeRating)comicBatch.ContentRatings[comic.Id].AgeRating,
                                ContentFlags = comicMetaMap[comic.Id].contentFlag[pricingIndex[pricingRegionKey]],
                                ContentWarning = comicBatch.ContentRatings[comic.Id].ContentWarning ?? string.Empty
                            };
                        }
                        catch (Exception ex)
                        {
                            Console.WriteLine(ex.Message);
                        }
                    }
                }
            }

            results.Add(comic.Id, new ComicVisibilityResult
            {
                ComicId = comic.Id,
                Success = true,
                ErrorMessage = null,
                ComputationTime = computationTime,
                ComputedVisibilities = visIdx == 0
                    ? new ComputedVisibilityData[0]
                    : computedVisibilityList[0..(visIdx + 1)]
            });
        }

        return results;
    }

    private ComicVisibilityResult[][] computeVisibilityOopStyle(ComicBook[][] comicBooks)
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
                        .WithLabels("compute_visibility_item",
                            computedVisibilities.Length > 0 ? "success" : "failure")
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