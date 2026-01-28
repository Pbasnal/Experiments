using ComicApiOop.Data;
using ComicApiOop.Models;
using Microsoft.EntityFrameworkCore;

namespace ComicApiOop.Services;

public class VisibilityComputationService
{
    private readonly ComicDbContext _dbContext;
    private readonly ILogger<VisibilityComputationService> _logger;

    public VisibilityComputationService(ComicDbContext dbContext, ILogger<VisibilityComputationService> logger)
    {
        _dbContext = dbContext;
        _logger = logger;
    }

    public async Task<VisibilityComputationResultDto> ComputeVisibilityForComicAsync(long comicId)
    {
        try
        {
            _logger.LogInformation($"Processing comic ID: {comicId}");

            // Fetch comic with all required data
            ComicBook? comic = await _dbContext.Comics
                .Include(c => c.Chapters)
                .Include(c => c.Tags)
                .Include(c => c.Publisher)
                .Include(c => c.Genre)
                .Include(c => c.ContentRating)
                .Include(c => c.RegionalPricing)
                .FirstOrDefaultAsync(c => c.Id == comicId);

            if (comic == null)
            {
                _logger.LogWarning($"Comic ID {comicId} not found");
                return new VisibilityComputationResultDto
                {
                    ComicId = comicId,
                    Success = false,
                    ErrorMessage = "Comic not found",
                    ComputationTime = DateTime.UtcNow,
                    ComputedVisibilities = new List<ComputedVisibilityDto>()
                };
            }

            var geographicRules = await _dbContext.GeographicRules
                .Where(r => r.ComicId == comicId)
                .ToListAsync();

            var customerSegmentRules = await _dbContext.CustomerSegmentRules
                .Include(r => r.Segment)
                .Where(r => r.ComicId == comicId)
                .ToListAsync();

            // Compute visibilities
            var computedVisibilities = new List<ComputedVisibilityDto>();
            foreach (var geoRule in geographicRules)
            {
                foreach (var segmentRule in customerSegmentRules)
                {
                    // First check if the comic is visible according to the rules
                    bool isVisible = geoRule.EvaluateVisibility() && segmentRule.IsVisible;

                    // Only create visibility object if the comic is visible
                    if (isVisible)
                    {
                        // Get regional pricing for this country
                        var pricing = comic.RegionalPricing
                            .FirstOrDefault(p => geoRule.CountryCodes.Contains(p.RegionCode));

                        var visibility = new ComputedVisibilityDto
                        {
                            ComicId = comicId,
                            CountryCode = geoRule.CountryCodes.First(),
                            CustomerSegmentId = segmentRule.SegmentId,
                            FreeChaptersCount = comic.Chapters.Count(c => c.IsFree),
                            LastChapterReleaseTime = comic.Chapters.Max(c => c.ReleaseTime),
                            GenreId = comic.GenreId,
                            PublisherId = comic.PublisherId,
                            AverageRating = comic.AverageRating,
                            SearchTags = string.Join(",", comic.Tags.Select(t => t.Name)),
                            IsVisible = true,
                            ComputedAt = DateTime.UtcNow,
                            LicenseType = geoRule.LicenseType,
                            CurrentPrice = pricing?.GetCurrentPrice() ?? 0m,
                            IsFreeContent = pricing?.IsFreeContent ?? false,
                            IsPremiumContent = pricing?.IsPremiumContent ?? false,
                            AgeRating = comic.ContentRating?.AgeRating ?? AgeRating.AllAges,
                            ContentFlags = ContentFlagService.DetermineContentFlags(
                                comic.ContentRating?.ContentFlags ?? ContentFlag.None,
                                comic.Chapters,
                                pricing),
                            ContentWarning = comic.ContentRating?.ContentWarning ?? string.Empty
                        };
                        computedVisibilities.Add(visibility);
                    }
                }
            }

            // Save computed visibilities to database
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
            await _dbContext.SaveChangesAsync();

            return new VisibilityComputationResultDto
            {
                ComicId = comicId,
                Success = true,
                ComputationTime = DateTime.UtcNow,
                ComputedVisibilities = computedVisibilities
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, $"Error processing comic {comicId}");
            return new VisibilityComputationResultDto
            {
                ComicId = comicId,
                Success = false,
                ErrorMessage = ex.Message,
                ComputationTime = DateTime.UtcNow,
                ComputedVisibilities = new List<ComputedVisibilityDto>()
            };
        }
    }

    public async Task<BulkVisibilityComputationResult> ComputeVisibilitiesBulkAsync(int startId, int limit)
    {
        // Validate input parameters
        if (startId < 1) throw new ArgumentException("startId must be greater than 0", nameof(startId));
        if (limit < 1) throw new ArgumentException("limit must be greater than 0", nameof(limit));
        if (limit > 20) throw new ArgumentException("limit cannot exceed 20 comics", nameof(limit));

        _logger.LogInformation($"Computing visibility for comics starting from ID {startId}, limit {limit}");

        // Get requested comic IDs
        var comicIds = await _dbContext.Comics
            .Where(c => c.Id >= startId)
            .OrderBy(c => c.Id)
            .Take(limit)
            .Select(c => c.Id)
            .ToListAsync();

        if (!comicIds.Any())
        {
            throw new InvalidOperationException($"No comics found starting from ID {startId}");
        }

        _logger.LogInformation($"Found {comicIds.Count} comics to process");

        var result = new List<VisibilityComputationResultDto>();
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

        _logger.LogInformation($"Completed processing. Success: {processedCount}, Failed: {failedCount}, Duration: {duration.TotalSeconds}s");

        return new BulkVisibilityComputationResult
        {
            StartId = startId,
            Limit = limit,
            ProcessedSuccessfully = processedCount,
            Failed = failedCount,
            DurationInSeconds = duration.TotalSeconds,
            NextStartId = startId + limit,
            Results = result
        };
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
    public List<VisibilityComputationResultDto> Results { get; set; } = new();
}
