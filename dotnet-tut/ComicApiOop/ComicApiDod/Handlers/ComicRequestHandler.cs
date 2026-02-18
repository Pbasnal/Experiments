using Common.Metrics;
using ComicApiDod.Middleware;
using ComicApiDod.Services;
using System.Diagnostics;
using Common.Models;
using Common.SimpleQueue;

namespace ComicApiDod.Handlers;

public static class ComicRequestHandler
{
    private const string ProcessName = "compute_visibilities";
    private static TimeSpan processTimeout = TimeSpan.FromMilliseconds(1000);


    public static async Task<IResult> HandleComputeVisibilities(
        long startId,
        int limit,
        SimpleMessageBus bus,
        ComicVisibilityService comicVisibilityService,
        IAppMetrics metrics,
        HttpContext context)
    {
        var sw = Stopwatch.StartNew();

        // Validate input parameters upfront
        IResult? validationResult = IsRequestValid(startId, limit, metrics, sw);
        if (validationResult != null) return validationResult;

        CancellationTokenSource tknSrc = new CancellationTokenSource();

        try
        {
            return await ProcessRequest(startId, limit, bus, context, metrics, tknSrc.Token, sw);
        }
        catch (TimeoutException)
        {
            tknSrc.Cancel();
            var attrs = new Dictionary<string, string> { ["status"] = "timeout" };
            metrics.CaptureCount(ProcessName, 1, attrs);
            metrics.RecordLatency(ProcessName, sw.Elapsed.TotalSeconds, attrs);
            return Results.Problem(
                detail: "Request timed out after 10 seconds",
                title: "Request Timeout",
                statusCode: 504
            );
        }
        catch (Exception ex)
        {
            var attrs = new Dictionary<string, string> { ["status"] = "failure" };
            metrics.CaptureCount(ProcessName, 1, attrs);
            metrics.RecordLatency(ProcessName, sw.Elapsed.TotalSeconds, attrs);
            return Results.Problem(
                detail: ex.Message,
                title: "Internal Server Error",
                statusCode: 500
            );
        }
    }

    private static IResult? IsRequestValid(long startId, int limit, IAppMetrics metrics, Stopwatch sw)
    {
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

        return null;
    }

    private static async Task<IResult> ProcessRequest(
        long startId,
        int limit,
        SimpleMessageBus bus,
        HttpContext context,
        IAppMetrics metrics,
        CancellationToken tkn,
        Stopwatch sw)
    {
        VisibilityComputationRequest request = new(startId, limit);
        if (context.Items[RequestWaitTimeMiddleware.RequestReceivedAtUtcKey] is DateTime receivedAtUtc)
            request.RequestStartTimeUtc = receivedAtUtc;
        bus.Enqueue(request);

        metrics.CaptureCount(ProcessName, 1, new Dictionary<string, string> { ["status"] = "success" });

        VisibilityComputationResponse response = await request.ResponseSrc.Task
            .WaitAsync(processTimeout, tkn);

        metrics.RecordLatency(ProcessName, sw.Elapsed.TotalSeconds,
            new Dictionary<string, string> { ["status"] = "success" });
        return Results.Ok(response);
    }
}