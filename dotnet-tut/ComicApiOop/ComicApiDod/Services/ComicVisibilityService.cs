using ComicApiDod.Data;
using Common.Metrics;
using Microsoft.EntityFrameworkCore;
using System.Diagnostics;
using Common.Models;

namespace ComicApiDod.Services;

public class ComicVisibilityService
{
    private readonly IDbContextFactory<ComicDbContext> _dbFactory;
    private readonly ILogger<ComicVisibilityService> _logger;
    private readonly IAppMetrics _appMetrics;

    public ComicVisibilityService(
        IDbContextFactory<ComicDbContext> dbFactory,
        ILogger<ComicVisibilityService> logger,
        IAppMetrics appMetrics)
    {
        _dbFactory = dbFactory;
        _logger = logger;
        _appMetrics = appMetrics;
    }

    public async Task ComputeVisibilities(int numOfRequest, List<VisibilityComputationRequest?> reqs)
    {
        var processingStartUtc = DateTime.UtcNow;
        foreach (var req in reqs)
        {
            if (req != null)
            {
                var waitSeconds = (processingStartUtc - req.RequestStartTimeUtc).TotalSeconds;
                _appMetrics.Observe(MetricNames.RequestWaitTimeSeconds, waitSeconds);
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
            var sortAttrs = new Dictionary<string, string> { ["status"] = "success" };
            _appMetrics.RecordLatency("comic_visibility_operation_sort_requests", sortSw.Elapsed.TotalSeconds, sortAttrs);
            _appMetrics.CaptureCount("comic_visibility_operation_sort_requests", 1, sortAttrs);

            _appMetrics.Set(MetricNames.RequestsInBatch, reqs.Count);
            _appMetrics.CaptureCount("visibility_batch_processing", 1, new Dictionary<string, string> { ["status"] = "started" });

            // Validate requests (metrics recorded inside method)
            bool[] isReqValid = ValidateAllRequests(reqs, out IResult?[] validationResults);

            // Filter valid requests
            var filterSw = Stopwatch.StartNew();
            List<VisibilityComputationRequest> validatedRequests = FilterValidRequests(reqs, isReqValid,
                out IDictionary<int, int> originalIndices);
            var filterAttrs = new Dictionary<string, string> { ["status"] = "success" };
            _appMetrics.RecordLatency("comic_visibility_operation_filter_requests", filterSw.Elapsed.TotalSeconds,
                filterAttrs);
            _appMetrics.CaptureCount("comic_visibility_operation_filter_requests", 1, filterAttrs);

            // Generate comic IDs
            var generateSw = Stopwatch.StartNew();
            long[][] allComicIds = GenerateAllComicIds(validatedRequests);
            var genAttrs = new Dictionary<string, string> { ["status"] = "success" };
            _appMetrics.RecordLatency("comic_visibility_operation_generate_comic_ids", generateSw.Elapsed.TotalSeconds,
                genAttrs);
            _appMetrics.CaptureCount("comic_visibility_operation_generate_comic_ids", 1, genAttrs);

            // Fetch batch data
            // ComicBook[][] comicBatchData = await FetchBatchDataForComics(allComicIds);
            //
            // // Compute visibility
            // ComicVisibilityResult[][] visibilityResults = ComputeVisibility(comicBatchData);

            await using ComicDbContext db = _dbFactory.CreateDbContext();
            long[] comicIds = allComicIds.SelectMany(i => i).ToHashSet().ToArray();

            DodSqlHelper.DodVisibilityBatch dodVisibilityBatch;
            var fetchSw = Stopwatch.StartNew();
            try
            {
                dodVisibilityBatch = await DodSqlHelper.FetchVisibilityBatchAsync(db, comicIds, _appMetrics, tkn);
            }
            finally
            {
                _appMetrics.RecordLatency("comic_visibility_db_query_fetch_visibility_batch",
                    fetchSw.Elapsed.TotalSeconds);
            }

            ComicVisibilityResult[][] visibilityResults;
            var computeSw = Stopwatch.StartNew();
            string computeStatus = "success";
            try
            {
                visibilityResults = ComputeVisibilityDod2(allComicIds, dodVisibilityBatch);
            }
            catch
            {
                computeStatus = "failure";
                throw;
            }
            finally
            {
                var computeAttrs = new Dictionary<string, string> { ["status"] = computeStatus };
                _appMetrics.RecordLatency("comic_visibility_operation_compute_visibility_dod_style2",
                    computeSw.Elapsed.TotalSeconds, computeAttrs);
                _appMetrics.CaptureCount("comic_visibility_operation_compute_visibility_dod_style2", 1, computeAttrs);
            }

            // Save computed visibility
            await SaveComputedVisibility(visibilityResults);

            SetResponse(reqs, isReqValid, visibilityResults, originalIndices);
        }
        catch (Exception ex)
        {
            computationStatus = "batch_failure";
            _appMetrics.CaptureCount("visibility_error", 1,
                new Dictionary<string, string> { ["status"] = $"{ex.GetType().Name}_batch_processing" });
            throw;
        }
        finally
        {
            var duration = sw.Elapsed;
            var batchAttrs = new Dictionary<string, string> { ["status"] = computationStatus };
            _appMetrics.RecordLatency("visibility_batch_processing", duration.TotalSeconds, batchAttrs);
            _appMetrics.CaptureCount("visibility_batch_processing", 1, batchAttrs);
        }
    }

    private static void SetResponse(List<VisibilityComputationRequest?> reqs,
        bool[] isReqValid,
        ComicVisibilityResult[][] visibilityResults,
        IDictionary<int, int> originalIndices)
    {
        for (int i = 0; i < reqs.Count; i++)
        {
            if (isReqValid[i]
                && visibilityResults[originalIndices[i]] != null)
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
        var validateAttrs = new Dictionary<string, string> { ["status"] = "success" };
        _appMetrics.RecordLatency("comic_visibility_operation_validate_all_requests", sw.Elapsed.TotalSeconds,
            validateAttrs);
        _appMetrics.CaptureCount("comic_visibility_operation_validate_all_requests", 1, validateAttrs);

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
                    : computedVisibilityList[0..(visIdx)]
            });
        }

        return results;
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

            var querySw = Stopwatch.StartNew();
            await DatabaseQueryHelper.SaveComputedVisibilitiesBulkAsync(_dbFactory, visibilityResultsToSave);
            _appMetrics.RecordLatency("comic_visibility_db_query_save_computed_visibilities",
                querySw.Elapsed.TotalSeconds);
        }
        catch (Exception ex)
        {
            status = "failure";
            _logger.LogError(ex, "Error saving computed visibility");
            throw;
        }
        finally
        {
            var saveAttrs = new Dictionary<string, string> { ["status"] = status };
            _appMetrics.RecordLatency("comic_visibility_operation_save_computed_visibility", sw.Elapsed.TotalSeconds,
                saveAttrs);
            _appMetrics.CaptureCount("comic_visibility_operation_save_computed_visibility", 1, saveAttrs);
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