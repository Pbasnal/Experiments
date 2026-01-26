using ComicApiDod.SimpleQueue;

namespace ComicApiDod.Models;

public class ComicBookData
{
    public long Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public long PublisherId { get; set; }
    public long GenreId { get; set; }
    public long ThemeId { get; set; }
    public int TotalChapters { get; set; }
    public DateTime LastUpdateTime { get; set; }
    public double AverageRating { get; set; }
}

public readonly struct ChapterData
{
    public readonly long Id { get; init; }
    public readonly long ComicId { get; init; }
    public readonly int ChapterNumber { get; init; }
    public readonly DateTime ReleaseTime { get; init; }
    public readonly bool IsFree { get; init; }
}

public class PublisherData
{
    public long Id { get; set; }
    public string Name { get; set; } = string.Empty;
}

public class GenreData
{
    public long Id { get; set; }
    public string Name { get; set; } = string.Empty;
}

public class ThemeData
{
    public long Id { get; set; }
    public string Name { get; set; } = string.Empty;
}

public class TagData
{
    public long Id { get; set; }
    public string Name { get; set; } = string.Empty;
}

public readonly struct ContentRatingData
{
    public readonly long Id { get; init; }
    public readonly long ComicId { get; init; }
    public readonly AgeRating AgeRating { get; init; }
    public readonly ContentFlag ContentFlags { get; init; }
    public readonly string ContentWarning { get; init; }
    public readonly bool RequiresParentalGuidance { get; init; }
}

public readonly struct PricingData
{
    public readonly long Id { get; init; }
    public readonly long ComicId { get; init; }
    public readonly string RegionCode { get; init; }
    public readonly decimal BasePrice { get; init; }
    public readonly bool IsFreeContent { get; init; }
    public readonly bool IsPremiumContent { get; init; }
    public readonly DateTime? DiscountStartDate { get; init; }
    public readonly DateTime? DiscountEndDate { get; init; }
    public readonly decimal? DiscountPercentage { get; init; }
}

public readonly struct GeographicRuleData
{
    public readonly long Id { get; init; }
    public readonly long ComicId { get; init; }
    public readonly string[] CountryCodes { get; init; }
    public readonly DateTime LicenseStartDate { get; init; }
    public readonly DateTime LicenseEndDate { get; init; }
    public readonly LicenseType LicenseType { get; init; }
    public readonly bool IsVisible { get; init; }
    public readonly DateTime LastUpdated { get; init; }
}

public readonly struct CustomerSegmentData
{
    public readonly long Id { get; init; }
    public readonly string Name { get; init; }
    public readonly bool IsPremium { get; init; }
    public readonly bool IsActive { get; init; }
}

public readonly struct CustomerSegmentRuleData
{
    public readonly long Id { get; init; }
    public readonly long ComicId { get; init; }
    public readonly long SegmentId { get; init; }
    public readonly bool IsVisible { get; init; }
    public readonly DateTime LastUpdated { get; init; }
}

public readonly struct ComputedVisibilityData
{
    public readonly long ComicId { get; init; }
    public readonly string CountryCode { get; init; }
    public readonly long CustomerSegmentId { get; init; }
    public readonly int FreeChaptersCount { get; init; }
    public readonly DateTime LastChapterReleaseTime { get; init; }
    public readonly long GenreId { get; init; }
    public readonly long PublisherId { get; init; }
    public readonly double AverageRating { get; init; }
    public readonly string SearchTags { get; init; }
    public readonly bool IsVisible { get; init; }
    public readonly DateTime ComputedAt { get; init; }
    public readonly LicenseType LicenseType { get; init; }
    public readonly decimal CurrentPrice { get; init; }
    public readonly bool IsFreeContent { get; init; }
    public readonly bool IsPremiumContent { get; init; }
    public readonly AgeRating AgeRating { get; init; }
    public readonly ContentFlag ContentFlags { get; init; }
    public readonly string ContentWarning { get; init; }
}

public class ComicBatchData
{
    public long ComicId { get; set; }
    public ComicBookData Comic { get; set; } = new();
    public ChapterData[] Chapters { get; set; } = Array.Empty<ChapterData>();
    public TagData[] Tags { get; set; } = Array.Empty<TagData>();
    public ContentRatingData? ContentRating { get; set; }
    public PricingData[] RegionalPricing { get; set; } = Array.Empty<PricingData>();
    public GeographicRuleData[] GeographicRules { get; set; } = Array.Empty<GeographicRuleData>();
    public CustomerSegmentRuleData[] SegmentRules { get; set; } = Array.Empty<CustomerSegmentRuleData>();
    public CustomerSegmentData[] Segments { get; set; } = Array.Empty<CustomerSegmentData>();
}

public class VisibilityComputationRequest : IValue 
{
    public long StartId { get; set; }
    public int Limit { get; set; }

    public int Id { get; set; }
    public TaskCompletionSource<VisibilityComputationResponse> ResponseSrc { get; set; }

    public VisibilityComputationRequest(long startId, int limit)
    {
        StartId = startId;
        Limit = limit;
        Id = Guid.NewGuid().GetHashCode();
        ResponseSrc = new TaskCompletionSource<VisibilityComputationResponse>();
    }

    public override string ToString()
    {
        return $"[{Id}] {StartId} -> {Limit}";
    }
}

public class VisibilityComputationResponse : IValue
{
    public long StartId { get; set; }
    public int Limit { get; set; }
    public int ProcessedSuccessfully { get; set; }
    public int Failed { get; set; }
    public double DurationInSeconds { get; set; }
    public long NextStartId { get; set; }
    public ComicVisibilityResult[] Results { get; set; } = Array.Empty<ComicVisibilityResult>();

    public int Id { get; set; }
}

public class ComicVisibilityResult
{
    public long ComicId { get; set; }
    public bool Success { get; set; }
    public string? ErrorMessage { get; set; }
    public DateTime ComputationTime { get; set; }
    public ComputedVisibilityData[] ComputedVisibilities { get; set; } = Array.Empty<ComputedVisibilityData>();
}