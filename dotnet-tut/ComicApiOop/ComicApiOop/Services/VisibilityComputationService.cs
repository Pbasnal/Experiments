using Common.Models;
using ComicApiOop.Data;
using Microsoft.EntityFrameworkCore;

namespace ComicApiOop.Services;

public class VisibilityComputationService
{
    private readonly ComicDbContext _dbContext;
    private readonly ILogger<VisibilityComputationService> _logger;
    private readonly MetricsReporter _metricsReporter;

    public VisibilityComputationService(
        ComicDbContext dbContext, 
        ILogger<VisibilityComputationService> logger,
        MetricsReporter metricsReporter)
    {
        _dbContext = dbContext;
        _logger = logger;
        _metricsReporter = metricsReporter;
    }

    public async Task<ComicVisibilityResult> ComputeVisibilityForComicAsync(long comicId)
    {
        const string operationName = "ComputeVisibilityForComicAsync";
        
        return await _metricsReporter.TrackCompleteOperationAsync(operationName, async () =>
        {
            try
            {
                _logger.LogInformation($"Processing comic ID: {comicId}");

                // Track change tracker entities before queries
                _metricsReporter.TrackChangeTracker(operationName);

                // Fetch all required data in a single query
                var comic = await FetchComicWithAllDataAsync(comicId, operationName);
                if (comic == null)
                {
                    _logger.LogWarning($"Comic ID {comicId} not found");
                    return CreateNotFoundResult(comicId);
                }

                // Track change tracker entities after query (should be 0 with AsNoTracking)
                _metricsReporter.TrackChangeTracker(operationName);

                // Extract rules from the loaded comic
                var geographicRules = comic.GeographicRules;
                var customerSegmentRules = comic.CustomerSegmentRules;

                // Compute visibilities
                var computedVisibilities = _metricsReporter.TrackOperation(
                    "computation",
                    operationName,
                    () => ComputeVisibilities(
                        comic,
                        geographicRules,
                        customerSegmentRules,
                        DateTime.UtcNow));

                // Save computed visibilities
                await SaveComputedVisibilitiesAsync(computedVisibilities, operationName);

                return new ComicVisibilityResult
                {
                    ComicId = comicId,
                    Success = computedVisibilities.Length > 0,
                    ComputationTime = DateTime.UtcNow,
                    ComputedVisibilities = computedVisibilities
                };
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error processing comic {comicId}");
                return CreateErrorResult(comicId, ex.Message);
            }
        });
    }

    private async Task<ComicBook?> FetchComicWithAllDataAsync(long comicId, string operationName)
    {
        return await _metricsReporter.TrackQueryAsync(
            "fetch_comic",
            operationName,
            async () => await _dbContext.Comics
                .Where(c => c.Id == comicId)
                .Include(c => c.Chapters)
                .Include(c => c.ContentRating)
                .Include(c => c.RegionalPricing)
                .Include(c => c.GeographicRules)
                .Include(c => c.CustomerSegmentRules)
                    .ThenInclude(csr => csr.Segment)
                .Include(c => c.ComicTags)
                    .ThenInclude(ct => ct.Tag)
                .AsNoTracking()
                .FirstOrDefaultAsync());
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
        
        // Validate input parameters
        if (startId < 1) throw new ArgumentException("startId must be greater than 0", nameof(startId));
        if (limit < 1) throw new ArgumentException("limit must be greater than 0", nameof(limit));
        if (limit > 20) throw new ArgumentException("limit cannot exceed 20 comics", nameof(limit));

        return await _metricsReporter.TrackCompleteOperationAsync(operationName, async () =>
        {
            _logger.LogInformation($"Computing visibility for comics starting from ID {startId}, limit {limit}");

            // Track change tracker entities before query
            _metricsReporter.TrackChangeTracker(operationName);

            // Get requested comic IDs
            var comicIds = await _metricsReporter.TrackQueryAsync(
                "fetch_comic_ids",
                operationName,
                async () => await _dbContext.Comics
                    .Where(c => c.Id >= startId)
                    .OrderBy(c => c.Id)
                    .Take(limit)
                    .AsNoTracking()
                    .Select(c => c.Id)
                    .ToListAsync());

            if (!comicIds.Any())
            {
                throw new InvalidOperationException($"No comics found starting from ID {startId}");
            }

            _logger.LogInformation($"Found {comicIds.Count} comics to process");

            var result = new List<ComicVisibilityResult>();
            var processedCount = 0;
            var failedCount = 0;
            var startTime = DateTime.UtcNow;

            // Process comics
            foreach (var comicId in comicIds)
            {
                var computationResult = await ComputeVisibilityForComicAsync(comicId);
                result.Add(computationResult);

                if (computationResult.Success)
                {
                    processedCount++;
                }
                else
                {
                    failedCount++;
                }
            }

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

    /// <summary>
    /// Computes visibilities for a comic given pre-loaded data.
    /// This method is pure computation logic and can be used for benchmarking.
    /// Uses Common.Models types for consistency with DOD version.
    /// </summary>
    /// <param name="comic">The comic book with all required navigation properties loaded</param>
    /// <param name="geographicRules">Geographic rules for the comic</param>
    /// <param name="customerSegmentRules">Customer segment rules for the comic</param>
    /// <param name="computationTime">The time to use for computation (usually DateTime.UtcNow)</param>
    /// <returns>Array of computed visibility data</returns>
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
