using Common.Metrics;
using ComicApiOop.Services;
using System.Diagnostics;

namespace ComicApiOop.Endpoints;

public static class ComicEndpoints
{
    private const string ProcessName = "compute_visibilities";
    /// <summary>Server-side request timeout; requests exceeding this return 504 so timeouts are visible in metrics.</summary>
    private static readonly TimeSpan ProcessTimeout = TimeSpan.FromSeconds(10);

    public static void MapComicEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/comics")
            .WithTags("Comics");

        // Bulk visibility computation endpoint
        group.MapGet("/compute-visibilities", async (
            int startId,
            int limit,
            VisibilityComputationService service,
            IAppMetrics metrics,
            ILoggerFactory loggerFactory) =>
        {
            var logger = loggerFactory.CreateLogger("ComicApiOop.Endpoints.ComicEndpoints");
            var sw = Stopwatch.StartNew();
            string status = "success";

            // Validate input parameters upfront
            if (startId < 1)
            {
                var attrs = new Dictionary<string, string> { ["status"] = "bad_request" };
                metrics.CaptureCount(ProcessName, 1, attrs);
                metrics.RecordLatency(ProcessName, sw.Elapsed.TotalSeconds, attrs);
                return Results.BadRequest("startId must be greater than 0");
            }

            if (limit < 1 || limit > 20)
            {
                var attrs = new Dictionary<string, string> { ["status"] = "bad_request" };
                metrics.CaptureCount(ProcessName, 1, attrs);
                metrics.RecordLatency(ProcessName, sw.Elapsed.TotalSeconds, attrs);
                return Results.BadRequest("limit must be between 1 and 20");
            }

            try
            {
                var workTask = service.ComputeVisibilitiesBulkAsync(startId, limit);
                var delayTask = Task.Delay(ProcessTimeout);
                var completed = await Task.WhenAny(workTask, delayTask);
                if (completed == delayTask)
                {
                    var timeoutAttrs = new Dictionary<string, string> { ["status"] = "timeout" };
                    metrics.CaptureCount(ProcessName, 1, timeoutAttrs);
                    metrics.RecordLatency(ProcessName, sw.Elapsed.TotalSeconds, timeoutAttrs);
                    return Results.Problem(
                        detail: "Request timed out after 10 seconds",
                        title: "Request Timeout",
                        statusCode: 504);
                }
                var result = await workTask;
                var attrs = new Dictionary<string, string> { ["status"] = status };
                metrics.CaptureCount(ProcessName, 1, attrs);
                metrics.RecordLatency(ProcessName, sw.Elapsed.TotalSeconds, attrs);
                return Results.Ok(result);
            }
            catch (ArgumentException ex)
            {
                status = "bad_request";
                logger.LogWarning(ex, "Invalid parameters for visibility computation");
                var attrs = new Dictionary<string, string> { ["status"] = status };
                metrics.CaptureCount(ProcessName, 1, attrs);
                metrics.RecordLatency(ProcessName, sw.Elapsed.TotalSeconds, attrs);
                return Results.BadRequest(ex.Message);
            }
            catch (InvalidOperationException ex)
            {
                status = "not_found";
                logger.LogWarning(ex, "No comics found for visibility computation");
                var attrs = new Dictionary<string, string> { ["status"] = status };
                metrics.CaptureCount(ProcessName, 1, attrs);
                metrics.RecordLatency(ProcessName, sw.Elapsed.TotalSeconds, attrs);
                return Results.NotFound(ex.Message);
            }
            catch (Exception ex)
            {
                status = "failure";
                logger.LogError(ex, "Error during bulk visibility computation");
                var attrs = new Dictionary<string, string> { ["status"] = status };
                metrics.CaptureCount(ProcessName, 1, attrs);
                metrics.RecordLatency(ProcessName, sw.Elapsed.TotalSeconds, attrs);
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
        .Produces(StatusCodes.Status504GatewayTimeout)
        .Produces(StatusCodes.Status500InternalServerError);
    }
}
