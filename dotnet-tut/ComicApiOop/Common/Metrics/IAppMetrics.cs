namespace Common.Metrics;

/// <summary>
/// Application metrics capture: one counter and one latency histogram,
/// differentiated by process name and optional attributes (e.g. status).
/// </summary>
public interface IAppMetrics
{
    /// <summary>
    /// Executes the action, records its duration to the latency histogram, and returns the result.
    /// On exception, records with status "error" and rethrows.
    /// </summary>
    T CaptureLatency<T>(Func<T> action, string process, IReadOnlyDictionary<string, string>? attributes = null);

    /// <summary>
    /// Executes the action and records its duration. On exception, records with status "error" and rethrows.
    /// </summary>
    void CaptureLatency(Action action, string process, IReadOnlyDictionary<string, string>? attributes = null);

    /// <summary>
    /// Executes the async function, records its duration to the latency histogram, and returns the result.
    /// On exception, records with status "error" and rethrows.
    /// </summary>
    Task<T> CaptureLatencyAsync<T>(Func<Task<T>> action, string process, IReadOnlyDictionary<string, string>? attributes = null);

    /// <summary>
    /// Executes the async action and records its duration. On exception, records with status "error" and rethrows.
    /// </summary>
    Task CaptureLatencyAsync(Func<Task> action, string process, IReadOnlyDictionary<string, string>? attributes = null);

    /// <summary>
    /// Records a count to the app counter metric.
    /// </summary>
    /// <param name="process">Process name (e.g. "compute_visibilities", "db_query").</param>
    /// <param name="count">Count to add (default 1).</param>
    /// <param name="attributes">Optional labels; use "status" for success/failure (default "ok").</param>
    void CaptureCount(string process, long count = 1, IReadOnlyDictionary<string, string>? attributes = null);

    /// <summary>
    /// Records an observed duration to the latency histogram (e.g. when you measure with a stopwatch and have multiple exit points).
    /// </summary>
    /// <param name="process">Process name.</param>
    /// <param name="durationSeconds">Duration in seconds.</param>
    /// <param name="attributes">Optional labels; use "status" for success/failure (default "ok").</param>
    void RecordLatency(string process, double durationSeconds, IReadOnlyDictionary<string, string>? attributes = null);

    /// <summary>
    /// Increments a counter metric by the given value. Labels are merged with the fixed superset (missing keys = "unknown").
    /// </summary>
    /// <param name="metric">Logical metric name (e.g. "api_http_requests_total").</param>
    /// <param name="value">Amount to add (default 1).</param>
    /// <param name="labels">Optional labels; merged with superset.</param>
    void Inc(string metric, double value = 1, IReadOnlyDictionary<string, string>? labels = null);

    /// <summary>
    /// Records an observation for a histogram metric (e.g. duration in seconds). Labels are merged with the fixed superset.
    /// </summary>
    /// <param name="metric">Logical metric name (e.g. "http_request_duration_seconds").</param>
    /// <param name="valueSeconds">Observed value in seconds (or other unit as appropriate).</param>
    /// <param name="labels">Optional labels; merged with superset.</param>
    void Observe(string metric, double valueSeconds, IReadOnlyDictionary<string, string>? labels = null);

    /// <summary>
    /// Sets a gauge metric to the given value. Labels are merged with the fixed superset.
    /// </summary>
    /// <param name="metric">Logical metric name (e.g. "requests_in_batch").</param>
    /// <param name="value">Gauge value.</param>
    /// <param name="labels">Optional labels; merged with superset.</param>
    void Set(string metric, double value, IReadOnlyDictionary<string, string>? labels = null);
}
