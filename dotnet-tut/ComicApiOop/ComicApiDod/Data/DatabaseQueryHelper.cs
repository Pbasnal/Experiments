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
    /// </summary>
    /// <param name="db">Database context</param>
    /// <param name="comicId">ID of the comic to fetch data for</param>
    /// <returns>Batch data containing all relevant information for visibility computation</returns>
    public static async Task<ComicBatchData> GetComicBatchDataAsync(ComicDbContext db, long comicId)
    {
        // Fetch comic details
        var comicEntity = await db.Comics
            .Where(c => c.Id == comicId)
            .Select(c => new ComicBookData
            {
                Id = c.Id,
                Title = c.Title,
                PublisherId = c.PublisherId,
                GenreId = c.GenreId,
                ThemeId = c.ThemeId,
                TotalChapters = c.TotalChapters,
                LastUpdateTime = c.LastUpdateTime,
                AverageRating = c.AverageRating
            })
            .FirstOrDefaultAsync();

        if (comicEntity == null)
        {
            throw new InvalidOperationException($"Comic with ID {comicId} not found.");
        }

        // Fetch chapters
        var chapters = await db.Chapters
            .Where(ch => ch.ComicId == comicId)
            .Select(ch => new ChapterData
            {
                Id = ch.Id,
                ComicId = ch.ComicId,
                ChapterNumber = ch.ChapterNumber,
                ReleaseTime = ch.ReleaseTime,
                IsFree = ch.IsFree
            })
            .ToArrayAsync();

        // Fetch tags
        var tags = await db.Tags
            .Where(t => t.Id == comicId)
            .Select(t => new TagData
            {
                Id = t.Id,
                Name = t.Name
            })
            .ToArrayAsync();

        // Fetch content rating
        var contentRating = await db.ContentRatings
            .Where(cr => cr.ComicId == comicId)
            .Select(cr => new ContentRatingData
            {
                Id = cr.Id,
                ComicId = cr.ComicId,
                AgeRating = cr.AgeRating,
                ContentFlags = cr.ContentFlags,
                ContentWarning = cr.ContentWarning,
                RequiresParentalGuidance = cr.RequiresParentalGuidance
            })
            .FirstOrDefaultAsync();

        // Fetch pricing
        var regionalPricing = await db.ComicPricings
            .Where(p => p.ComicId == comicId)
            .Select(p => new PricingData
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
            })
            .ToArrayAsync();

        // Fetch geographic rules
        var geographicRules = await db.GeographicRules
            .Where(gr => gr.ComicId == comicId)
            .Select(gr => new GeographicRuleData
            {
                Id = gr.Id,
                ComicId = gr.ComicId,
                CountryCodes = gr.CountryCodes.ToArray(),
                LicenseStartDate = gr.LicenseStartDate,
                LicenseEndDate = gr.LicenseEndDate,
                LicenseType = gr.LicenseType,
                IsVisible = gr.IsVisible,
                LastUpdated = gr.LastUpdated
            })
            .ToArrayAsync();

        // Fetch customer segment rules
        var segmentRules = await db.CustomerSegmentRules
            .Where(csr => csr.ComicId == comicId)
            .Select(csr => new CustomerSegmentRuleData
            {
                Id = csr.Id,
                ComicId = csr.ComicId,
                SegmentId = csr.SegmentId,
                IsVisible = csr.IsVisible,
                LastUpdated = csr.LastUpdated
            })
            .ToArrayAsync();

        // Fetch customer segments
        var segments = await db.CustomerSegments
            .Where(cs => segmentRules.Select(sr => sr.SegmentId).Contains(cs.Id))
            .Select(cs => new CustomerSegmentData
            {
                Id = cs.Id,
                Name = cs.Name,
                IsPremium = cs.IsPremium,
                IsActive = cs.IsActive
            })
            .ToArrayAsync();

        // Construct and return batch data
        return new ComicBatchData
        {
            ComicId = comicId,
            Comic = comicEntity,
            Chapters = chapters,
            Tags = tags,
            ContentRating = contentRating,
            RegionalPricing = regionalPricing,
            GeographicRules = geographicRules,
            SegmentRules = segmentRules,
            Segments = segments
        };
    }

    /// <summary>
    /// Saves computed visibility data to the database in a batch
    /// </summary>
    /// <param name="db">Database context</param>
    /// <param name="computedVisibilities">Array of computed visibility data to save</param>
    public static async Task SaveComputedVisibilitiesAsync(ComicDbContext db, ComputedVisibilityData[] computedVisibilities)
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
