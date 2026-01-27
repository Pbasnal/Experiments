using ComicApiOop.Services;
using Prometheus;
using System.Diagnostics;

namespace ComicApiOop.Endpoints;

public static class ComicEndpoints
{
    private static readonly Histogram RequestProcessLatency = Prometheus.Metrics.CreateHistogram(
        "request_process_duration_ms_oop",
        "Time taken to process requests in the OOP API",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.01, 2, 10),
            LabelNames = new[] { "query_type", "status" }
        });

    private static readonly Counter RequestCounter = Prometheus.Metrics.CreateCounter(
        "request_count_total_oop",
        "Total number of requests in the OOP API",
        new CounterConfiguration
        {
            LabelNames = new[] { "query_type", "status" }
        });

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
            var sw = Stopwatch.StartNew();
            string status = "success";

            // Validate input parameters upfront
            if (startId < 1)
            {
                status = "bad_request";
                RequestCounter
                    .WithLabels("compute_visibilities", status)
                    .Inc();
                RequestProcessLatency
                    .WithLabels("compute_visibilities", status)
                    .Observe(sw.Elapsed.TotalSeconds);
                return Results.BadRequest("startId must be greater than 0");
            }

            if (limit < 1 || limit > 20)
            {
                status = "bad_request";
                RequestCounter
                    .WithLabels("compute_visibilities", status)
                    .Inc();
                RequestProcessLatency
                    .WithLabels("compute_visibilities", status)
                    .Observe(sw.Elapsed.TotalSeconds);
                return Results.BadRequest("limit must be between 1 and 20");
            }

            try
            {
                var result = await service.ComputeVisibilitiesBulkAsync(startId, limit);
                
                RequestCounter
                    .WithLabels("compute_visibilities", status)
                    .Inc();
                RequestProcessLatency
                    .WithLabels("compute_visibilities", status)
                    .Observe(sw.Elapsed.TotalSeconds);
                
                return Results.Ok(result);
            }
            catch (ArgumentException ex)
            {
                status = "bad_request";
                logger.LogWarning(ex, "Invalid parameters for visibility computation");
                RequestCounter
                    .WithLabels("compute_visibilities", status)
                    .Inc();
                RequestProcessLatency
                    .WithLabels("compute_visibilities", status)
                    .Observe(sw.Elapsed.TotalSeconds);
                return Results.BadRequest(ex.Message);
            }
            catch (InvalidOperationException ex)
            {
                status = "not_found";
                logger.LogWarning(ex, "No comics found for visibility computation");
                RequestCounter
                    .WithLabels("compute_visibilities", status)
                    .Inc();
                RequestProcessLatency
                    .WithLabels("compute_visibilities", status)
                    .Observe(sw.Elapsed.TotalSeconds);
                return Results.NotFound(ex.Message);
            }
            catch (Exception ex)
            {
                status = "failure";
                logger.LogError(ex, "Error during bulk visibility computation");
                RequestCounter
                    .WithLabels("compute_visibilities", status)
                    .Inc();
                RequestProcessLatency
                    .WithLabels("compute_visibilities", status)
                    .Observe(sw.Elapsed.TotalSeconds);
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
