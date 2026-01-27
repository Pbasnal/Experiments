using ComicApiOop.Services;

namespace ComicApiOop.Endpoints;

public static class ComicEndpoints
{
    public static void MapComicEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/comics")
            .WithTags("Comics");

        // Bulk visibility computation endpoint
        group.MapGet("/compute-visibilities", async (
            int startId,
            int limit,
            VisibilityComputationService service,
            ILoggerFactory loggerFactory) =>
        {
            var logger = loggerFactory.CreateLogger("ComicApiOop.Endpoints.ComicEndpoints");
            try
            {
                var result = await service.ComputeVisibilitiesBulkAsync(startId, limit);
                return Results.Ok(result);
            }
            catch (ArgumentException ex)
            {
                logger.LogWarning(ex, "Invalid parameters for visibility computation");
                return Results.BadRequest(ex.Message);
            }
            catch (InvalidOperationException ex)
            {
                logger.LogWarning(ex, "No comics found for visibility computation");
                return Results.NotFound(ex.Message);
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
        .WithOpenApi()
        .Produces<BulkVisibilityComputationResult>(StatusCodes.Status200OK)
        .Produces(StatusCodes.Status400BadRequest)
        .Produces(StatusCodes.Status404NotFound)
        .Produces(StatusCodes.Status500InternalServerError);
    }
}
