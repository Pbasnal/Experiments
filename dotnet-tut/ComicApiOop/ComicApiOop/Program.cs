using ComicApiOop.Data;
using ComicApiOop.Models;
using Microsoft.EntityFrameworkCore;
using Prometheus;
using System.Diagnostics;
using Prometheus.DotNetRuntime;
using System.Runtime;
using System.Runtime.InteropServices;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Configure JSON serialization to handle circular references
builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.ReferenceHandler = System.Text.Json.Serialization.ReferenceHandler.IgnoreCycles;
    options.SerializerOptions.WriteIndented = true;
});

// Add database context
var connectionString = builder.Configuration.GetConnectionString("DefaultConnection");
builder.Services.AddDbContext<ComicDbContext>(options =>
{
    var serverVersion = new MySqlServerVersion(new Version(8, 0, 0));
    options.UseMySql(connectionString, serverVersion,
        mysqlOptions =>
        {
            mysqlOptions.EnableRetryOnFailure(
                maxRetryCount: 5,
                maxRetryDelay: TimeSpan.FromSeconds(30),
                errorNumbersToAdd: null);
        });
});

// Configure metrics
var metrics = new MetricPusher(new MetricPusherOptions
{
    Endpoint = "http://prometheus:9090/api/v1/write",
    Job = "comic_api"
});
metrics.Start();

// Enable collection of .NET runtime metrics
var collector = DotNetRuntimeStatsBuilder.Default().StartCollecting();

// Create custom metrics
var httpRequestDuration = Metrics.CreateHistogram(
    "http_request_duration_seconds",
    "Duration of HTTP requests in seconds",
    new HistogramConfiguration
    {
        // Buckets: 0.001, 0.0025, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10, 25, 50, 100
        // This provides better granularity for typical API response times (ms to seconds)
        Buckets = new[] { 0.001, 0.0025, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 25.0, 50.0, 100.0 },
        LabelNames = new[] { "method", "endpoint" }
    });

var dbQueryDuration = Metrics.CreateHistogram(
    "db_query_duration_seconds",
    "Duration of database queries in seconds",
    new HistogramConfiguration
    {
        Buckets = Histogram.ExponentialBuckets(0.001, 2, 10),
        LabelNames = new[] { "query_type" }
    });

var httpRequestCounter = Metrics.CreateCounter(
    "http_requests_total",
    "Total number of HTTP requests",
    new CounterConfiguration
    {
        LabelNames = new[] { "method", "endpoint", "status" }
    });

// Add memory-related metrics
var memoryAllocatedBytes = Metrics.CreateGauge(
    "dotnet_memory_allocated_bytes",
    "Total memory allocated by the application",
    new GaugeConfiguration
    {
        LabelNames = new[] { "api_type" }
    });

var memoryTotalBytes = Metrics.CreateGauge(
    "dotnet_memory_total_bytes",
    "Total memory used by the application",
    new GaugeConfiguration
    {
        LabelNames = new[] { "api_type" }
    });

var gcCollectionCount = Metrics.CreateCounter(
    "dotnet_gc_collection_count",
    "Number of garbage collections",
    new CounterConfiguration
    {
        LabelNames = new[] { "api_type", "generation" }
    });

// Periodic metric update
var timer = new System.Timers.Timer(5000); // Update every 5 seconds
timer.Elapsed += (sender, e) => 
{
    var gcInfo = GC.GetGCMemoryInfo();
    
    memoryAllocatedBytes
        .WithLabels("OOP")
        .Set(GC.GetTotalMemory(false));
    
    memoryTotalBytes
        .WithLabels("OOP")
        .Set(gcInfo.TotalAvailableMemoryBytes);
    
    gcCollectionCount
        .WithLabels("OOP", "0")
        .Inc(GC.CollectionCount(0));
    
    gcCollectionCount
        .WithLabels("OOP", "1")
        .Inc(GC.CollectionCount(1));
    
    gcCollectionCount
        .WithLabels("OOP", "2")
        .Inc(GC.CollectionCount(2));
};
timer.Start();

var app = builder.Build();

// Apply database migrations
if (app.Environment.IsEnvironment("Docker"))
{
    app.Logger.LogInformation("Running in Docker environment. Applying database migrations...");
    try
    {
        using (var scope = app.Services.CreateScope())
        {
            var db = scope.ServiceProvider.GetRequiredService<ComicDbContext>();
            if (db.Database.GetPendingMigrations().Any())
            {
                app.Logger.LogInformation("Applying pending migrations...");
                db.Database.Migrate();
                app.Logger.LogInformation("Migrations applied successfully.");
            }
            else
            {
                app.Logger.LogInformation("No pending migrations. Database is up to date.");
            }
        }
    }
    catch (Exception ex)
    {
        app.Logger.LogError(ex, "An error occurred while migrating the database.");
        throw;
    }
}

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// Only use HTTPS redirection in development
if (!app.Environment.IsEnvironment("Docker"))
{
    app.UseHttpsRedirection();
}

