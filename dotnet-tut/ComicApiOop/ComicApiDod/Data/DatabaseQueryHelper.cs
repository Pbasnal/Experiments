using System.Collections.Concurrent;
using System.Collections.Immutable;
using ComicApiDod.Models;
using Microsoft.EntityFrameworkCore;

namespace ComicApiDod.Data;

/// <summary>
/// Helper class for batch database queries in the Data-Oriented Design approach
/// Provides methods to fetch data in batches for efficient processing
/// </summary>
public static class DatabaseQueryHelper
{
    /// <summary>
    /// Retrieves a batch of comic IDs starting from a given ID
    /// </summary>
    /// <param name="db">Database context</param>
    /// <param name="startId">Starting comic ID</param>
    /// <param name="limit">Maximum number of IDs to retrieve</param>
    /// <returns>Array of comic IDs</returns>
    public static async Task<long[]> GetComicIdsAsync(ComicDbContext db, int startId, int limit)
    {
        return await db.Comics
            .Where(c => c.Id >= startId)
            .OrderBy(c => c.Id)
            .Take(limit)
            .Select(c => c.Id)
            .ToArrayAsync();
    }

    /// <summary>
    /// Retrieves all batch data for a specific comic to compute its visibility
    /// Uses Include() for efficient data loading
    /// </summary>
    /// <param name="db">Database context</param>
    /// <param name="comicId">ID of the comic to fetch data for</param>
    /// <returns>Batch data containing all relevant information for visibility computation</returns>
    public static async Task<ComicBatchData> GetComicBatchDataAsync(ComicDbContext db, long comicId)
    {
        // Use Include() to fetch all related data efficiently (like OOP version)
        var comic = await db.Comics
            .Where(c => c.Id == comicId)
            .Include(c => c.Chapters)
            .Include(c => c.ContentRating)
            .Include(c => c.RegionalPricing)
            .Include(c => c.GeographicRules)
            .Include(c => c.CustomerSegmentRules)
                .ThenInclude(csr => csr.Segment)
            .Include(c => c.ComicTags)
                .ThenInclude(ct => ct.Tag)
            .FirstOrDefaultAsync();

        if (comic == null)
        {
            throw new InvalidOperationException($"Comic with ID {comicId} not found.");
        }

        // Get segment IDs
        var segmentIds = comic.CustomerSegmentRules.Select(csr => csr.SegmentId).Distinct().ToList();

        // Fetch segments
        var segments = await db.CustomerSegments
            .Where(cs => segmentIds.Contains(cs.Id))
            .Select(cs => new CustomerSegmentData
            {
                Id = cs.Id,
                Name = cs.Name,
                IsPremium = cs.IsPremium,
                IsActive = cs.IsActive
            })
            .ToArrayAsync();

        var segmentsDict = segments.ToDictionary(s => s.Id, s => s);

        // Project EF entities to DOD data structures
        return new ComicBatchData
        {
            ComicId = comicId,
            Comic = new ComicBookData
            {
                Id = comic.Id,
                Title = comic.Title,
                PublisherId = comic.PublisherId,
                GenreId = comic.GenreId,
                ThemeId = comic.ThemeId,
                TotalChapters = comic.TotalChapters,
                LastUpdateTime = comic.LastUpdateTime,
                AverageRating = comic.AverageRating
            },
            Chapters = comic.Chapters.Select(ch => new ChapterData
            {
                Id = ch.Id,
                ComicId = ch.ComicId,
                ChapterNumber = ch.ChapterNumber,
                ReleaseTime = ch.ReleaseTime,
                IsFree = ch.IsFree
            }).ToArray(),
            Tags = comic.ComicTags
                .Where(ct => ct.Tag != null)
                .Select(ct => new TagData
                {
                    Id = ct.ComicsId,
                    Name = ct.Tag!.Name
                })
                .ToArray(),
            ContentRating = comic.ContentRating != null
                ? new ContentRatingData
                {
                    Id = comic.ContentRating.Id,
                    ComicId = comic.ContentRating.ComicId,
                    AgeRating = comic.ContentRating.AgeRating,
                    ContentFlags = comic.ContentRating.ContentFlags,
                    ContentWarning = comic.ContentRating.ContentWarning,
                    RequiresParentalGuidance = comic.ContentRating.RequiresParentalGuidance
                }
                : null,
            RegionalPricing = comic.RegionalPricing.Select(p => new PricingData
            {
                Id = p.Id,
                ComicId = p.ComicId,
                RegionCode = p.RegionCode,
                BasePrice = p.BasePrice,
                IsFreeContent = p.IsFreeContent,
                IsPremiumContent = p.IsPremiumContent,
                DiscountStartDate = p.DiscountStartDate,
                DiscountEndDate = p.DiscountEndDate,
                DiscountPercentage = p.DiscountPercentage ?? 0
            }).ToArray(),
            GeographicRules = comic.GeographicRules.Select(gr => new GeographicRuleData
            {
                Id = gr.Id,
                ComicId = gr.ComicId,
                CountryCodes = gr.CountryCodes.ToArray(),
                LicenseStartDate = gr.LicenseStartDate,
                LicenseEndDate = gr.LicenseEndDate,
                LicenseType = gr.LicenseType,
                IsVisible = gr.IsVisible,
                LastUpdated = gr.LastUpdated
            }).ToArray(),
            SegmentRules = comic.CustomerSegmentRules.Select(csr => new CustomerSegmentRuleData
            {
                Id = csr.Id,
                ComicId = csr.ComicId,
                SegmentId = csr.SegmentId,
                IsVisible = csr.IsVisible,
                LastUpdated = csr.LastUpdated
            }).ToArray(),
            Segments = comic.CustomerSegmentRules
                .Select(csr => csr.SegmentId)
                .Where(sid => segmentsDict.ContainsKey(sid))
                .Select(sid => segmentsDict[sid])
                .ToArray()
        };
    }

