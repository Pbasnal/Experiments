using ComicApiDod.Services;
using ComicApiDod.SimpleQueue;
using ComicApiDod.Models;
using Prometheus;
using System.Diagnostics;

namespace ComicApiDod.Handlers;

public static class ComicRequestHandler
{
    private static readonly Histogram DatabaseQueryDuration = Metrics.CreateHistogram(
        "database_query_duration_seconds_dod",
        "Duration of database queries in the DOD API",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.01, 2, 10),
            LabelNames = new[] { "query_type", "status" }
        });

    private static readonly Counter DatabaseQueryCounter = Metrics.CreateCounter(
        "database_queries_total_dod",
        "Total number of database queries in the DOD API",
        new CounterConfiguration
        {
            LabelNames = new[] { "query_type", "status" }
        });

    public static async Task<IResult> HandleComputeVisibilities(
        long startId,
        int limit,
        ComicVisibilityService comicVisibilityService)
    {
        var sw = Stopwatch.StartNew();
        string status = "success";

        try
        {
            var result = await comicVisibilityService.ComputeVisibilitiesAsync(startId, limit);
            
            DatabaseQueryCounter
                .WithLabels("compute_visibilities", status)
                .Inc();
            
            DatabaseQueryDuration
                .WithLabels("compute_visibilities", status)
                .Observe(sw.Elapsed.TotalSeconds);

            return Results.Ok(result);
        }
        catch
        {
            status = "failure";
            DatabaseQueryCounter
                .WithLabels("compute_visibilities", status)
                .Inc();
            
            DatabaseQueryDuration
                .WithLabels("compute_visibilities", status)
                .Observe(sw.Elapsed.TotalSeconds);
            
            throw;
        }
    }

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
