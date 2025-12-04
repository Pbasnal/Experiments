using ComicApiDod.Models;
using ComicApiDod.Services;
using ComicApiDod.SimpleQueue;
using Prometheus;
using System.Diagnostics;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;

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
        SimpleMap simpleMap,
        ComicVisibilityService comicVisibilityService)
    {
        var sw = Stopwatch.StartNew();
        string status = "success";

        CancellationTokenSource tknSrc = new CancellationTokenSource();
        CancellationToken tkn = tknSrc.Token;

        try
        {
            VisibilityComputationRequest request = new(startId, limit);
            bus.Enqueue(request);
            int numberOfResponses = 0;
            VisibilityComputationResponse response;

            int timeout = 10000;
            Task.Delay(timeout, tkn);
            
            RequestCounter
                .WithLabels("compute_visibilities", status)
                .Inc();
            
            Task<VisibilityComputationResponse> responseTask = WaitForResponse(simpleMap, request, tkn);
            if (await Task.WhenAny(responseTask, Task.Delay(timeout)) == responseTask)
            {
                response = responseTask.Result;
            }
            else
            {
                status = "timeout";
                tknSrc.Cancel();
                response = null;
            }
            
            RequestProcessLatency
                .WithLabels("compute_visibilities", status)
                .Observe(sw.Elapsed.TotalSeconds);
            if (status == "timeout")
            {
                return Results.Problem();
            }

            return Results.Ok(response);
        }
        catch
        {
            status = "failure";
            RequestCounter
                .WithLabels("compute_visibilities", status)
                .Inc();

            RequestProcessLatency
                .WithLabels("compute_visibilities", status)
                .Observe(sw.Elapsed.TotalSeconds);

            throw;
        }
    }

    private static async Task<VisibilityComputationResponse> WaitForResponse(SimpleMap simpleMap,
        VisibilityComputationRequest request, CancellationToken tkn)
    {
        VisibilityComputationResponse? response = null;
        while (!tkn.IsCancellationRequested && !simpleMap.Find(request.Id, out response))
        {
            await Task.Delay(10);
        }

        return response;
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

    public static async Task<IResult> HandleAsyncVisibility(
        long comicId,
        string? region,
        string? segment,
        SimpleMessageBus messageBus,
        SimpleMap map)
    {
        // Generate a unique request ID
        var requestId = Guid.NewGuid().GetHashCode();

        // Create the request
        var request = new ComicRequest
        {
            RequestId = requestId,
            ComicId = comicId,
            Region = region,
            CustomerSegment = segment
        };

        // Enqueue the request for processing
        messageBus.Enqueue(request);

        // Poll for the response (with timeout)
        var timeout = DateTime.UtcNow.AddSeconds(5);
        while (DateTime.UtcNow < timeout)
        {
            if (map.Find<ComicResponse>(requestId, out var response))
            {
                // Remove the response from the map to free memory
                map.Remove(requestId);

                // Return the response
                return Results.Ok(new ComicResponseDto
                {
                    ComicId = response!.ComicId,
                    IsVisible = response.IsVisible,
                    CurrentPrice = response.CurrentPrice,
                    ContentFlags = response.ContentFlags,
                    ProcessedAt = response.ProcessedAt
                });
            }

            // Wait a bit before checking again
            await Task.Delay(10);
        }

        return Results.StatusCode(408); // Request Timeout
    }
}