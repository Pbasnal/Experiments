using Common.Models;
using ComicApiOop.Data;
using ComicApiOop.Metrics;
using ComicApiOop.Middleware;
using Microsoft.EntityFrameworkCore;

namespace ComicApiOop.Services;

public class VisibilityComputationService
{
    private readonly ComicDbContext _dbContext;
    private readonly ILogger<VisibilityComputationService> _logger;
    private readonly MetricsReporter _metricsReporter;
    private readonly IHttpContextAccessor _httpContextAccessor;

    public VisibilityComputationService(
        ComicDbContext dbContext,
        ILogger<VisibilityComputationService> logger,
        MetricsReporter metricsReporter,
        IHttpContextAccessor httpContextAccessor)
    {
        _dbContext = dbContext;
        _logger = logger;
        _metricsReporter = metricsReporter;
        _httpContextAccessor = httpContextAccessor;
    }

    /// <summary>
    /// Fetches all comics with required related data in a single query (batch fetch).
    /// Reduces DB round-trips from N to 1 for a bulk of N comics.
    /// </summary>
    private async Task<Dictionary<long, ComicBook>> FetchComicsWithAllDataAsync(
        IReadOnlyList<long> comicIds,
        string operationName)
    {
        if (comicIds.Count == 0)
            return new Dictionary<long, ComicBook>();

        var comics = await _metricsReporter.TrackQueryAsync(
            "fetch_comics_bulk",
            operationName,
            async () => await _dbContext.Comics
                .Where(c => comicIds.Contains(c.Id))
                .Include(c => c.Chapters)
                .Include(c => c.ContentRating)
                .Include(c => c.RegionalPricing)
                .Include(c => c.GeographicRules)
                .Include(c => c.CustomerSegmentRules)
                    .ThenInclude(csr => csr.Segment)
                .Include(c => c.ComicTags)
                    .ThenInclude(ct => ct.Tag)
                .AsNoTracking()
                .ToListAsync());

        return comics.ToDictionary(c => c.Id);
    }

    private async Task SaveComputedVisibilitiesAsync(
        ComputedVisibilityData[] computedVisibilities,
        string operationName)
    {
        await _metricsReporter.TrackQueryAsync(
            "save_visibilities",
            operationName,
            async () =>
            {
                var dbVisibilities = computedVisibilities.Select(cv => new ComputedVisibility
                {
                    ComicId = cv.ComicId,
                    CountryCode = cv.CountryCode,
                    CustomerSegmentId = cv.CustomerSegmentId,
                    FreeChaptersCount = cv.FreeChaptersCount,
                    LastChapterReleaseTime = cv.LastChapterReleaseTime,
                    GenreId = cv.GenreId,
                    PublisherId = cv.PublisherId,
                    AverageRating = cv.AverageRating,
                    SearchTags = cv.SearchTags,
                    IsVisible = cv.IsVisible,
                    ComputedAt = cv.ComputedAt,
                    LicenseType = cv.LicenseType,
                    CurrentPrice = cv.CurrentPrice,
                    IsFreeContent = cv.IsFreeContent,
                    IsPremiumContent = cv.IsPremiumContent,
                    AgeRating = cv.AgeRating,
                    ContentFlags = cv.ContentFlags,
                    ContentWarning = cv.ContentWarning
                }).ToList();

                await _dbContext.ComputedVisibilities.AddRangeAsync(dbVisibilities);
                return await _dbContext.SaveChangesAsync();
            });
    }

    /// <summary>
    /// Saves all computed visibilities in a single round-trip (batch save).
    /// Reduces DB round-trips from N to 1 for a bulk of N comics.
    /// </summary>
    private async Task SaveComputedVisibilitiesBulkAsync(
        IReadOnlyList<ComputedVisibilityData> computedVisibilities,
        string operationName)
    {
        if (computedVisibilities.Count == 0)
            return;

        await _metricsReporter.TrackQueryAsync(
            "save_visibilities_bulk",
            operationName,
            async () =>
            {
                var dbVisibilities = computedVisibilities.Select(cv => new ComputedVisibility
                {
                    ComicId = cv.ComicId,
                    CountryCode = cv.CountryCode,
                    CustomerSegmentId = cv.CustomerSegmentId,
                    FreeChaptersCount = cv.FreeChaptersCount,
                    LastChapterReleaseTime = cv.LastChapterReleaseTime,
                    GenreId = cv.GenreId,
                    PublisherId = cv.PublisherId,
                    AverageRating = cv.AverageRating,
                    SearchTags = cv.SearchTags,
                    IsVisible = cv.IsVisible,
                    ComputedAt = cv.ComputedAt,
                    LicenseType = cv.LicenseType,
                    CurrentPrice = cv.CurrentPrice,
                    IsFreeContent = cv.IsFreeContent,
                    IsPremiumContent = cv.IsPremiumContent,
                    AgeRating = cv.AgeRating,
                    ContentFlags = cv.ContentFlags,
                    ContentWarning = cv.ContentWarning
                }).ToList();

                await _dbContext.ComputedVisibilities.AddRangeAsync(dbVisibilities);
                return await _dbContext.SaveChangesAsync();
            });
    }

