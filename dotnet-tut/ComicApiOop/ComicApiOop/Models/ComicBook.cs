// using System;
// using System.Collections.Generic;
//
// namespace ComicApiOop.Models;
//
// // Core entities that we'll fetch from the database
// public class ComicBook
// {
//     public long Id { get; set; }
//     public string Title { get; set; } = string.Empty;
//     public long PublisherId { get; set; }
//     public long GenreId { get; set; }
//     public long ThemeId { get; set; }
//     public int TotalChapters { get; set; }
//     public DateTime LastUpdateTime { get; set; }
//     
//     // Navigation properties
//     public List<Chapter> Chapters { get; set; } = new();
//     public ICollection<ComicTag> Tags { get; set; } = new List<ComicTag>();
//     public Publisher? Publisher { get; set; }
//     public Genre? Genre { get; set; }
//     public Theme? Theme { get; set; }
//     public double AverageRating { get; set; }
//     
//     // New navigation properties
//     public ContentRating? ContentRating { get; set; }
//     public List<ComicPricing> RegionalPricing { get; set; } = new();
//     public List<GeographicRule> RegionalAvailability { get; set; } = new();
//     public List<CustomerSegmentRule> CustomerSegmentRules { get; set; } = new();
// }
//
// public class Chapter
// {
//     public long Id { get; set; }
//     public long ComicId { get; set; }
//     public int ChapterNumber { get; set; }
//     public DateTime ReleaseTime { get; set; }
//     public bool IsFree { get; set; }
//     
//     // Navigation property
//     public ComicBook? Comic { get; set; }
// }
//
// // Metadata entities
// public class Publisher
// {
//     public long Id { get; set; }
//     public string Name { get; set; } = string.Empty;
//     public List<ComicBook> Comics { get; set; } = new();
// }
//
// public class Genre
// {
//     public long Id { get; set; }
//     public string Name { get; set; } = string.Empty;
//     public List<ComicBook> Comics { get; set; } = new();
// }
//
// public class Theme
// {
//     public long Id { get; set; }
//     public string Name { get; set; } = string.Empty;
//     public List<ComicBook> Comics { get; set; } = new();
// }
//
// public class ComicTag
// {
//     public long Id { get; set; }
//     public string Name { get; set; } = string.Empty;
//     public List<ComicBook> Comics { get; set; } = new();
// }
//
//
//
//
