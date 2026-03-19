using ComicApiDod.Handlers;

namespace ComicApiDod.Configuration;

public static class RouteConfiguration
{
    public static void ConfigureRoutes(WebApplication app)
    {
        // Configure endpoints
        ConfigureComicEndpoints(app);
        ConfigureHealthEndpoint(app);
    }

    private static void ConfigureComicEndpoints(WebApplication app)
    {
        app.MapGet("/api/comics/compute-visibilities", ComicRequestHandler.HandleComputeVisibilities)
            .WithName("ComputeVisibilities")
            .WithOpenApi();
    }

    private static void ConfigureHealthEndpoint(WebApplication app)
    {
        app.MapGet("/health", HealthRequestHandler.HandleHealthCheck)
            .WithName("HealthCheck")
            .WithOpenApi();
    }
}
