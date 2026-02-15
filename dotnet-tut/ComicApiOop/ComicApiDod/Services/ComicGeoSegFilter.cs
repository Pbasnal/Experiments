using ComicApiDod.Data;
using Common.Models;

namespace ComicApiDod.Services;

public struct ComicGeoSegFilter
{
    public int totalVisibleCount;
    public bool[] geoFilter;
    public bool[] segmentFilter;

    public ComicGeoSegFilter(int lenOfGeoFilter, int lenOfSegmentFilter)
    {
        geoFilter = new bool[lenOfGeoFilter];
        segmentFilter = new bool[lenOfSegmentFilter];
    }

    public static ComicGeoSegFilter[][] GenerateGeoSegFilters(ComicBook[][] comicBooks,
        DateTime computationTime)
    {
        ComicGeoSegFilter[][] geoSegFilters = new ComicGeoSegFilter[comicBooks.Length][];
        for (int reqIdx = 0; reqIdx < comicBooks.Length; reqIdx++)
        {
            // comicBooks[i] <- this gives list of comics for request i
            geoSegFilters[reqIdx] = new ComicGeoSegFilter[comicBooks[reqIdx].Length];
            for (int comicIdx = 0; comicIdx < comicBooks[reqIdx].Length; comicIdx++)
            {
                ComicBook comic = comicBooks[reqIdx][comicIdx];
                geoSegFilters[reqIdx][comicIdx] = new ComicGeoSegFilter(
                    comic.GeographicRules.Count,
                    comic.CustomerSegmentRules.Count);

                // comicBooks[i][j] <- this gives jth comic for request i
                int visibleGeos = 0;
                for (int geoRuleIdx = 0; geoRuleIdx < comic.GeographicRules.Count; geoRuleIdx++)
                {
                    geoSegFilters[reqIdx][comicIdx].geoFilter[geoRuleIdx] = VisibilityProcessor
                        .EvaluateGeographicVisibility(
                            comic.GeographicRules[geoRuleIdx],
                            computationTime);
                    if (geoSegFilters[reqIdx][comicIdx].geoFilter[geoRuleIdx]) visibleGeos++;
                }

                int visibleSegs = 0;
                for (int segIdx = 0; segIdx < comic.CustomerSegmentRules.Count; segIdx++)
                {
                    geoSegFilters[reqIdx][comicIdx].segmentFilter[segIdx] = VisibilityProcessor
                        .EvaluateSegmentVisibility(comic.CustomerSegmentRules[segIdx]);
                    if (geoSegFilters[reqIdx][comicIdx].segmentFilter[segIdx]) visibleSegs++;
                }

                geoSegFilters[reqIdx][comicIdx].totalVisibleCount = visibleGeos * visibleSegs;
            }
            
        }
        
        return geoSegFilters;
    }
}

public struct ComicGeoPricing
{
    public ComicPricing?[] pricing;

    public ComicGeoPricing(int lenOfGeoPricing)
    {
        pricing = new ComicPricing?[lenOfGeoPricing];
    }

    public static ComicGeoPricing[][] GenerateGeoPricingRules(ComicBook[][] comicBooks,
        ComicGeoSegFilter[][] geoSegFilters)
    {
        ComicGeoPricing[][] geoPricingOfComics = new ComicGeoPricing[comicBooks.Length][];
        for (int reqIdx = 0; reqIdx < comicBooks.Length; reqIdx++)
        {
            geoPricingOfComics[reqIdx] = new ComicGeoPricing[comicBooks[reqIdx].Length];
            for (int comicIdx = 0; comicIdx < comicBooks[reqIdx].Length; comicIdx++)
            {
                ComicBook comic = comicBooks[reqIdx][comicIdx];
                geoPricingOfComics[reqIdx][comicIdx] = new ComicGeoPricing(comic.GeographicRules.Count);
                // comicBooks[i][j] <- this gives jth comic for request i
                for (int geoIdx = 0; geoIdx < comic.GeographicRules.Count; geoIdx++)
                {
                    GeographicRule geoRule = comic.GeographicRules[geoIdx];

                    if (geoSegFilters[reqIdx][comicIdx].geoFilter[geoIdx])
                    {
                        geoPricingOfComics[reqIdx][comicIdx].pricing[geoIdx] = comic.RegionalPricing
                            .FirstOrDefault(p => geoRule.CountryCodes.Contains(p.RegionCode));
                    }
                    else
                    {
                        geoPricingOfComics[reqIdx][comicIdx].pricing[geoIdx] = null;
                    }
                }
            }
        }

        return geoPricingOfComics;
    }
}

public struct ComicMeta
{
    public long comicId;
    public int freeChapterCount;
    public bool allChaptersFree;
    public DateTime lastChapterReleaseTime;
    public string searchTags;
    public ContentFlag[] contentFlag;

    public ComicMeta(int lenOfGeoRules)
    {
        contentFlag = new ContentFlag[lenOfGeoRules];
    }

    public static ComicMeta[][] GenerateComicMeta(
        ComicBook[][] comicBooks,
        ComicGeoPricing[][] geoPricingOfComics)
    {
        ComicMeta[][] comicMetas = new ComicMeta[comicBooks.Length][];
        for (int reqIdx = 0; reqIdx < comicMetas.Length; reqIdx++)
        {
            comicMetas[reqIdx] = new ComicMeta[comicBooks[reqIdx].Length];
            for (int comicIdx = 0; comicIdx < comicBooks[reqIdx].Length; comicIdx++)
            {
                ComicBook comic = comicBooks[reqIdx][comicIdx];
                ComicMeta comicMeta = new ComicMeta(comic.GeographicRules.Count);

                comicMeta.comicId = comic.Id;
                comicMeta.freeChapterCount = comic.Chapters.Count(c => c.IsFree);
                comicMeta.allChaptersFree = comic.TotalChapters == comicMeta.freeChapterCount;
                comicMeta.lastChapterReleaseTime = comic.Chapters.Max(c => c.ReleaseTime);
                comicMeta.searchTags = string.Join(",", comic.ComicTags.Select(t => t.Tag.Name));

                for (int geoIdx = 0; geoIdx < comic.GeographicRules.Count; geoIdx++)
                {
                    comicMeta.contentFlag[geoIdx] = VisibilityProcessor.DetermineContentFlags(
                        comic.ContentRating?.ContentFlags ?? ContentFlag.None,
                        comicMeta.allChaptersFree, comicMeta.freeChapterCount > 0,
                        geoPricingOfComics[reqIdx][comicIdx].pricing[geoIdx]
                    );
                }

                comicMetas[reqIdx][comicIdx] = comicMeta;
            }
        }

        return comicMetas;
    }
}