    private static ComicVisibilityResult CreateNotFoundResult(long comicId)
    {
        return new ComicVisibilityResult
        {
            ComicId = comicId,
            Success = false,
            ErrorMessage = "Comic not found",
            ComputationTime = DateTime.UtcNow,
            ComputedVisibilities = Array.Empty<ComputedVisibilityData>()
        };
    }

    private static ComicVisibilityResult CreateErrorResult(long comicId, string errorMessage)
    {
        return new ComicVisibilityResult
        {
            ComicId = comicId,
            Success = false,
            ErrorMessage = errorMessage,
            ComputationTime = DateTime.UtcNow,
            ComputedVisibilities = Array.Empty<ComputedVisibilityData>()
        };
    }

    public async Task<BulkVisibilityComputationResult> ComputeVisibilitiesBulkAsync(int startId, int limit)
    {
        const string operationName = "ComputeVisibilitiesBulkAsync";

        // Emit Request Wait Time: time from request receipt (middleware) to start of processing (includes task scheduling delay)
        var httpContext = _httpContextAccessor.HttpContext;
        if (httpContext?.Items[RequestWaitTimeMiddleware.RequestReceivedAtUtcKey] is DateTime requestReceivedAtUtc)
        {
            var waitSeconds = (DateTime.UtcNow - requestReceivedAtUtc).TotalSeconds;
            MetricsConfiguration.RequestWaitTimeSeconds.Observe(waitSeconds);
        }

        // Validate input parameters
        if (startId < 1) throw new ArgumentException("startId must be greater than 0", nameof(startId));
        if (limit < 1) throw new ArgumentException("limit must be greater than 0", nameof(limit));
        if (limit > 20) throw new ArgumentException("limit cannot exceed 20 comics", nameof(limit));

        return await _metricsReporter.TrackCompleteOperationAsync(operationName, async () =>
        {
            _logger.LogInformation($"Computing visibility for comics starting from ID {startId}, limit {limit}");

            // Track change tracker entities before query
            _metricsReporter.TrackChangeTracker(operationName);

            // Derive comic IDs in memory (same semantics as DOD: startId, startId+1, ... startId+limit-1)
            // Avoids one DB round-trip; non-existent IDs are handled as "not found" after batch fetch.
            var comicIds = Enumerable.Range(startId, limit).Select(i => (long)i).ToList();

            _logger.LogInformation($"Processing {comicIds.Count} comic IDs from {startId}");

            // Batch fetch: one query for all comics and related data (instead of N queries)
            var comicMap = await FetchComicsWithAllDataAsync(comicIds, operationName);

            if (comicMap.Count == 0)
            {
                throw new InvalidOperationException($"No comics found starting from ID {startId}");
            }

            var result = new List<ComicVisibilityResult>();
            var allVisibilities = new List<ComputedVisibilityData>();
            var processedCount = 0;
            var failedCount = 0;
            var startTime = DateTime.UtcNow;
            var computationTime = DateTime.UtcNow;

            // Process comics in memory (no per-comic DB calls)
            foreach (var comicId in comicIds)
            {
                if (!comicMap.TryGetValue(comicId, out var comic))
                {
                    result.Add(CreateNotFoundResult(comicId));
                    failedCount++;
                    continue;
                }

                var geographicRules = comic.GeographicRules;
                var customerSegmentRules = comic.CustomerSegmentRules;

                var computedVisibilities = _metricsReporter.TrackOperation(
                    "computation",
                    operationName,
                    () => ComputeVisibilities(
                        comic,
                        geographicRules,
                        customerSegmentRules,
                        computationTime));

                allVisibilities.AddRange(computedVisibilities);

                result.Add(new ComicVisibilityResult
                {
                    ComicId = comicId,
                    Success = computedVisibilities.Length > 0,
                    ComputationTime = computationTime,
                    ComputedVisibilities = computedVisibilities
                });

                if (computedVisibilities.Length > 0)
                    processedCount++;
                else
                    failedCount++;
            }

            // Batch save: one round-trip for all visibilities (instead of N saves)
            await SaveComputedVisibilitiesBulkAsync(allVisibilities, operationName);

            var endTime = DateTime.UtcNow;
            var duration = endTime - startTime;

            // Track change tracker entities after all operations (should be 0 with AsNoTracking)
            _metricsReporter.TrackChangeTracker(operationName);

            _logger.LogInformation($"Completed processing. Success: {processedCount}, Failed: {failedCount}, Duration: {duration.TotalSeconds}s");

            return new BulkVisibilityComputationResult
            {
                StartId = startId,
                Limit = limit,
                ProcessedSuccessfully = processedCount,
                Failed = failedCount,
                DurationInSeconds = duration.TotalSeconds,
                NextStartId = startId + limit,
                Results = result.ToArray()
            };
        });
    }

