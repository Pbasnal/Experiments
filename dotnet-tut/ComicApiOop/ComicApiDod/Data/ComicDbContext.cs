using ComicApiDod.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.ChangeTracking;

namespace ComicApiDod.Data;

/// <summary>
/// Simple EF Core context for database access
/// In DOD, we keep the DbContext minimal and use it only for data retrieval
/// </summary>
public class ComicDbContext : DbContext
{
    public ComicDbContext(DbContextOptions<ComicDbContext> options) : base(options)
    {
    }

    // Entity Framework entities (these will be converted to data structures)
    public DbSet<ComicBook> Comics { get; set; } = null!;
    public DbSet<Chapter> Chapters { get; set; } = null!;
    public DbSet<Publisher> Publishers { get; set; } = null!;
    public DbSet<Genre> Genres { get; set; } = null!;
    public DbSet<Theme> Themes { get; set; } = null!;
    public DbSet<ComicTag> ComicTags { get; set; } = null!;
    public DbSet<Tag> Tags { get; set; } = null!;
    public DbSet<GeographicRule> GeographicRules { get; set; } = null!;
    public DbSet<CustomerSegmentRule> CustomerSegmentRules { get; set; } = null!;
    public DbSet<CustomerSegment> CustomerSegments { get; set; } = null!;
    public DbSet<ContentRating> ContentRatings { get; set; } = null!;
    public DbSet<ComicPricing> ComicPricings { get; set; } = null!;
    public DbSet<ComputedVisibility> ComputedVisibilities { get; set; } = null!;

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        // Configure entities for EF Core
        modelBuilder.Entity<ComicBook>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Title).IsRequired().HasMaxLength(255);
        });

        modelBuilder.Entity<Chapter>(entity => { entity.HasKey(e => e.Id); });

        modelBuilder.Entity<ComicTag>(entity =>
        {
            entity.HasKey(e => e.ComicsId);
            entity.Property(e => e.TagsId).IsRequired();
        });
        
        modelBuilder.Entity<Tag>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Name).IsRequired().HasMaxLength(50);
        });

        modelBuilder.Entity<ContentRating>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.ContentWarning).HasMaxLength(500);
        });

        modelBuilder.Entity<ComicPricing>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.RegionCode).IsRequired().HasMaxLength(2);
            entity.Property(e => e.BasePrice).HasPrecision(10, 2);
            entity.Property(e => e.DiscountPercentage).HasPrecision(5, 2);
        });

        modelBuilder.Entity<GeographicRule>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.CountryCodes)
                .HasConversion(
                    v => string.Join(',', v),
                    v => v.Split(',', StringSplitOptions.RemoveEmptyEntries).ToList(),
                    new ValueComparer<List<string>>(
                        (c1, c2) => c1!.SequenceEqual(c2!),
                        c => c.Aggregate(0, (a, v) => HashCode.Combine(a, v.GetHashCode())),
                        c => c.ToList()
                    )
                )
                .HasMaxLength(1000);
        });

        modelBuilder.Entity<CustomerSegmentRule>(entity => { entity.HasKey(e => e.Id); });

        modelBuilder.Entity<CustomerSegment>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Name).IsRequired().HasMaxLength(100);
        });

        modelBuilder.Entity<ComputedVisibility>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.CountryCode).IsRequired().HasMaxLength(2);
            entity.Property(e => e.SearchTags).HasMaxLength(1000);
        });
    }
}

/// <summary>
/// Simple entity classes for EF Core (these are internal to the Data layer)
/// </summary>
public class ComicBook
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

public class Chapter
{
    public long Id { get; set; }
    public long ComicId { get; set; }
    public int ChapterNumber { get; set; }
    public DateTime ReleaseTime { get; set; }
    public bool IsFree { get; set; }
}

public class Publisher
{
    public long Id { get; set; }
    public string Name { get; set; } = string.Empty;
}

public class Genre
{
    public long Id { get; set; }
    public string Name { get; set; } = string.Empty;
}

public class Theme
{
    public long Id { get; set; }
    public string Name { get; set; } = string.Empty;
}

public class ComicTag
{
    public long ComicsId { get; set; }
    public long TagsId { get; set; }
}

public class Tag
{
    public long Id { get; set; }
    public string Name { get; set; } = string.Empty;
}

public class ContentRating
{
    public long Id { get; set; }
    public long ComicId { get; set; }
    public AgeRating AgeRating { get; set; }
    public ContentFlag ContentFlags { get; set; }
    public string ContentWarning { get; set; } = string.Empty;
    public bool RequiresParentalGuidance { get; set; }
}

public class ComicPricing
{
    public long Id { get; set; }
    public long ComicId { get; set; }
    public string RegionCode { get; set; } = string.Empty;
    public decimal BasePrice { get; set; }
    public bool IsFreeContent { get; set; }
    public bool IsPremiumContent { get; set; }
    public DateTime? DiscountStartDate { get; set; }
    public DateTime? DiscountEndDate { get; set; }
    public decimal? DiscountPercentage { get; set; }
}

public class GeographicRule
{
    public long Id { get; set; }
    public long ComicId { get; set; }
    public List<string> CountryCodes { get; set; } = new();
    public DateTime LicenseStartDate { get; set; }
    public DateTime LicenseEndDate { get; set; }
    public LicenseType LicenseType { get; set; }
    public bool IsVisible { get; set; }
    public DateTime LastUpdated { get; set; }
}

public class CustomerSegment
{
    public long Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public bool IsPremium { get; set; }
    public bool IsActive { get; set; }
}

public class CustomerSegmentRule
{
    public long Id { get; set; }
    public long ComicId { get; set; }
    public long SegmentId { get; set; }
    public bool IsVisible { get; set; }
    public DateTime LastUpdated { get; set; }
}

public class ComputedVisibility
{
    public long Id { get; set; }
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