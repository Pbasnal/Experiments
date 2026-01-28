using Common.Models;

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

    public static bool EvaluateGeographicVisibility(GeographicRule rule, DateTime currentTime)
    {
        return rule.IsVisible
               && currentTime >= rule.LicenseStartDate
               && currentTime <= rule.LicenseEndDate
               && rule.LicenseType != LicenseType.NoAccess;
    }

    /// <summary>
    /// Evaluate if a segment rule allows visibility
    /// </summary>
    public static bool EvaluateSegmentVisibility(CustomerSegmentRule segment)
    {
        return segment.IsVisible && segment.Segment.IsActive;
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

    public static ComputedVisibilityData[] ComputeVisibilities(
        ComicBook comicBook,
        DateTime computationTime)
    {
        var results = new List<ComputedVisibilityData>();
        int freeChapterCount = comicBook.Chapters.Count(c => c.IsFree);
        DateTime lastChapterReleaseTime = comicBook.Chapters.Max(c => c.ReleaseTime);
        string searchTags = string.Join(",", comicBook.ComicTags.Select(t => t.Tag.Name));

        // Process all combinations of geographic and segment rules
        foreach (var geoRule in comicBook.GeographicRules)
        {
            // Check geographic visibility
            if (!EvaluateGeographicVisibility(geoRule, computationTime))
                continue;

            foreach (CustomerSegmentRule segmentRule in comicBook.CustomerSegmentRules)
            {
                // Check segment visibility
                if (!EvaluateSegmentVisibility(segmentRule))
                    continue;

                // Get regional pricing
                ComicPricing? pricing = comicBook.RegionalPricing
                    .FirstOrDefault(p => geoRule.CountryCodes.Contains(p.RegionCode));

                bool allChaptersFree = comicBook.Chapters.Count == freeChapterCount;
                bool hasAnyFreeChapter = freeChapterCount > 0;

                ContentFlag contentFlags = DetermineContentFlags(
                    comicBook.ContentRating?.ContentFlags ?? ContentFlag.None,
                    allChaptersFree, hasAnyFreeChapter,
                    pricing);

                // Create visibility data
                var visibility = new ComputedVisibilityData
                {
                    ComicId = comicBook.Id,
                    CountryCode = geoRule.CountryCodes.FirstOrDefault() ?? string.Empty,
                    CustomerSegmentId = segmentRule.SegmentId,
                    FreeChaptersCount = freeChapterCount,
                    LastChapterReleaseTime = lastChapterReleaseTime,
                    GenreId = comicBook.GenreId,
                    PublisherId = comicBook.PublisherId,
                    AverageRating = comicBook.AverageRating,
                    SearchTags = searchTags,
                    IsVisible = true,
                    ComputedAt = computationTime,
                    LicenseType = geoRule.LicenseType,
                    CurrentPrice = pricing?.BasePrice ?? 0m,
                    IsFreeContent = pricing?.IsFreeContent ?? false,
                    IsPremiumContent = pricing?.IsPremiumContent ?? false,
                    AgeRating = comicBook.ContentRating?.AgeRating ?? AgeRating.AllAges,
                    ContentFlags = contentFlags,
                    ContentWarning = comicBook.ContentRating?.ContentWarning ?? string.Empty
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

    public static ContentFlag DetermineContentFlags(
        ContentFlag baseFlags,
        bool allChaptersFree,
        bool hasAnyFreeChapter,
        ComicPricing? pricing)
    {
        var flags = baseFlags;

        // Add Free flag if all chapters are free
        if (allChaptersFree)
        {
            flags |= ContentFlag.Free;
        }

        // Add Premium flag if no free chapters
        if (!hasAnyFreeChapter)
        {
            flags |= ContentFlag.Premium;
        }

        // Add Freemium flag if the comic has both free and paid chapters or has a price
        if ((hasAnyFreeChapter && !allChaptersFree) || (pricing?.BasePrice > 0))
        {
            flags |= ContentFlag.Freemium;
        }

        return flags;
    }
}