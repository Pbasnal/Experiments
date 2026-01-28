using Common.Models;

namespace ComicApiBenchmarks;

public static class MockDataGenerator
{
    private static readonly Random Random = new Random(42); // Fixed seed for reproducibility
    private static readonly string[] Countries = { "US", "UK", "CA", "AU", "DE", "FR", "JP", "KR" };

    private static readonly string[] Tags =
        { "action", "comedy", "drama", "fantasy", "romance", "sci-fi", "horror", "mystery" };

    public static (ComicBook comic, List<GeographicRule> geoRules, List<CustomerSegmentRule> segmentRules)
        CreateOopComicBook(long comicId, int numChapters = 20, int numGeoRules = 5, int numSegmentRules = 3,
            int numPricingRegions = 5)
    {
        var comic = new ComicBook
        {
            Id = comicId,
            Title = $"Comic {comicId}",
            PublisherId = 1,
            GenreId = 1,
            ThemeId = 1,
            TotalChapters = numChapters,
            LastUpdateTime = DateTime.UtcNow.AddDays(-Random.Next(1, 365)),
            AverageRating = Random.NextDouble() * 5.0,
            ContentRating = new ContentRating
            {
                Id = comicId,
                ComicId = comicId,
                AgeRating = AgeRating.Teen,
                ContentFlags = ContentFlag.None,
                ContentWarning = string.Empty,
                RequiresParentalGuidance = false
            }
        };

        // Generate chapters
        for (int i = 1; i <= numChapters; i++)
        {
            comic.Chapters.Add(new Chapter
            {
                Id = comicId * 1000 + i,
                ComicId = comicId,
                ChapterNumber = i,
                ReleaseTime = DateTime.UtcNow.AddDays(-Random.Next(0, 365)),
                IsFree = Random.NextDouble() < 0.3 // 30% free chapters
            });
        }

        // Generate geographic rules (OOP fetches these separately)
        var geoRules = new List<GeographicRule>();
        for (int i = 0; i < numGeoRules; i++)
        {
            var countryCount = Random.Next(1, 3);
            var countries = Countries.OrderBy(x => Random.Next()).Take(countryCount).ToList();

            geoRules.Add(new GeographicRule
            {
                Id = comicId * 100 + i,
                ComicId = comicId,
                CountryCodes = countries,
                LicenseStartDate = DateTime.UtcNow.AddDays(-Random.Next(30, 365)),
                LicenseEndDate = DateTime.UtcNow.AddDays(Random.Next(30, 365)),
                LicenseType = LicenseType.Full,
                IsVisible = true,
                LastUpdated = DateTime.UtcNow
            });
        }

        // Generate customer segment rules (OOP fetches these separately)
        var segmentRules = new List<CustomerSegmentRule>();
        for (int i = 0; i < numSegmentRules; i++)
        {
            var segmentId = i + 1;
            segmentRules.Add(new CustomerSegmentRule
            {
                Id = comicId * 1000 + i,
                ComicId = comicId,
                SegmentId = segmentId,
                Segment = new CustomerSegment
                {
                    Id = segmentId,
                    Name = $"Segment {segmentId}",
                    IsPremium = i == 0,
                    IsActive = true
                },
                IsVisible = true,
                LastUpdated = DateTime.UtcNow
            });
        }

        // Generate regional pricing
        for (int i = 0; i < numPricingRegions; i++)
        {
            comic.RegionalPricing.Add(new ComicPricing
            {
                Id = comicId * 100 + i,
                ComicId = comicId,
                RegionCode = Countries[i % Countries.Length],
                BasePrice = (decimal)(Random.NextDouble() * 10 + 1),
                IsFreeContent = Random.NextDouble() < 0.1,
                IsPremiumContent = Random.NextDouble() < 0.2,
                DiscountStartDate = Random.NextDouble() < 0.3 ? DateTime.UtcNow.AddDays(-10) : null,
                DiscountEndDate = Random.NextDouble() < 0.3 ? DateTime.UtcNow.AddDays(10) : null,
                DiscountPercentage = Random.NextDouble() < 0.3 ? (decimal?)Random.Next(10, 50) : null
            });
        }

        // Generate tags (OOP structure)
        var numTags = Random.Next(2, 5);
        var selectedTags = Tags.OrderBy(x => Random.Next()).Take(numTags).ToList();
        foreach (var tagName in selectedTags)
        {
            var tagId = Random.Next(1, 1000);
            comic.ComicTags.Add(new ComicTag
            {
                ComicsId = comicId,
                TagsId = tagId,
                Tag = new Tag
                {
                    Id = tagId,
                    Name = tagName
                }
            });
        }

        return (comic, geoRules, segmentRules);
    }

