using ComicApiDod.Data;
using ComicApiDod.Services;
using ComicApiDod.Configuration;
using Common.Models;
using Common.SimpleQueue;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

// Add services
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Configure JSON serialization
builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.WriteIndented = true;
});

// Add database context factory for concurrent operations (DOD approach)
// Using factory pattern to allow concurrent operations without lifetime conflicts
var connectionString = builder.Configuration.GetConnectionString("DefaultConnection");

// Register query metrics interceptor
builder.Services.AddSingleton<QueryMetricsInterceptor>();

builder.Services.AddDbContextFactory<ComicDbContext>((serviceProvider, options) =>
{
    var serverVersion = new MySqlServerVersion(new Version(8, 0, 0));
    var interceptor = serviceProvider.GetRequiredService<QueryMetricsInterceptor>();
    
    options.UseMySql(connectionString, serverVersion,
        mysqlOptions =>
        {
            mysqlOptions.EnableRetryOnFailure(
                maxRetryCount: 5,
                maxRetryDelay: TimeSpan.FromSeconds(30),
                errorNumbersToAdd: null);
        })
        .EnableSensitiveDataLogging()  // Shows parameter values
        .LogTo(Console.WriteLine, LogLevel.Information)  // Logs all queries
        .AddInterceptors(interceptor);  // Add query metrics interceptor
});

// Add services
builder.Services.AddSingleton<VisibilityMetricsService>();
builder.Services.AddScoped<ComicVisibilityService>();

// Add Simple DOD Framework services as Singletons
builder.Services.AddSingleton<SimpleMessageBus>();

// Add hosted service for background message processing
builder.Services.AddHostedService<MessageProcessingHostedService>();

var app = builder.Build();

// Get SimpleMessageBus instance for queue registration
var messageBus = app.Services.GetRequiredService<SimpleMessageBus>();

// Configure metrics
MetricsConfiguration.ConfigureMetrics(app);

// Configure routes
RouteConfiguration.ConfigureRoutes(app);

// Apply database migrations
if (app.Environment.IsEnvironment("Docker"))
{
    app.Logger.LogInformation("Running in Docker environment. Applying database migrations...");
    try
    {
        await using (var scope = app.Services.CreateAsyncScope())
        {
            var dbFactory = scope.ServiceProvider.GetRequiredService<IDbContextFactory<ComicDbContext>>();
            await using var db = await dbFactory.CreateDbContextAsync();
            if ((await db.Database.GetPendingMigrationsAsync()).Any())
            {
                app.Logger.LogInformation("Applying pending migrations...");
                await db.Database.MigrateAsync();
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

// Here you can register your message consumers using messageBus
// Example:
// messageBus.RegisterQueue<YourRequestType>(new SimpleQueue<YourRequestType>());

app.Run();