    public static async Task<IDictionary<long, ComicBatchData>> GetComicBatchDataAsync(ComicDbContext db,
        long[] comicIds)
    {
        ISet<long> sortedComicIds = new SortedSet<long>(comicIds);
        
        // Use Include() to fetch all related data in fewer queries (like OOP version)
        var comics = await db.Comics
            .Where(c => sortedComicIds.Contains(c.Id))
            .Include(c => c.Chapters)
            .Include(c => c.ContentRating)
            .Include(c => c.RegionalPricing)
            .Include(c => c.GeographicRules)
            .Include(c => c.CustomerSegmentRules)
                .ThenInclude(csr => csr.Segment)
            .Include(c => c.ComicTags)
                .ThenInclude(ct => ct.Tag)
            .ToListAsync();

        if (comics == null || comics.Count == 0)
        {
            throw new InvalidOperationException($"No comics found with IDs: {string.Join(", ", comicIds)}");
        }

        // Get all unique segment IDs from the loaded data
        var segmentIds = comics
            .SelectMany(c => c.CustomerSegmentRules)
            .Select(csr => csr.SegmentId)
            .Distinct()
            .ToImmutableSortedSet();

        // Fetch segments (if not already loaded via Include)
        IDictionary<long, CustomerSegmentData> segments = await db.CustomerSegments
            .Where(cs => segmentIds.Contains(cs.Id))
            .Select(cs => new CustomerSegmentData
            {
                Id = cs.Id,
                Name = cs.Name,
                IsPremium = cs.IsPremium,
                IsActive = cs.IsActive
            })
            .ToDictionaryAsync(c => c.Id, c => c);

        // Project EF entities to DOD data structures
        IDictionary<long, ComicBatchData> comicBatchData = new Dictionary<long, ComicBatchData>();
        
        foreach (var comic in comics)
        {
            // Project chapters
            var chapters = comic.Chapters.Select(ch => new ChapterData
            {
                Id = ch.Id,
                ComicId = ch.ComicId,
                ChapterNumber = ch.ChapterNumber,
                ReleaseTime = ch.ReleaseTime,
                IsFree = ch.IsFree
            }).ToArray();

            // Project tags
            var tags = comic.ComicTags
                .Where(ct => ct.Tag != null)
                .Select(ct => new TagData
                {
                    Id = ct.ComicsId,
                    Name = ct.Tag!.Name
                })
                .ToArray();

            // Project content rating
            ContentRatingData? contentRating = comic.ContentRating != null
                ? new ContentRatingData
                {
                    Id = comic.ContentRating.Id,
                    ComicId = comic.ContentRating.ComicId,
                    AgeRating = comic.ContentRating.AgeRating,
                    ContentFlags = comic.ContentRating.ContentFlags,
                    ContentWarning = comic.ContentRating.ContentWarning,
                    RequiresParentalGuidance = comic.ContentRating.RequiresParentalGuidance
                }
                : null;

            // Project pricing
            var regionalPricing = comic.RegionalPricing.Select(p => new PricingData
            {
                Id = p.Id,
                ComicId = p.ComicId,
                RegionCode = p.RegionCode,
                BasePrice = p.BasePrice,
                IsFreeContent = p.IsFreeContent,
                IsPremiumContent = p.IsPremiumContent,
                DiscountStartDate = p.DiscountStartDate,
                DiscountEndDate = p.DiscountEndDate,
                DiscountPercentage = p.DiscountPercentage ?? 0
            }).ToArray();

            // Project geographic rules
            var geographicRules = comic.GeographicRules.Select(gr => new GeographicRuleData
            {
                Id = gr.Id,
                ComicId = gr.ComicId,
                CountryCodes = gr.CountryCodes.ToArray(),
                LicenseStartDate = gr.LicenseStartDate,
                LicenseEndDate = gr.LicenseEndDate,
                LicenseType = gr.LicenseType,
                IsVisible = gr.IsVisible,
                LastUpdated = gr.LastUpdated
            }).ToArray();

            // Project segment rules
            var segmentRules = comic.CustomerSegmentRules.Select(csr => new CustomerSegmentRuleData
            {
                Id = csr.Id,
                ComicId = csr.ComicId,
                SegmentId = csr.SegmentId,
                IsVisible = csr.IsVisible,
                LastUpdated = csr.LastUpdated
            }).ToArray();

            // Get segments for this comic
            var comicSegments = segmentRules
                .Select(sr => sr.SegmentId)
                .Where(sid => segments.ContainsKey(sid))
                .Select(sid => segments[sid])
                .ToArray();

            comicBatchData.Add(comic.Id, new ComicBatchData
            {
                ComicId = comic.Id,
                Comic = new ComicBookData
                {
                    Id = comic.Id,
                    Title = comic.Title,
                    PublisherId = comic.PublisherId,
                    GenreId = comic.GenreId,
                    ThemeId = comic.ThemeId,
                    TotalChapters = comic.TotalChapters,
                    LastUpdateTime = comic.LastUpdateTime,
                    AverageRating = comic.AverageRating
                },
                Chapters = chapters,
                Tags = tags,
                ContentRating = contentRating,
                RegionalPricing = regionalPricing,
                GeographicRules = geographicRules,
                SegmentRules = segmentRules,
                Segments = comicSegments
            });
        }

        return comicBatchData;
    }