    public static ComicBook[] CreateOopComicBooks(int count, int numChapters = 20, int numGeoRules = 5,
        int numSegmentRules = 3, int numPricingRegions = 5)
    {
        var comics = new ComicBook[count];
        for (int i = 0; i < count; i++)
        {
            var (comic, _, _) = CreateOopComicBook(i + 1, numChapters, numGeoRules, numSegmentRules, numPricingRegions);
            comics[i] = comic;
        }

        return comics;
    }


    public static ComicBook CreateDodComicBook(long comicId, int numChapters = 20, int numGeoRules = 5,
        int numSegmentRules = 3, int numPricingRegions = 5)
    {
        var comic = new ComicBook
        {
            Id = comicId,
            Title = $"Comic {comicId}",
            PublisherId = 1,
            GenreId = 1,
            ThemeId = 1,
            TotalChapters = numChapters,
            LastUpdateTime = DateTime.UtcNow.AddDays(-Random.Next(1, 365)),
            AverageRating = Random.NextDouble() * 5.0,
            ContentRating = new ContentRating
            {
                Id = comicId,
                ComicId = comicId,
                AgeRating = AgeRating.Teen,
                ContentFlags = ContentFlag.None,
                ContentWarning = string.Empty,
                RequiresParentalGuidance = false
            }
        };

        // Generate chapters
        for (int i = 1; i <= numChapters; i++)
        {
            comic.Chapters.Add(new Chapter
            {
                Id = comicId * 1000 + i,
                ComicId = comicId,
                ChapterNumber = i,
                ReleaseTime = DateTime.UtcNow.AddDays(-Random.Next(0, 365)),
                IsFree = Random.NextDouble() < 0.3 // 30% free chapters
            });
        }

        // Generate geographic rules
        for (int i = 0; i < numGeoRules; i++)
        {
            var countryCount = Random.Next(1, 3);
            var countries = Countries.OrderBy(x => Random.Next()).Take(countryCount).ToList();

            comic.GeographicRules.Add(new GeographicRule
            {
                Id = comicId * 100 + i,
                ComicId = comicId,
                CountryCodes = countries,
                LicenseStartDate = DateTime.UtcNow.AddDays(-Random.Next(30, 365)),
                LicenseEndDate = DateTime.UtcNow.AddDays(Random.Next(30, 365)),
                LicenseType = LicenseType.Full,
                IsVisible = true,
                LastUpdated = DateTime.UtcNow
            });
        }

        // Generate customer segment rules
        for (int i = 0; i < numSegmentRules; i++)
        {
            var segmentId = i + 1;
            comic.CustomerSegmentRules.Add(new CustomerSegmentRule
            {
                Id = comicId * 1000 + i,
                ComicId = comicId,
                SegmentId = segmentId,
                Segment = new CustomerSegment
                {
                    Id = segmentId,
                    Name = $"Segment {segmentId}",
                    IsPremium = i == 0,
                    IsActive = true
                },
                IsVisible = true,
                LastUpdated = DateTime.UtcNow
            });
        }

        // Generate regional pricing
        for (int i = 0; i < numPricingRegions; i++)
        {
            comic.RegionalPricing.Add(new ComicPricing
            {
                Id = comicId * 100 + i,
                ComicId = comicId,
                RegionCode = Countries[i % Countries.Length],
                BasePrice = (decimal)(Random.NextDouble() * 10 + 1),
                IsFreeContent = Random.NextDouble() < 0.1,
                IsPremiumContent = Random.NextDouble() < 0.2,
                DiscountStartDate = Random.NextDouble() < 0.3 ? DateTime.UtcNow.AddDays(-10) : null,
                DiscountEndDate = Random.NextDouble() < 0.3 ? DateTime.UtcNow.AddDays(10) : null,
                DiscountPercentage = Random.NextDouble() < 0.3 ? (decimal?)Random.Next(10, 50) : null
            });
        }

        // Generate tags (DoD structure)
        var numTags = Random.Next(2, 5);
        var selectedTags = Tags.OrderBy(x => Random.Next()).Take(numTags).ToList();
        foreach (var tagName in selectedTags)
        {
            var tagId = Random.Next(1, 1000);
            comic.ComicTags.Add(new ComicTag
            {
                ComicsId = comicId,
                TagsId = tagId,
                Tag = new Tag
                {
                    Id = tagId,
                    Name = tagName
                }
            });
        }

        return comic;
    }

    public static ComicBook[][] CreateDodComicBookBatches(int numBatches, int comicsPerBatch,
        int numChapters = 20, int numGeoRules = 5, int numSegmentRules = 3, int numPricingRegions = 5)
    {
        var batches = new ComicBook[numBatches][];
        long comicId = 1;

        for (int i = 0; i < numBatches; i++)
        {
            var batch = new ComicBook[comicsPerBatch];
            for (int j = 0; j < comicsPerBatch; j++)
            {
                batch[j] = CreateDodComicBook(comicId++, numChapters, numGeoRules, numSegmentRules, numPricingRegions);
            }

            batches[i] = batch;
        }

        return batches;
    }
}