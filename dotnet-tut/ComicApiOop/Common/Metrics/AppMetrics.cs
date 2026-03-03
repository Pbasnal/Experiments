using System.Diagnostics;
using Prometheus;

namespace Common.Metrics;

public sealed class AppMetrics : IAppMetrics
{
    private static readonly string[] LabelNames = { "process", "status" };

    private static readonly Counter OperationCount = Prometheus.Metrics.CreateCounter(
        "metric_counter",
        "Total number of operations by process and status",
        new CounterConfiguration { LabelNames = LabelNames });

    private static readonly Histogram OperationDuration = Prometheus.Metrics.CreateHistogram(
        "metric_latency",
        "Duration of operations in seconds by process and status",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.001, 2, 14),
            LabelNames = LabelNames
        });

    private static string GetStatus(IReadOnlyDictionary<string, string>? attributes) =>
        attributes?.TryGetValue("status", out var s) == true ? s : "ok";
    
    public T CaptureLatency<T>(Func<T> action, string process, IReadOnlyDictionary<string, string>? attributes = null)
    {
        var sw = Stopwatch.StartNew();
        string status;
        try
        {
            var result = action();
            status = GetStatus(attributes);
            OperationDuration.WithLabels(process, status).Observe(sw.Elapsed.TotalSeconds);
            return result;
        }
        catch
        {
            status = "error";
            OperationDuration.WithLabels(process, status).Observe(sw.Elapsed.TotalSeconds);
            throw;
        }
    }

    public void CaptureLatency(Action action, string process, IReadOnlyDictionary<string, string>? attributes = null)
    {
        CaptureLatency(() =>
        {
            action.Invoke();
            return 0;
        }, process, attributes);
    }

    public async Task<T> CaptureLatencyAsync<T>(Func<Task<T>> action,
        string process,
        IReadOnlyDictionary<string, string>? attributes = null)
    {
        var sw = Stopwatch.StartNew();
        string status;
        try
        {
            var result = await action().ConfigureAwait(false);
            status = GetStatus(attributes);
            OperationDuration.WithLabels(process, status).Observe(sw.Elapsed.TotalSeconds);
            return result;
        }
        catch
        {
            status = "error";
            OperationDuration.WithLabels(process, status).Observe(sw.Elapsed.TotalSeconds);
            throw;
        }
    }
    
    public async Task CaptureLatencyAsync(Func<Task> action, 
        string process,
        IReadOnlyDictionary<string, string>? attributes = null)
    {
        await CaptureLatencyAsync(async () =>
        {
            await action().ConfigureAwait(false);
            return 0;
        }, process, attributes).ConfigureAwait(false);
    }
    
    public void CaptureCount(string process, long count = 1, IReadOnlyDictionary<string, string>? attributes = null)
    {
        var status = GetStatus(attributes);
        OperationCount.WithLabels(process, status).Inc(count);
    }

    public void RecordLatency(string process, double durationSeconds,
        IReadOnlyDictionary<string, string>? attributes = null)
    {
        var status = GetStatus(attributes);
        OperationDuration.WithLabels(process, status).Observe(durationSeconds);
    }
}