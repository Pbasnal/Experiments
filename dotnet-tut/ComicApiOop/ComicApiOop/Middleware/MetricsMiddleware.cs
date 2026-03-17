using Common.Metrics;
using System.Diagnostics;

namespace ComicApiOop.Middleware;

public class MetricsMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<MetricsMiddleware> _logger;
    private readonly IAppMetrics _appMetrics;

    public MetricsMiddleware(RequestDelegate next, ILogger<MetricsMiddleware> logger, IAppMetrics appMetrics)
    {
        _next = next;
        _logger = logger;
        _appMetrics = appMetrics;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        var sw = Stopwatch.StartNew();
        string status;
        var path = context.Request.Path.Value ?? "/";
        var method = context.Request.Method;

        try
        {
            await _next(context);
            status = context.Response.StatusCode.ToString();
        }
        catch
        {
            status = "500";
            var labels = new Dictionary<string, string>
            {
                ["status"] = status,
                ["endpoint"] = path,
                ["method"] = method
            };
            _appMetrics.Inc(MetricNames.ApiHttpRequestsTotal, 1, labels);
            _appMetrics.Observe(MetricNames.HttpRequestDuration, sw.Elapsed.TotalSeconds, labels);
            throw;
        }

        var okLabels = new Dictionary<string, string>
        {
            ["status"] = status,
            ["endpoint"] = path,
            ["method"] = method
        };
        _appMetrics.Inc(MetricNames.ApiHttpRequestsTotal, 1, okLabels);
        _appMetrics.Observe(MetricNames.HttpRequestDuration, sw.Elapsed.TotalSeconds, okLabels);
    }
}
