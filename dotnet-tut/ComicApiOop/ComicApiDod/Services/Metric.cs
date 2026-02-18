using System.Diagnostics;
using Common.Metrics;
using Common.Models;
using Prometheus;

namespace ComicApiDod.Services;

public class Metric
{
    public static void RecordReqMetrics(List<VisibilityComputationRequest?> reqs,
        Gauge RequestsInBatch,
        IAppMetrics appMetrics,
        Stopwatch sortSw)
    {
        var sortAttrs = new Dictionary<string, string> { ["status"] = "success" };
        appMetrics.RecordLatency("comic_visibility_operation_sort_requests", sortSw.Elapsed.TotalSeconds, sortAttrs);
        appMetrics.CaptureCount("comic_visibility_operation_sort_requests", 1, sortAttrs);

        RequestsInBatch.Set(reqs.Count);
        appMetrics.CaptureCount("visibility_batch_processing", 1, new Dictionary<string, string> { ["status"] = "started" });
    }
}