// Add metrics endpoint and middleware
app.UseMetricServer();
app.UseHttpMetrics();

// Add custom metrics middleware
app.Use(async (context, next) =>
{
    var path = context.Request.Path.Value;
    var method = context.Request.Method;
    var endpoint = $"{method} {path}";
    
    var sw = Stopwatch.StartNew();
    
    try
    {
        await next();
        
        // Record metrics after successful request
        httpRequestCounter
            .WithLabels(method, path, context.Response.StatusCode.ToString())
            .Inc();
            
        httpRequestDuration
            .WithLabels(method, path)
            .Observe(sw.Elapsed.TotalSeconds);
    }
    catch
    {
        // Record metrics after failed request
        httpRequestCounter
            .WithLabels(method, path, "500")
            .Inc();
            
        httpRequestDuration
            .WithLabels(method, path)
            .Observe(sw.Elapsed.TotalSeconds);
        throw;
    }
});

// Bulk visibility computation endpoint
app.MapGet("/api/comics/compute-visibilities", async (int startId, int limit, ComicDbContext db, ILogger<Program> logger) =>
{
    try
    {
        // Validate input parameters
        if (startId < 1) return Results.BadRequest("startId must be greater than 0");
        if (limit < 1) return Results.BadRequest("limit must be greater than 0");
        if (limit > 20) return Results.BadRequest("limit cannot exceed 20 comics");

        logger.LogInformation($"Computing visibility for comics starting from ID {startId}, limit {limit}");
        
        // Get requested comic IDs
        var comicIds = await db.Comics
            .Where(c => c.Id >= startId)
            .OrderBy(c => c.Id)
            .Take(limit)
            .Select(c => c.Id)
            .ToListAsync();

        if (!comicIds.Any())
        {
            return Results.NotFound($"No comics found starting from ID {startId}");
        }
        
        logger.LogInformation($"Found {comicIds.Count} comics to process");

        var result = new List<VisibilityComputationResultDto>();
        var processedCount = 0;
        var failedCount = 0;
        var startTime = DateTime.UtcNow;

        // Process comics
        foreach (var comicId in comicIds)
        {
            try
            {
                logger.LogInformation($"Processing comic ID: {comicId}");
                // Fetch comic with all required data
                var comic = await db.Comics
                    .Include(c => c.Chapters)
                    .Include(c => c.Tags)
                    .Include(c => c.Publisher)
                    .Include(c => c.Genre)
                    .Include(c => c.ContentRating)
                    .Include(c => c.RegionalPricing)
                    .FirstOrDefaultAsync(c => c.Id == comicId);

                if (comic == null)
                {
                    logger.LogWarning($"Comic ID {comicId} not found");
                    continue;
                }

                var geographicRules = await db.GeographicRules
                    .Where(r => r.ComicId == comicId)
                    .ToListAsync();

                var customerSegmentRules = await db.CustomerSegmentRules
                    .Include(r => r.Segment)
                    .Where(r => r.ComicId == comicId)
                    .ToListAsync();

                // Compute visibilities
                var computedVisibilities = new List<ComputedVisibilityDto>();
                foreach (var geoRule in geographicRules)
                {
                    foreach (var segmentRule in customerSegmentRules)
                    {
                        // First check if the comic is visible according to the rules
                        bool isVisible = geoRule.EvaluateVisibility() && segmentRule.IsVisible;

                        // Only create visibility object if the comic is visible
                        if (isVisible)
                        {
                            // Get regional pricing for this country
                            var pricing = comic.RegionalPricing
                                .FirstOrDefault(p => geoRule.CountryCodes.Contains(p.RegionCode));

                            var visibility = new ComputedVisibilityDto
                            {
                                ComicId = comicId,
                                CountryCode = geoRule.CountryCodes.First(),
                                CustomerSegmentId = segmentRule.SegmentId,
                                FreeChaptersCount = comic.Chapters.Count(c => c.IsFree),
                                LastChapterReleaseTime = comic.Chapters.Max(c => c.ReleaseTime),
                                GenreId = comic.GenreId,
                                PublisherId = comic.PublisherId,
                                AverageRating = comic.AverageRating,
                                SearchTags = string.Join(",", comic.Tags.Select(t => t.Name)),
                                IsVisible = true,
                                ComputedAt = DateTime.UtcNow,
                                LicenseType = geoRule.LicenseType,
                                CurrentPrice = pricing?.GetCurrentPrice() ?? 0m,
                                IsFreeContent = pricing?.IsFreeContent ?? false,
                                IsPremiumContent = pricing?.IsPremiumContent ?? false,
                                AgeRating = comic.ContentRating?.AgeRating ?? AgeRating.AllAges,
                                ContentFlags = DetermineContentFlags(
                                    comic.ContentRating?.ContentFlags ?? ContentFlag.None,
                                    comic.Chapters,
                                    pricing),
                                ContentWarning = comic.ContentRating?.ContentWarning ?? string.Empty
                            };
                            computedVisibilities.Add(visibility);
                        }
                    }
                }

                // Save computed visibilities to database
                var dbVisibilities = computedVisibilities.Select(cv => new ComputedVisibility
                {
                    ComicId = cv.ComicId,
                    CountryCode = cv.CountryCode,
                    CustomerSegmentId = cv.CustomerSegmentId,
                    FreeChaptersCount = cv.FreeChaptersCount,
                    LastChapterReleaseTime = cv.LastChapterReleaseTime,
                    GenreId = cv.GenreId,
                    PublisherId = cv.PublisherId,
                    AverageRating = cv.AverageRating,
                    SearchTags = cv.SearchTags,
                    IsVisible = cv.IsVisible,
                    ComputedAt = cv.ComputedAt,
                    LicenseType = cv.LicenseType,
                    CurrentPrice = cv.CurrentPrice,
                    IsFreeContent = cv.IsFreeContent,
                    IsPremiumContent = cv.IsPremiumContent,
                    AgeRating = cv.AgeRating,
                    ContentFlags = cv.ContentFlags,
                    ContentWarning = cv.ContentWarning
                }).ToList();

                await db.ComputedVisibilities.AddRangeAsync(dbVisibilities);
                await db.SaveChangesAsync();

                result.Add(new VisibilityComputationResultDto
                {
                    ComicId = comicId,
                    Success = true,
                    ComputationTime = DateTime.UtcNow,
                    ComputedVisibilities = computedVisibilities
                });

                processedCount++;
            }
            catch (Exception ex)
            {
                failedCount++;
                logger.LogError(ex, $"Error processing comic {comicId}");
                result.Add(new VisibilityComputationResultDto
                {
                    ComicId = comicId,
                    Success = false,
                    ErrorMessage = ex.Message,
                    ComputationTime = DateTime.UtcNow,
                    ComputedVisibilities = new List<ComputedVisibilityDto>()
                });
            }
        }

        var endTime = DateTime.UtcNow;
        var duration = endTime - startTime;

        logger.LogInformation($"Completed processing. Success: {processedCount}, Failed: {failedCount}, Duration: {duration.TotalSeconds}s");

        return Results.Ok(new
        {
            StartId = startId,
            Limit = limit,
            ProcessedSuccessfully = processedCount,
            Failed = failedCount,
            DurationInSeconds = duration.TotalSeconds,
            NextStartId = startId + limit,
            Results = result
        });
    }
    catch (Exception ex)
    {
        logger.LogError(ex, "Error during bulk visibility computation");
        return Results.Problem(
            detail: ex.ToString(),
            title: "Error computing visibilities",
            statusCode: 500
        );
    }
})
.WithName("ComputeVisibilities")
.WithOpenApi();

// Health check endpoint
app.MapGet("/health", () => Results.Ok("Healthy"))
    .WithName("HealthCheck")
    .WithOpenApi();

// Helper method to determine content flags based on business rules
static ContentFlag DetermineContentFlags(ContentFlag baseFlags, List<Chapter> chapters, ComicPricing? pricing)
{
    var flags = baseFlags;
    
    // Check if all chapters are free
    bool allChaptersFree = chapters.All(c => c.IsFree);
    bool hasAnyFreeChapter = chapters.Any(c => c.IsFree);
    bool hasPaidChapters = chapters.Any(c => !c.IsFree);
    
    // Add Free flag if all chapters are free
    if (allChaptersFree)
    {
        flags |= ContentFlag.Free;
    }
    
    // Add Premium flag if no free chapters
    if (!hasAnyFreeChapter)
    {
        flags |= ContentFlag.Premium;
    }
    
    // Add Freemium flag if the comic has both free and paid chapters or has a price
    if ((hasAnyFreeChapter && hasPaidChapters) || (pricing?.BasePrice > 0))
    {
        flags |= ContentFlag.Freemium;
    }
    
    return flags;
}

app.Run();