    public static ComputedVisibilityData[] ComputeVisibilities(
        ComicBook comic,
        List<GeographicRule> geographicRules,
        List<CustomerSegmentRule> customerSegmentRules,
        DateTime computationTime)
    {
        var computedVisibilities = new List<ComputedVisibilityData>();
        int freeChaptersCount = comic.Chapters.Count(c => c.IsFree);
        DateTime lastChapterReleaseTime = comic.Chapters.Max(c => c.ReleaseTime);
        string searchTags = string.Join(",", comic.ComicTags
            .Where(ct => ct.Tag != null)
            .Select(ct => ct.Tag!.Name));
        
        bool allChaptersFree = comic.Chapters.All(c => c.IsFree);
        bool hasAnyFreeChapter = comic.Chapters.Any(c => c.IsFree);
            
        foreach (var geoRule in geographicRules)
        {
            // Evaluate geographic visibility
            bool isGeographicVisible = geoRule.IsVisible
                && computationTime >= geoRule.LicenseStartDate
                && computationTime <= geoRule.LicenseEndDate
                && geoRule.LicenseType != LicenseType.NoAccess;
            
            if (!isGeographicVisible)
                continue;
            
            foreach (var segmentRule in customerSegmentRules)
            {
                // Check segment visibility
                bool isSegmentVisible = segmentRule.IsVisible && segmentRule.Segment?.IsActive == true;
                
                if (!isSegmentVisible)
                    continue;

                // Create visibility object (both geographic and segment checks passed)
                {
                    // Get regional pricing for this country
                    var pricing = comic.RegionalPricing
                        .FirstOrDefault(p => geoRule.CountryCodes.Contains(p.RegionCode));

                    // Calculate current price
                    decimal currentPrice = 0m;
                    bool isFreeContent = false;
                    bool isPremiumContent = false;
                    
                    if (pricing != null)
                    {
                        isFreeContent = pricing.IsFreeContent;
                        isPremiumContent = pricing.IsPremiumContent;
                        
                        if (isFreeContent)
                        {
                            currentPrice = 0m;
                        }
                        else
                        {
                            var now = DateTime.UtcNow;
                            if (pricing.DiscountStartDate.HasValue && pricing.DiscountEndDate.HasValue &&
                                now >= pricing.DiscountStartDate.Value && now <= pricing.DiscountEndDate.Value &&
                                pricing.DiscountPercentage.HasValue)
                            {
                                currentPrice = pricing.BasePrice * (1m - pricing.DiscountPercentage.Value / 100m);
                            }
                            else
                            {
                                currentPrice = pricing.BasePrice;
                            }
                        }
                    }

                    var visibility = new ComputedVisibilityData
                    {
                        ComicId = comic.Id,
                        CountryCode = geoRule.CountryCodes.FirstOrDefault() ?? string.Empty,
                        CustomerSegmentId = segmentRule.SegmentId,
                        FreeChaptersCount = freeChaptersCount,
                        LastChapterReleaseTime = lastChapterReleaseTime,
                        GenreId = comic.GenreId,
                        PublisherId = comic.PublisherId,
                        AverageRating = comic.AverageRating,
                        SearchTags = searchTags,
                        IsVisible = true,
                        ComputedAt = computationTime,
                        LicenseType = geoRule.LicenseType,
                        CurrentPrice = currentPrice,
                        IsFreeContent = isFreeContent,
                        IsPremiumContent = isPremiumContent,
                        AgeRating = comic.ContentRating?.AgeRating ?? AgeRating.AllAges,
                        ContentFlags = ContentFlagService.DetermineContentFlags(
                            comic.ContentRating?.ContentFlags ?? ContentFlag.None,
                            allChaptersFree,
                            hasAnyFreeChapter,
                            pricing),
                        ContentWarning = comic.ContentRating?.ContentWarning ?? string.Empty
                    };
                    computedVisibilities.Add(visibility);
                }
            }
        }

        return computedVisibilities.ToArray();
    }
}

public class BulkVisibilityComputationResult
{
    public int StartId { get; set; }
    public int Limit { get; set; }
    public int ProcessedSuccessfully { get; set; }
    public int Failed { get; set; }
    public double DurationInSeconds { get; set; }
    public int NextStartId { get; set; }
    public ComicVisibilityResult[] Results { get; set; } = Array.Empty<ComicVisibilityResult>();
}
