using ComicApiDod.Models;
using ComicApiDod.Services;
using ComicApiDod.SimpleQueue;
using Prometheus;
using System.Diagnostics;

namespace ComicApiDod.Handlers;

public static class ComicRequestHandler
{
    private static readonly Histogram RequestProcessLatency = Metrics.CreateHistogram(
        "request_process_duration_ms_dod",
        "Time taken to process requests in the DOD API",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.01, 2, 10),
            LabelNames = new[] { "query_type", "status" }
        });

    private static readonly Counter RequestCounter = Metrics.CreateCounter(
        "request_count_total_dod",
        "Total number of requests in the DOD API",
        new CounterConfiguration
        {
            LabelNames = new[] { "query_type", "status" }
        });

    public static async Task<IResult> HandleComputeVisibilities(
        long startId,
        int limit,
        SimpleMessageBus bus,
        ComicVisibilityService comicVisibilityService)
    {
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

        CancellationTokenSource tknSrc = new CancellationTokenSource();
        CancellationToken tkn = tknSrc.Token;

        try
        {
            VisibilityComputationRequest request = new(startId, limit);
            bus.Enqueue(request);

            RequestCounter
                .WithLabels("compute_visibilities", status)
                .Inc();

            // Await the TCS directly - no polling, no map lookup!
            VisibilityComputationResponse response = await request.ResponseSrc.Task
                .WaitAsync(TimeSpan.FromMilliseconds(10000), tknSrc.Token); // Built-in timeout support

            RequestProcessLatency
                .WithLabels("compute_visibilities", status)
                .Observe(sw.Elapsed.TotalSeconds);

            return Results.Ok(response);
        }
        catch (TimeoutException ex)
        {
            tknSrc.Cancel();
            status = "timeout";
            RequestCounter
                .WithLabels("compute_visibilities", status)
                .Inc();
            RequestProcessLatency
                .WithLabels("compute_visibilities", status)
                .Observe(sw.Elapsed.TotalSeconds);
            return Results.Problem(
                detail: "Request timed out after 10 seconds",
                title: "Request Timeout",
                statusCode: 504
            );
        }
        catch (Exception ex)
        {
            status = "failure";
            RequestCounter
                .WithLabels("compute_visibilities", status)
                .Inc();

            RequestProcessLatency
                .WithLabels("compute_visibilities", status)
                .Observe(sw.Elapsed.TotalSeconds);

            return Results.Problem(
                detail: ex.Message,
                title: "Internal Server Error",
                statusCode: 500
            );
        }
    }

    //public static async Task<IResult> HandleComputeVisibilities(
    //    long startId,
    //    int limit,
    //    SimpleMessageBus bus,
    //    ComicVisibilityService comicVisibilityService)
    //{
    //    var sw = Stopwatch.StartNew();
    //    string status = "success";

    //    try
    //    {
    //        bus.Enqueue(new ComputeVisibilityRequest(startId, limit));
    //        var result = await comicVisibilityService.ComputeVisibilitiesAsync(startId, limit);

    //        DatabaseQueryCounter
    //            .WithLabels("compute_visibilities", status)
    //            .Inc();

    //        DatabaseQueryDuration
    //            .WithLabels("compute_visibilities", status)
    //            .Observe(sw.Elapsed.TotalSeconds);

    //        return Results.Ok(result);
    //    }
    //    catch
    //    {
    //        status = "failure";
    //        DatabaseQueryCounter
    //            .WithLabels("compute_visibilities", status)
    //            .Inc();

    //        DatabaseQueryDuration
    //            .WithLabels("compute_visibilities", status)
    //            .Observe(sw.Elapsed.TotalSeconds);

    //        throw;
    //    }
    //}
}