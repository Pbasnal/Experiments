namespace Common.Metrics;

/// <summary>
/// Fixed label superset for all metrics. Missing keys are filled with <see cref="DefaultValue"/>.
/// </summary>
public static class MetricLabels
{
    /// <summary>Value used for any label not supplied by the caller.</summary>
    public const string DefaultValue = "unknown";

    /// <summary>
    /// Ordered label names for all metrics (counter, histogram, gauge).
    /// Order must match the values array passed to Prometheus .WithLabels().
    /// </summary>
    public static readonly string[] OrderedNames =
    {
        "metric",
        "api_type",
        "type",
        "operation",
        "stage",
        "endpoint",
        "method",
        "status",
        "query_type",
        "table",
        "generation",
        "batch_size"
    };
}
