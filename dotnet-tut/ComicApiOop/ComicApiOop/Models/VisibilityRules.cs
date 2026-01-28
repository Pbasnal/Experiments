// namespace ComicApiOop.Models;
//
// public abstract class VisibilityRule
// {
//     public long Id { get; set; }
//     public long ComicId { get; set; }
//     public bool IsVisible { get; set; }
//     public DateTime LastUpdated { get; set; }
//     
//     // Navigation property
//     public ComicBook? Comic { get; set; }
//     
//     public abstract bool EvaluateVisibility();
// }
//
// public class GeographicRule : VisibilityRule
// {
//     public List<string> CountryCodes { get; set; } = new();
//     public DateTime LicenseStartDate { get; set; }
//     public DateTime LicenseEndDate { get; set; }
//     public LicenseType LicenseType { get; set; }
//     
//     public override bool EvaluateVisibility()
//     {
//         var now = DateTime.UtcNow;
//         return IsVisible 
//                && now >= LicenseStartDate 
//                && now <= LicenseEndDate 
//                && LicenseType != LicenseType.NoAccess;
//     }
// }
//
// public class CustomerSegmentRule : VisibilityRule
// {
//     public long SegmentId { get; set; }
//     public CustomerSegment? Segment { get; set; }
//     
//     public override bool EvaluateVisibility()
//     {
//         // Implementation will check if comic should be visible for this segment
//         return IsVisible && Segment?.IsActive == true;
//     }
// }
//
// public class CustomerSegment
// {
//     public long Id { get; set; }
//     public string Name { get; set; } = string.Empty;
//     public bool IsPremium { get; set; }
//     public bool IsActive { get; set; }
//     public List<CustomerSegmentRule> Rules { get; set; } = new();
// }
