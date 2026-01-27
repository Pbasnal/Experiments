namespace ComicApiOop.Endpoints;

public static class HealthEndpoints
{
    public static void MapHealthEndpoints(this WebApplication app)
    {
        app.MapGet("/health", () => Results.Ok("Healthy"))
            .WithName("HealthCheck")
            .WithTags("Health")
            .WithOpenApi()
            .Produces<string>(StatusCodes.Status200OK);
    }
}
