using ComicApiDod.Data;
using Common.Models;

namespace ComicApiDod.Services;

public struct ComicGeoSegFilter
{
    public int totalVisibleCount;
    public bool[] geoFilter;
    public bool[] segmentFilter;
    public IDictionary<long, IList<int>> comicToGeoIndex;
    public IDictionary<long, IList<int>> comicToSegIndex;
    
    public ComicGeoSegFilter(int lenOfGeoFilter, int lenOfSegmentFilter)
    {
        geoFilter = new bool[lenOfGeoFilter];
        segmentFilter = new bool[lenOfSegmentFilter];
        comicToGeoIndex = new Dictionary<long, IList<int>>();
        comicToSegIndex = new Dictionary<long, IList<int>>();
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

    public static ComicGeoSegFilter GenerateGeoSegFilters(
        DodSqlHelper.GeoRuleRow[] geoRules,
        DodSqlHelper.SegmentRuleRow[] segRules,
        DateTime computationTime)
    {
        ComicGeoSegFilter geoSegFilters = new ComicGeoSegFilter(
            geoRules.Length,
            segRules.Length);

        // comicBooks[i][j] <- this gives jth comic for request i
        int visibleGeos = 0;
        // assumption: SQL will get geoRules sorted on comicId
        // So when we are building the Dictionary, we will be
        // processing 1 comicId at a time and code won't jump 
        // between comicIds
        for (int geoRuleIdx = 0; geoRuleIdx < geoRules.Length; geoRuleIdx++)
        {
            geoSegFilters.geoFilter[geoRuleIdx] = VisibilityProcessor
                .EvaluateGeographicVisibility(
                    geoRules[geoRuleIdx],
                    computationTime);
            if (geoSegFilters.geoFilter[geoRuleIdx]) visibleGeos++;

            updateIndex(geoSegFilters.comicToGeoIndex, geoRules[geoRuleIdx].ComicId, geoRuleIdx);
        }

        int visibleSegs = 0;
        for (int segIdx = 0; segIdx < segRules.Length; segIdx++)
        {
            geoSegFilters.segmentFilter[segIdx] = VisibilityProcessor
                .EvaluateSegmentVisibility(segRules[segIdx]);
            if (geoSegFilters.segmentFilter[segIdx]) visibleSegs++;
            
            updateIndex(geoSegFilters.comicToSegIndex, segRules[segIdx].ComicId, segIdx);
        }

        geoSegFilters.totalVisibleCount = visibleGeos * visibleSegs;
        
        return geoSegFilters;
    }

    private static void updateIndex(
        IDictionary<long, IList<int>> index,
        long comicId,
        int idx)
    {
        if (!index.ContainsKey(comicId))
        {
            index.Add(comicId, new List<int>());
        }
        index[comicId].Add(idx);
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
                        geoPricingOfComics[reqIdx][comicIdx].pricing[geoIdx]?.BasePrice
                    );
                }

                comicMetas[reqIdx][comicIdx] = comicMeta;
            }
        }

        return comicMetas;
    }

    public static IDictionary<long, ComicMeta> GenerateComicMeta(
        DodSqlHelper.DodVisibilityBatch comicBatch)
    {
        IDictionary<long, ComicMeta> comicMetaMap = new Dictionary<long, ComicMeta>();
        for (int comicIdx = 0; comicIdx < comicBatch.Comics.Length; comicIdx++)
        {
            DodSqlHelper.ComicRow comic = comicBatch.Comics[comicIdx];
            ComicMeta comicMeta = new ComicMeta(comicBatch.Pricings.Length);

            comicMeta.comicId = comic.Id;
            comicMeta.freeChapterCount = comicBatch.Chapters[comic.Id].Count(c => c.IsFree);
            comicMeta.allChaptersFree = comicBatch.Chapters[comic.Id].Count == comicMeta.freeChapterCount;
            comicMeta.lastChapterReleaseTime = comicBatch.Chapters[comic.Id].Max(c => c.ReleaseTime);
            comicMeta.searchTags = comicBatch.ComicTags.ContainsKey(comic.Id) 
                ?  string.Join(",", comicBatch.ComicTags[comic.Id] .Select(t => t.TagName))
                : string.Empty;
            
            comicMetaMap.Add(comic.Id, comicMeta);
        }
        return comicMetaMap;
    }
}