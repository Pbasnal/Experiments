using ComicApiDod.Services;
using ComicApiDod.SimpleQueue;
using ComicApiDod.Handlers;
using Prometheus;

namespace ComicApiDod.Configuration;

public static class RouteConfiguration
{
    public static void ConfigureRoutes(WebApplication app)
    {
        // Configure the HTTP request pipeline
        if (app.Environment.IsDevelopment())
        {
            app.UseSwagger();
            app.UseSwaggerUI();
        }

        // Add metrics endpoint
        app.UseMetricServer();
        app.UseHttpMetrics();

        // Configure endpoints
        ConfigureComicEndpoints(app);
        ConfigureHealthEndpoint(app);
    }

    private static void ConfigureComicEndpoints(WebApplication app)
    {
        // Main endpoint: Compute visibilities using DOD approach
        app.MapGet("/api/comics/compute-visibilities", ComicRequestHandler.HandleComputeVisibilities)
            .WithName("ComputeVisibilities")
            .WithOpenApi();

        // Example endpoint using the Simple DOD Framework
        app.MapGet("/api/comics/{comicId}/async-visibility", ComicRequestHandler.HandleAsyncVisibility)
            .WithName("AsyncComicVisibility")
            .WithOpenApi();
    }

    private static void ConfigureHealthEndpoint(WebApplication app)
    {
        app.MapGet("/health", HealthRequestHandler.HandleHealthCheck)
            .WithName("HealthCheck")
            .WithOpenApi();
    }
}
