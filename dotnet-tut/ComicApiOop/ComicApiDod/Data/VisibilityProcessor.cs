using ComicApiDod.Models;

namespace ComicApiDod.Data;

/// <summary>
/// Pure functions for processing visibility data
/// Separated from data structures - this is the DOD way
/// </summary>
public static class VisibilityProcessor
{
    /// <summary>
    /// Evaluate if a geographic rule allows visibility
    /// </summary>
    public static bool EvaluateGeographicVisibility(in GeographicRuleData rule, DateTime currentTime)
    {
        return rule.IsVisible
               && currentTime >= rule.LicenseStartDate
               && currentTime <= rule.LicenseEndDate
               && rule.LicenseType != LicenseType.NoAccess;
    }

    /// <summary>
    /// Evaluate if a segment rule allows visibility
    /// </summary>
    public static bool EvaluateSegmentVisibility(
        in CustomerSegmentRuleData rule, 
        in CustomerSegmentData segment)
    {
        return rule.IsVisible && segment.IsActive;
    }

    /// <summary>
    /// Calculate current price from pricing data
    /// </summary>
    public static decimal CalculateCurrentPrice(in PricingData pricing, DateTime currentTime)
    {
        if (pricing.IsFreeContent)
            return 0m;

        if (pricing.DiscountStartDate.HasValue && 
            pricing.DiscountEndDate.HasValue &&
            currentTime >= pricing.DiscountStartDate.Value &&
            currentTime <= pricing.DiscountEndDate.Value &&
            pricing.DiscountPercentage.HasValue)
        {
            return pricing.BasePrice * (1m - pricing.DiscountPercentage.Value / 100m);
        }

        return pricing.BasePrice;
    }

    /// <summary>
    /// Determine content flags based on chapters and pricing
    /// </summary>
    public static ContentFlag DetermineContentFlags(
        ContentFlag baseFlags,
        ReadOnlySpan<ChapterData> chapters,
        in PricingData? pricing)
    {
        var flags = baseFlags;

        if (chapters.IsEmpty)
            return flags;

        // Count chapter types
        int freeCount = 0;
        int paidCount = 0;

        foreach (var chapter in chapters)
        {
            if (chapter.IsFree)
                freeCount++;
            else
                paidCount++;
        }

        bool allChaptersFree = freeCount == chapters.Length;
        bool hasAnyFreeChapter = freeCount > 0;
        bool hasPaidChapters = paidCount > 0;

        // Add Free flag if all chapters are free
        if (allChaptersFree)
            flags |= ContentFlag.Free;

        // Add Premium flag if no free chapters
        if (!hasAnyFreeChapter)
            flags |= ContentFlag.Premium;

        // Add Freemium flag if mixed or has pricing
        if ((hasAnyFreeChapter && hasPaidChapters) || (pricing?.BasePrice > 0))
            flags |= ContentFlag.Freemium;

        return flags;
    }

    /// <summary>
    /// Compute visibilities for a comic batch
    /// This is a pure function that takes data and returns computed results
    /// </summary>
    public static ComputedVisibilityData[] ComputeVisibilities(
        ComicBatchData batchData,
        DateTime computationTime)
    {
        var results = new List<ComputedVisibilityData>();

        // Process all combinations of geographic and segment rules
        foreach (var geoRule in batchData.GeographicRules)
        {
            // Check geographic visibility
            if (!EvaluateGeographicVisibility(geoRule, computationTime))
                continue;

            foreach (var segmentRule in batchData.SegmentRules)
            {
                // Find the segment data
                var segment = Array.Find(batchData.Segments, s => s.Id == segmentRule.SegmentId);
                if (segment.Id == 0) // struct default
                    continue;

                // Check segment visibility
                if (!EvaluateSegmentVisibility(segmentRule, segment))
                    continue;

                // Get regional pricing
                var pricing = Array.Find(
                    batchData.RegionalPricing,
                    p => geoRule.CountryCodes.Contains(p.RegionCode)
                );

                // Calculate derived values
                var freeChaptersCount = CountFreeChapters(batchData.Chapters);
                var lastChapterTime = GetLastChapterTime(batchData.Chapters);
                var searchTags = string.Join(",", batchData.Tags.Select(t => t.Name));
                var currentPrice = pricing.Id != 0 
                    ? CalculateCurrentPrice(pricing, computationTime) 
                    : 0m;

                var contentFlags = DetermineContentFlags(
                    batchData.ContentRating?.ContentFlags ?? ContentFlag.None,
                    batchData.Chapters,
                    pricing.Id != 0 ? pricing : null
                );

                // Create visibility data
                var visibility = new ComputedVisibilityData
                {
                    ComicId = batchData.ComicId,
                    CountryCode = geoRule.CountryCodes.FirstOrDefault() ?? string.Empty,
                    CustomerSegmentId = segmentRule.SegmentId,
                    FreeChaptersCount = freeChaptersCount,
                    LastChapterReleaseTime = lastChapterTime,
                    GenreId = batchData.Comic.GenreId,
                    PublisherId = batchData.Comic.PublisherId,
                    AverageRating = batchData.Comic.AverageRating,
                    SearchTags = searchTags,
                    IsVisible = true,
                    ComputedAt = computationTime,
                    LicenseType = geoRule.LicenseType,
                    CurrentPrice = currentPrice,
                    IsFreeContent = pricing.IsFreeContent,
                    IsPremiumContent = pricing.IsPremiumContent,
                    AgeRating = batchData.ContentRating?.AgeRating ?? AgeRating.AllAges,
                    ContentFlags = contentFlags,
                    ContentWarning = batchData.ContentRating?.ContentWarning ?? string.Empty
                };

                results.Add(visibility);
            }
        }

        return results.ToArray();
    }

    /// <summary>
    /// Count free chapters - using span for performance
    /// </summary>
    private static int CountFreeChapters(ReadOnlySpan<ChapterData> chapters)
    {
        int count = 0;
        foreach (var chapter in chapters)
        {
            if (chapter.IsFree)
                count++;
        }
        return count;
    }

    /// <summary>
    /// Get the last chapter release time
    /// </summary>
    private static DateTime GetLastChapterTime(ReadOnlySpan<ChapterData> chapters)
    {
        if (chapters.IsEmpty)
            return DateTime.MinValue;

        var maxTime = chapters[0].ReleaseTime;
        for (int i = 1; i < chapters.Length; i++)
        {
            if (chapters[i].ReleaseTime > maxTime)
                maxTime = chapters[i].ReleaseTime;
        }
        return maxTime;
    }
}




