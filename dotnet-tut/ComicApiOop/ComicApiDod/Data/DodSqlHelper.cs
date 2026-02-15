using Common.Models;
using Microsoft.EntityFrameworkCore;
using MySqlConnector;

namespace ComicApiDod.Data;

/// <summary>
/// Fetches visibility computation data directly from SQL in a DOD-friendly layout.
/// Uses a single round-trip with multiple result sets for cache-efficient processing.
/// </summary>
public static class DodSqlHelper
{
    /// <summary>
    /// DOD-friendly row structs filled directly from SQL result sets.
    /// Flat, contiguous layout for cache efficiency.
    /// </summary>
    public readonly struct DodVisibilityBatch
    {
        public readonly ComicRow[] Comics;
        public readonly GeoRuleRow[] GeoRules;
        public readonly SegmentRuleRow[] SegmentRules;
        public readonly PricingRow[] Pricings;
        public readonly IDictionary<long, IList<ComicTagRow>> ComicTags;
        public readonly IDictionary<long, IList<ChapterRow>> Chapters;
        public readonly IDictionary<long, ContentRatingRow> ContentRatings;

        public DodVisibilityBatch(
            ComicRow[] comics,
            IDictionary<long, IList<ChapterRow>> chapters,
            GeoRuleRow[] geoRules,
            SegmentRuleRow[] segmentRules,
            PricingRow[] pricings,
            IDictionary<long, ContentRatingRow> contentRatings,
            IDictionary<long, IList<ComicTagRow>> comicTags)
        {
            Comics = comics;
            Chapters = chapters;
            GeoRules = geoRules;
            SegmentRules = segmentRules;
            Pricings = pricings;
            ContentRatings = contentRatings;
            ComicTags = comicTags;
        }
    }

    public readonly struct ComicRow
    {
        public readonly long Id;
        public readonly long PublisherId;
        public readonly long GenreId;
        public readonly double AverageRating;

        public ComicRow(long id, long publisherId, long genreId, double averageRating)
        {
            Id = id;
            PublisherId = publisherId;
            GenreId = genreId;
            AverageRating = averageRating;
        }
    }

    public readonly struct ChapterRow
    {
        public readonly long ComicId;
        public readonly DateTime ReleaseTime;
        public readonly bool IsFree;

        public ChapterRow(long comicId, DateTime releaseTime, bool isFree)
        {
            ComicId = comicId;
            ReleaseTime = releaseTime;
            IsFree = isFree;
        }
    }

    public readonly struct GeoRuleRow
    {
        public readonly long Id;
        public readonly long ComicId;
        public readonly ISet<string> CountryCodes;
        public readonly DateTime LicenseStartDate;
        public readonly DateTime LicenseEndDate;
        public readonly int LicenseType;
        public readonly bool IsVisible;

        public GeoRuleRow(long id, long comicId, string countryCodes, DateTime licenseStart, DateTime licenseEnd,
            int licenseType, bool isVisible)
        {
            Id = id;
            ComicId = comicId;
            CountryCodes = countryCodes.Split(",").ToHashSet();
            LicenseStartDate = licenseStart;
            LicenseEndDate = licenseEnd;
            LicenseType = licenseType;
            IsVisible = isVisible;
        }
    }

    public readonly struct SegmentRuleRow
    {
        public readonly long ComicId;
        public readonly long SegmentId;
        public readonly bool IsVisible;
        public readonly bool SegmentIsActive;

        public SegmentRuleRow(long comicId, long segmentId, bool isVisible, bool segmentIsActive)
        {
            ComicId = comicId;
            SegmentId = segmentId;
            IsVisible = isVisible;
            SegmentIsActive = segmentIsActive;
        }
    }

    public readonly struct PricingRow
    {
        public readonly long ComicId;
        public readonly string RegionCode;
        public readonly decimal BasePrice;
        public readonly bool IsFreeContent;
        public readonly bool IsPremiumContent;

        public PricingRow(long comicId, string regionCode, decimal basePrice, bool isFreeContent, bool isPremiumContent)
        {
            ComicId = comicId;
            RegionCode = regionCode;
            BasePrice = basePrice;
            IsFreeContent = isFreeContent;
            IsPremiumContent = isPremiumContent;
        }
    }

