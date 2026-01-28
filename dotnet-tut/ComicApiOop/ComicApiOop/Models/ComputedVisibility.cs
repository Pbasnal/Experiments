// using System;
// using System.Collections.Generic;
//
// namespace ComicApiOop.Models;
//
// public class ComputedVisibility
// {
//     public long Id { get; set; }
//     public long ComicId { get; set; }
//     public string CountryCode { get; set; } = string.Empty;
//     public long CustomerSegmentId { get; set; }
//     public int FreeChaptersCount { get; set; }
//     public DateTime LastChapterReleaseTime { get; set; }
//     public long GenreId { get; set; }
//     public long PublisherId { get; set; }
//     public double AverageRating { get; set; }
//     public string SearchTags { get; set; } = string.Empty;
//     public bool IsVisible { get; set; }
//     public DateTime ComputedAt { get; set; }
//     
//     // New fields for licensing and content
//     public LicenseType LicenseType { get; set; }
//     public decimal CurrentPrice { get; set; }
//     public bool IsFreeContent { get; set; }
//     public bool IsPremiumContent { get; set; }
//     public AgeRating AgeRating { get; set; }
//     public ContentFlag ContentFlags { get; set; }
//     public string ContentWarning { get; set; } = string.Empty;
//     
//     // Navigation properties
//     public ComicBook? Comic { get; set; }
//     public CustomerSegment? CustomerSegment { get; set; }
// }
//
// // This class represents the request to compute visibility
// public class VisibilityComputationRequest
// {
//     public long ComicId { get; set; }
//     public DateTime RequestTime { get; set; }
//     public string TriggerReason { get; set; } = string.Empty;
// }
//
// // This class represents the result of visibility computation
// public class VisibilityComputationResult
// {
//     public long ComicId { get; set; }
//     public bool Success { get; set; }
//     public string? ErrorMessage { get; set; }
//     public DateTime ComputationTime { get; set; }
//     public List<ComputedVisibility> ComputedVisibilities { get; set; } = new();
// }
//
//
//
//
