using System.Diagnostics;
using Common.Models;
using Prometheus;

namespace ComicApiDod.Services;

public class Metric
{
    public static void RecordReqMetrics(List<VisibilityComputationRequest?> reqs,
        Histogram OperationDuration,
        Counter OperationCounter,
        Gauge RequestsInBatch,
        VisibilityMetricsService _metrics,
        Stopwatch sortSw)
    {
        
        OperationDuration
            .WithLabels("sort_requests", "success")
            .Observe(sortSw.Elapsed.TotalSeconds);
        OperationCounter
            .WithLabels("sort_requests", "success")
            .Inc();

        RequestsInBatch.Set(reqs.Count);
        _metrics.RecordBatchProcessing(reqs.Count, TimeSpan.Zero, "started");

    }
}