    public readonly struct ContentRatingRow
    {
        public readonly long ComicId;
        public readonly int AgeRating;
        public readonly int ContentFlags;
        public readonly string ContentWarning;

        public ContentRatingRow(long comicId, int ageRating, int contentFlags, string contentWarning)
        {
            ComicId = comicId;
            AgeRating = ageRating;
            ContentFlags = contentFlags;
            ContentWarning = contentWarning;
        }
    }

    public readonly struct ComicTagRow
    {
        public readonly long ComicId;
        public readonly string TagName;

        public ComicTagRow(long comicId, string tagName)
        {
            ComicId = comicId;
            TagName = tagName;
        }
    }

    /// <summary>
    /// Fetches all data required for visibility computation in a single round-trip.
    /// Returns flat arrays in DOD layout, ready for cache-friendly processing.
    /// </summary>
    /// <param name="db">Database context (connection will be used; context not disposed)</param>
    /// <param name="comicIds">Comic IDs to fetch</param>
    /// <param name="ct">Cancellation token</param>
    public static async Task<DodVisibilityBatch> FetchVisibilityBatchAsync(
        ComicDbContext db,
        long[] comicIds,
        CancellationToken ct = default)
    {
        if (comicIds.Length == 0)
        {
            return new DodVisibilityBatch(
                Array.Empty<ComicRow>(),
                new Dictionary<long, IList<ChapterRow>>(),
                Array.Empty<GeoRuleRow>(),
                Array.Empty<SegmentRuleRow>(),
                Array.Empty<PricingRow>(),
                new Dictionary<long, ContentRatingRow>(),
                new Dictionary<long, IList<ComicTagRow>>());
        }

        var connection = db.Database.GetDbConnection();
        if (connection.State != System.Data.ConnectionState.Open)
            await connection.OpenAsync(ct);

        if (connection is not MySqlConnection mysqlConn)
            throw new InvalidOperationException("DodSqlHelper requires MySQL. Connection is not MySqlConnection.");

        var inPlaceholders = new string[comicIds.Length];
        for (int i = 0; i < comicIds.Length; i++)
            inPlaceholders[i] = $"@id{i}";
        var inClause = string.Join(", ", inPlaceholders);

        using var batch = new MySqlBatch(mysqlConn)
        {
            BatchCommands =
            {
                new MySqlBatchCommand($@"
                    SELECT Id, PublisherId, GenreId, AverageRating
                    FROM Comics
                    WHERE Id IN ({inClause})
                    ORDER BY Id"),

                new MySqlBatchCommand($@"
                    SELECT ComicId, ReleaseTime, IsFree
                    FROM Chapters
                    WHERE ComicId IN ({inClause})
                    ORDER BY ComicId, ReleaseTime"),

                new MySqlBatchCommand($@"
                    SELECT gr.Id, gr.ComicId, gr.CountryCodes, gr.LicenseStartDate, gr.LicenseEndDate, gr.LicenseType, gr.IsVisible
                    FROM GeographicRules gr
                    WHERE gr.ComicId IN ({inClause})
                    ORDER BY gr.ComicId"),

                new MySqlBatchCommand($@"
                    SELECT csr.ComicId, csr.SegmentId, csr.IsVisible, cs.IsActive AS SegmentIsActive
                    FROM CustomerSegmentRules csr
                    INNER JOIN CustomerSegments cs ON cs.Id = csr.SegmentId
                    WHERE csr.ComicId IN ({inClause})
                    ORDER BY csr.ComicId"),

                new MySqlBatchCommand($@"
                    SELECT ComicId, RegionCode, BasePrice, IsFreeContent, IsPremiumContent
                    FROM ComicPricings
                    WHERE ComicId IN ({inClause})
                    ORDER BY ComicId"),

                new MySqlBatchCommand($@"
                    SELECT ComicId, AgeRating, ContentFlags, ContentWarning
                    FROM ContentRatings
                    WHERE ComicId IN ({inClause})"),

                new MySqlBatchCommand($@"
                    SELECT ct.ComicsId AS ComicId, t.Name AS TagName
                    FROM ComicTags ct
                    INNER JOIN Tags t ON t.Id = ct.TagsId
                    WHERE ct.ComicsId IN ({inClause})")
            }
        };

        for (int i = 0; i < batch.BatchCommands.Count; i++)
        {
            for (int j = 0; j < comicIds.Length; j++)
                batch.BatchCommands[i].Parameters.Add(new MySqlParameter($"@id{j}", comicIds[j]));
        }

        var comics = new List<ComicRow>();
        var geoRules = new List<GeoRuleRow>();
        var segmentRules = new List<SegmentRuleRow>();
        var pricings = new List<PricingRow>();
        IDictionary<long, IList<ComicTagRow>> comicTags = new Dictionary<long, IList<ComicTagRow>>();
        IDictionary<long, ContentRatingRow> contentRatings =
            new Dictionary<long, ContentRatingRow>();
        IDictionary<long, IList<ChapterRow>> chapters =
            new Dictionary<long, IList<ChapterRow>>();

        await using var reader = (MySqlDataReader)await batch.ExecuteReaderAsync(ct);

        // Result sets arrive in BatchCommands order: Comics, Chapters, GeoRules, SegmentRules, Pricings, ContentRatings, ComicTags
        // Comics
        while (await reader.ReadAsync(ct))
            comics.Add(new ComicRow(reader.GetInt64(0), reader.GetInt64(1), reader.GetInt64(2), reader.GetDouble(3)));

        await reader.NextResultAsync(ct);
        // Chapters
        while (await reader.ReadAsync(ct))
        {
            long comicId = reader.GetInt64(0);
            if (!chapters.ContainsKey(comicId))
            {
                chapters.Add(comicId, new List<ChapterRow>());
            }

            chapters[comicId].Add(new ChapterRow(comicId, reader.GetDateTime(1), reader.GetBoolean(2)));
        }

        await reader.NextResultAsync(ct);
        // GeoRules
        while (await reader.ReadAsync(ct))
            geoRules.Add(new GeoRuleRow(
                reader.GetInt64(0),
                reader.GetInt64(1),
                reader.GetString(2),
                reader.GetDateTime(3),
                reader.GetDateTime(4),
                reader.GetInt32(5),
                reader.GetBoolean(6)));


        await reader.NextResultAsync(ct);
        // SegmentRules
        while (await reader.ReadAsync(ct))
            segmentRules.Add(new SegmentRuleRow(
                reader.GetInt64(0),
                reader.GetInt64(1),
                reader.GetBoolean(2),
                reader.GetBoolean(3)));

        await reader.NextResultAsync(ct);
        // Pricings
        while (await reader.ReadAsync(ct))
            pricings.Add(new PricingRow(
                reader.GetInt64(0),
                reader.GetString(1),
                reader.GetDecimal(2),
                reader.GetBoolean(3),
                reader.GetBoolean(4)));

        await reader.NextResultAsync(ct);
        // ContentRatings
        while (await reader.ReadAsync(ct))
        {
            long comicId = reader.GetInt64(0);
            contentRatings.Add(comicId, new ContentRatingRow(
                comicId,
                reader.GetInt32(1),
                reader.GetInt32(2),
                reader.IsDBNull(3) ? string.Empty : reader.GetString(3)));
        }

        await reader.NextResultAsync(ct);
        // ComicTags
        while (await reader.ReadAsync(ct))
        {
            long comicId = reader.GetInt64(0);
            if (!comicTags.ContainsKey(comicId))
            {
                comicTags.Add(comicId, new List<ComicTagRow>());
            }

            comicTags[comicId].Add(new ComicTagRow(
                comicId,
                reader.IsDBNull(1) ? string.Empty : reader.GetString(1)));
        }

        return new DodVisibilityBatch(
            comics.ToArray(),
            chapters,
            geoRules.ToArray(),
            segmentRules.ToArray(),
            pricings.ToArray(),
            contentRatings,
            comicTags);
    }
}

public record struct PricingRegionKey
{
    public long comicId;
    public string regionCode;

    public PricingRegionKey(long comicId, string regionCode)
    {
        this.comicId = comicId;
        this.regionCode = regionCode;
    }

    public static PricingRegionKey of(long comicId, string regionCode)
    {
        return new PricingRegionKey(comicId, regionCode);
    }
}