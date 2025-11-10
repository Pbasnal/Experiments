namespace ComicApiOop.Models;

public class ComputedVisibilityDto
{
    public long ComicId { get; set; }
    public string CountryCode { get; set; } = string.Empty;
    public long CustomerSegmentId { get; set; }
    public int FreeChaptersCount { get; set; }
    public DateTime LastChapterReleaseTime { get; set; }
    public long GenreId { get; set; }
    public long PublisherId { get; set; }
    public double AverageRating { get; set; }
    public string SearchTags { get; set; } = string.Empty;
    public bool IsVisible { get; set; }
    public DateTime ComputedAt { get; set; }
    public LicenseType LicenseType { get; set; }
    public decimal CurrentPrice { get; set; }
    public bool IsFreeContent { get; set; }
    public bool IsPremiumContent { get; set; }
    public AgeRating AgeRating { get; set; }
    public ContentFlag ContentFlags { get; set; }
    public string ContentWarning { get; set; } = string.Empty;
}

public class VisibilityComputationResultDto
{
    public long ComicId { get; set; }
    public bool Success { get; set; }
    public string? ErrorMessage { get; set; }
    public DateTime ComputationTime { get; set; }
    public List<ComputedVisibilityDto> ComputedVisibilities { get; set; } = new();
}
