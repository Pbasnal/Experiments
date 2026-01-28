// using ComicApiOop.Models;
// using Microsoft.EntityFrameworkCore;
// using Microsoft.EntityFrameworkCore.ChangeTracking;
//
// namespace ComicApiOop.Data;
//
// public class ComicDbContext : DbContext
// {
//     public ComicDbContext(DbContextOptions<ComicDbContext> options) : base(options)
//     {
//     }
//
//     public DbSet<ComicBook> Comics { get; set; } = null!;
//     public DbSet<Chapter> Chapters { get; set; } = null!;
//     public DbSet<Publisher> Publishers { get; set; } = null!;
//     public DbSet<Genre> Genres { get; set; } = null!;
//     public DbSet<Theme> Themes { get; set; } = null!;
//     public DbSet<ComicTag> Tags { get; set; } = null!;
//     public DbSet<GeographicRule> GeographicRules { get; set; } = null!;
//     public DbSet<CustomerSegmentRule> CustomerSegmentRules { get; set; } = null!;
//     public DbSet<CustomerSegment> CustomerSegments { get; set; } = null!;
//     public DbSet<ComputedVisibility> ComputedVisibilities { get; set; } = null!;
//     public DbSet<ContentRating> ContentRatings { get; set; } = null!;
//     public DbSet<ComicPricing> ComicPricing { get; set; } = null!;
//
//     protected override void OnModelCreating(ModelBuilder modelBuilder)
//     {
//         // Configure ComicBook entity
//         modelBuilder.Entity<ComicBook>(entity =>
//         {
//             entity.HasKey(e => e.Id);
//             entity.Property(e => e.Title).IsRequired().HasMaxLength(255);
//             entity.HasOne(e => e.Publisher)
//                 .WithMany(p => p.Comics)
//                 .HasForeignKey(e => e.PublisherId);
//             entity.HasOne(e => e.Genre)
//                 .WithMany(g => g.Comics)
//                 .HasForeignKey(e => e.GenreId);
//             entity.HasOne(e => e.Theme)
//                 .WithMany(t => t.Comics)
//                 .HasForeignKey(e => e.ThemeId);
//         });
//
//         // Configure Chapter entity
//         modelBuilder.Entity<Chapter>(entity =>
//         {
//             entity.HasKey(e => e.Id);
//             entity.HasOne(e => e.Comic)
//                 .WithMany(c => c.Chapters)
//                 .HasForeignKey(e => e.ComicId);
//         });
//
//         // Configure ComicTag entity and its many-to-many relationship with Comic
//         modelBuilder.Entity<ComicTag>(entity =>
//         {
//             entity.HasKey(e => e.Id);
//             entity.Property(e => e.Name).IsRequired().HasMaxLength(50);
//             
//             entity.HasMany(t => t.Comics)
//                 .WithMany(c => c.Tags)
//                 .UsingEntity(j => j.ToTable("ComicTag"));
//         });
//
//         // Configure CustomerSegmentRule entity
//         modelBuilder.Entity<CustomerSegmentRule>(entity =>
//         {
//             entity.HasKey(e => e.Id);
//             entity.HasOne(e => e.Comic)
//                 .WithMany(c => c.CustomerSegmentRules)
//                 .HasForeignKey(e => e.ComicId);
//             entity.HasOne(e => e.Segment)
//                 .WithMany(s => s.Rules)
//                 .HasForeignKey(e => e.SegmentId);
//         });
//
//         // Configure ComputedVisibility entity
//         modelBuilder.Entity<ComputedVisibility>(entity =>
//         {
//             entity.HasKey(e => e.Id);
//             entity.Property(e => e.CountryCode).IsRequired().HasMaxLength(2);
//             entity.Property(e => e.SearchTags).HasMaxLength(1000);
//             entity.HasOne(e => e.Comic)
//                 .WithMany()
//                 .HasForeignKey(e => e.ComicId);
//             entity.HasOne(e => e.CustomerSegment)
//                 .WithMany()
//                 .HasForeignKey(e => e.CustomerSegmentId);
//         });
//
//         // Configure ContentRating entity
//         modelBuilder.Entity<ContentRating>(entity =>
//         {
//             entity.HasKey(e => e.Id);
//             entity.Property(e => e.ContentWarning).HasMaxLength(500);
//             entity.HasOne(e => e.Comic)
//                 .WithOne(c => c.ContentRating)
//                 .HasForeignKey<ContentRating>(e => e.ComicId);
//         });
//
//         // Configure ComicPricing entity
//         modelBuilder.Entity<ComicPricing>(entity =>
//         {
//             entity.HasKey(e => e.Id);
//             entity.Property(e => e.RegionCode).IsRequired().HasMaxLength(2);
//             entity.Property(e => e.BasePrice).HasPrecision(10, 2);
//             entity.Property(e => e.DiscountPercentage).HasPrecision(5, 2);
//             entity.HasOne(e => e.Comic)
//                 .WithMany(c => c.RegionalPricing)
//                 .HasForeignKey(e => e.ComicId);
//         });
//
//         // Configure GeographicRule entity
//         modelBuilder.Entity<GeographicRule>(entity =>
//         {
//             entity.HasKey(e => e.Id);
//             entity.Property(e => e.CountryCodes)
//                 .HasConversion(
//                     v => string.Join(',', v),
//                     v => v.Split(',', StringSplitOptions.RemoveEmptyEntries).ToList(),
//                     new ValueComparer<List<string>>(
//                         (c1, c2) => c1 != null && c2 != null && c1.SequenceEqual(c2),
//                         c => c != null ? c.Aggregate(0, (a, v) => HashCode.Combine(a, v.GetHashCode())) : 0,
//                         c => c != null ? c.ToList() : new List<string>()
//                     )
//                 )
//                 .HasMaxLength(1000); // Allow for multiple country codes
//             entity.HasOne(e => e.Comic)
//                 .WithMany(c => c.RegionalAvailability)
//                 .HasForeignKey(e => e.ComicId);
//         });
//     }
// }
//
//
//
//