    /// <summary>
    /// 
    /// Saves computed visibility data to the database in a batch
    /// </summary>
    /// <param name="db">Database context</param>
    /// <param name="computedVisibilities">Array of computed visibility data to save</param>
    public static async Task SaveComputedVisibilitiesAsync(ComicDbContext db,
        ComputedVisibilityData[] computedVisibilities)
    {
        // Convert or map ComputedVisibilityData to ComputedVisibility entity
        var entities = computedVisibilities.Select(cv => new ComputedVisibility
        {
            ComicId = cv.ComicId,
            CountryCode = cv.CountryCode ?? string.Empty,
            CustomerSegmentId = cv.CustomerSegmentId,
            FreeChaptersCount = cv.FreeChaptersCount,
            LastChapterReleaseTime = cv.LastChapterReleaseTime,
            GenreId = cv.GenreId,
            PublisherId = cv.PublisherId,
            AverageRating = cv.AverageRating,
            SearchTags = cv.SearchTags ?? string.Empty,
            IsVisible = cv.IsVisible,
            ComputedAt = cv.ComputedAt,
            LicenseType = cv.LicenseType,
            CurrentPrice = cv.CurrentPrice,
            IsFreeContent = cv.IsFreeContent,
            IsPremiumContent = cv.IsPremiumContent,
            AgeRating = cv.AgeRating,
            ContentFlags = cv.ContentFlags,
            ContentWarning = cv.ContentWarning ?? string.Empty
        }).ToList();

        // Add or update range
        db.ComputedVisibilities.AddRange(entities);
        await db.SaveChangesAsync();
    }
}