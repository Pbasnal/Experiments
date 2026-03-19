using System.Diagnostics;
using Prometheus;

namespace Common.Metrics;

public sealed class AppMetrics : IAppMetrics
{
    private readonly string _apiType;
    private static readonly string[] LabelNames = MetricLabels.OrderedNames;

    private static readonly Counter GenericCounter = Prometheus.Metrics.CreateCounter(
        "app_metric_counter",
        "Total count by metric and labels (fixed superset)",
        new CounterConfiguration { LabelNames = LabelNames });

    private static readonly Histogram GenericHistogram = Prometheus.Metrics.CreateHistogram(
        "app_metric_histogram",
        "Observed values (e.g. duration in seconds) by metric and labels",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.001, 2, 14),
            LabelNames = LabelNames
        });

    private static readonly Gauge GenericGauge = Prometheus.Metrics.CreateGauge(
        "app_metric_gauge",
        "Current value by metric and labels (fixed superset)",
        new GaugeConfiguration { LabelNames = LabelNames });

    public AppMetrics(string? apiType = null)
    {
        _apiType = apiType ?? MetricLabels.DefaultValue;
    }

    private string[] GetOrderedValues(string? metricName, IReadOnlyDictionary<string, string>? labels)
    {
        var merged = new Dictionary<string, string>(StringComparer.Ordinal);
        foreach (var name in LabelNames)
            merged[name] = MetricLabels.DefaultValue;
        merged["api_type"] = _apiType;
        if (metricName != null)
            merged["metric"] = metricName;
        if (labels != null)
        {
            foreach (var kv in labels)
                if (merged.ContainsKey(kv.Key))
                    merged[kv.Key] = kv.Value;
        }
        var ordered = new string[LabelNames.Length];
        for (var i = 0; i < LabelNames.Length; i++)
            ordered[i] = merged[LabelNames[i]];
        return ordered;
    }

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
            var labels = attributes != null ? new Dictionary<string, string>(attributes) : new Dictionary<string, string>();
            labels["status"] = status;
            Observe(process, sw.Elapsed.TotalSeconds, labels);
            return result;
        }
        catch
        {
            status = "error";
            var labels = attributes != null ? new Dictionary<string, string>(attributes) : new Dictionary<string, string>();
            labels["status"] = status;
            Observe(process, sw.Elapsed.TotalSeconds, labels);
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
            var labels = attributes != null ? new Dictionary<string, string>(attributes) : new Dictionary<string, string>();
            labels["status"] = status;
            Observe(process, sw.Elapsed.TotalSeconds, labels);
            return result;
        }
        catch
        {
            status = "error";
            var labels = attributes != null ? new Dictionary<string, string>(attributes) : new Dictionary<string, string>();
            labels["status"] = status;
            Observe(process, sw.Elapsed.TotalSeconds, labels);
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
        Inc(process, count, attributes);
    }

    public void RecordLatency(string process, double durationSeconds,
        IReadOnlyDictionary<string, string>? attributes = null)
    {
        Observe(process, durationSeconds, attributes);
    }

    public void Inc(string metric, double value = 1, IReadOnlyDictionary<string, string>? labels = null)
    {
        var values = GetOrderedValues(metric, labels);
        GenericCounter.WithLabels(values).Inc(value);
    }

    public void Observe(string metric, double valueSeconds, IReadOnlyDictionary<string, string>? labels = null)
    {
        var values = GetOrderedValues(metric, labels);
        GenericHistogram.WithLabels(values).Observe(valueSeconds);
    }

    public void Set(string metric, double value, IReadOnlyDictionary<string, string>? labels = null)
    {
        var values = GetOrderedValues(metric, labels);
        GenericGauge.WithLabels(values).Set(value);
    }
}
