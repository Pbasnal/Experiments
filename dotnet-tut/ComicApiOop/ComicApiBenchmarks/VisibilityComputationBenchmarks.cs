using BenchmarkDotNet.Attributes;
using BenchmarkDotNet.Order;
using ComicApiDod.Data;
using ComicApiOop.Services;
using Common.Models;

namespace ComicApiBenchmarks;

[MemoryDiagnoser]
[Orderer(SummaryOrderPolicy.FastestToSlowest)]
[Config(typeof(BenchmarkConfig))]
public class VisibilityComputationBenchmarks
{
    private ComicBook[] _oopComics = null!;
    private List<GeographicRule>[] _oopGeoRules = null!;
    private List<CustomerSegmentRule>[] _oopSegmentRules = null!;
    private Common.Models.ComicBook[][] _dodComicBatches = null!;
    private DateTime _computationTime;

    [Params(1, 5, 10, 20)]
    public int NumberOfComics { get; set; }

    [Params(20)]
    public int ChaptersPerComic { get; set; }

    [Params(5)]
    public int GeoRulesPerComic { get; set; }

    [Params(3)]
    public int SegmentRulesPerComic { get; set; }

    [GlobalSetup]
    public void Setup()
    {
        _computationTime = DateTime.UtcNow;
        
        // Create OOP comics with separate rules (matching actual service behavior)
        _oopComics = new ComicBook[NumberOfComics];
        _oopGeoRules = new List<GeographicRule>[NumberOfComics];
        _oopSegmentRules = new List<CustomerSegmentRule>[NumberOfComics];
        
        for (int i = 0; i < NumberOfComics; i++)
        {
            var (comic, geoRules, segmentRules) = MockDataGenerator.CreateOopComicBook(
                i + 1,
                ChaptersPerComic,
                GeoRulesPerComic,
                SegmentRulesPerComic
            );
            _oopComics[i] = comic;
            _oopGeoRules[i] = geoRules;
            _oopSegmentRules[i] = segmentRules;
        }

        // Create DoD batches (simulating batch processing)
        // For fair comparison, create batches of 1 comic each
        _dodComicBatches = MockDataGenerator.CreateDodComicBookBatches(
            NumberOfComics,
            1, // 1 comic per batch
            ChaptersPerComic,
            GeoRulesPerComic,
            SegmentRulesPerComic
        );
    }

    [Benchmark(Baseline = true)]
    [BenchmarkCategory("OOP")]
    public int OopComputation()
    {
        int totalVisibilities = 0;
        
        for (int i = 0; i < _oopComics.Length; i++)
        {
            // Use the actual service method
            var result = VisibilityComputationService.ComputeVisibilities(
                _oopComics[i],
                _oopGeoRules[i],
                _oopSegmentRules[i],
                _computationTime);
            totalVisibilities += result.Length;
        }
        
        return totalVisibilities;
    }

    [Benchmark]
    [BenchmarkCategory("DoD")]
    public int DodComputation()
    {
        int totalVisibilities = 0;
        
        foreach (var batch in _dodComicBatches)
        {
            foreach (var comic in batch)
            {
                if (comic != null)
                {
                    var result = VisibilityProcessor.ComputeVisibilities(comic, _computationTime);
                    totalVisibilities += result.Length;
                }
            }
        }
        
        return totalVisibilities;
    }

    // [Benchmark]
    // [BenchmarkCategory("OOP")]
    // public int OopComputationWithCaching()
    // {
    //     int totalVisibilities = 0;
    //     
    //     for (int i = 0; i < _oopComics.Length; i++)
    //     {
    //         var result = ComputeOopVisibilityWithCaching(_oopComics[i], _oopGeoRules[i], _oopSegmentRules[i], _computationTime);
    //         totalVisibilities += result.Count;
    //     }
    //     
    //     return totalVisibilities;
    // }

    //
    // // Optimized OOP version with caching (for comparison)
    // private List<ComputedVisibilityDto> ComputeOopVisibilityWithCaching(
    //     ComicBook comic,
    //     List<GeographicRule> geographicRules,
    //     List<CustomerSegmentRule> customerSegmentRules,
    //     DateTime computationTime)
    // {
    //     var computedVisibilities = new List<ComputedVisibilityDto>();
    //     
    //     // Cache computed values
    //     int freeChaptersCount = comic.Chapters.Count(c => c.IsFree);
    //     DateTime lastChapterReleaseTime = comic.Chapters.Max(c => c.ReleaseTime);
    //     string searchTags = string.Join(",", comic.Tags.Select(t => t.Name));
    //     
    //     // Pre-compute pricing lookup
    //     var pricingByRegion = comic.RegionalPricing.ToDictionary(p => p.RegionCode, p => p);
    //     
    //     foreach (var geoRule in geographicRules)
    //     {
    //         if (!geoRule.EvaluateVisibility())
    //             continue;
    //             
    //         foreach (var segmentRule in customerSegmentRules)
    //         {
    //             if (!segmentRule.IsVisible)
    //                 continue;
    //
    //             // Get regional pricing using dictionary lookup
    //             ComicPricing? pricing = null;
    //             foreach (var countryCode in geoRule.CountryCodes)
    //             {
    //                 if (pricingByRegion.TryGetValue(countryCode, out var p))
    //                 {
    //                     pricing = p;
    //                     break;
    //                 }
    //             }
    //
    //             var visibility = new ComputedVisibilityDto
    //             {
    //                 ComicId = comic.Id,
    //                 CountryCode = geoRule.CountryCodes.First(),
    //                 CustomerSegmentId = segmentRule.SegmentId,
    //                 FreeChaptersCount = freeChaptersCount,
    //                 LastChapterReleaseTime = lastChapterReleaseTime,
    //                 GenreId = comic.GenreId,
    //                 PublisherId = comic.PublisherId,
    //                 AverageRating = comic.AverageRating,
    //                 SearchTags = searchTags,
    //                 IsVisible = true,
    //                 ComputedAt = computationTime,
    //                 LicenseType = geoRule.LicenseType,
    //                 CurrentPrice = pricing?.GetCurrentPrice() ?? 0m,
    //                 IsFreeContent = pricing?.IsFreeContent ?? false,
    //                 IsPremiumContent = pricing?.IsPremiumContent ?? false,
    //                 AgeRating = comic.ContentRating?.AgeRating ?? AgeRating.AllAges,
    //                 ContentFlags = ContentFlagService.DetermineContentFlags(
    //                     comic.ContentRating?.ContentFlags ?? ContentFlag.None,
    //                     comic.Chapters,
    //                     pricing),
    //                 ContentWarning = comic.ContentRating?.ContentWarning ?? string.Empty
    //             };
    //             computedVisibilities.Add(visibility);
    //         }
    //     }
    //
    //     return computedVisibilities;
    // }
}
