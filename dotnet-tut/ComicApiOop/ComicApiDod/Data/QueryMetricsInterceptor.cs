using System.Data.Common;
using System.Diagnostics;
using System.Text.RegularExpressions;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using Prometheus;

namespace ComicApiDod.Data;

/// <summary>
/// EF Core interceptor that automatically tracks all database queries
/// Provides automatic instrumentation without manual code changes
/// </summary>
public class QueryMetricsInterceptor : DbCommandInterceptor
{
    private static readonly Histogram QueryDuration = Metrics.CreateHistogram(
        "ef_query_duration_seconds",
        "EF Core query duration",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.001, 2, 12),
            LabelNames = new[] { "query_type", "table", "api_type" }
        });

    private static readonly Counter QueryCount = Metrics.CreateCounter(
        "ef_query_count_total",
        "Total number of EF Core queries",
        new CounterConfiguration
        {
            LabelNames = new[] { "query_type", "table", "api_type" }
        });

    public override async ValueTask<InterceptionResult<DbDataReader>> ReaderExecutingAsync(
        DbCommand command,
        CommandEventData eventData,
        InterceptionResult<DbDataReader> result,
        CancellationToken cancellationToken = default)
    {
        var sw = Stopwatch.StartNew();
        var actualResult = await base.ReaderExecutingAsync(command, eventData, result, cancellationToken);

        var queryType = ExtractQueryType(command.CommandText);
        var table = ExtractTable(command.CommandText);

        QueryDuration
            .WithLabels(queryType, table, "DOD")
            .Observe(sw.Elapsed.TotalSeconds);

        QueryCount
            .WithLabels(queryType, table, "DOD")
            .Inc();

        return actualResult;
    }

    public override InterceptionResult<DbDataReader> ReaderExecuting(
        DbCommand command,
        CommandEventData eventData,
        InterceptionResult<DbDataReader> result)
    {
        var sw = Stopwatch.StartNew();
        var actualResult = base.ReaderExecuting(command, eventData, result);

        var queryType = ExtractQueryType(command.CommandText);
        var table = ExtractTable(command.CommandText);

        QueryDuration
            .WithLabels(queryType, table, "DOD")
            .Observe(sw.Elapsed.TotalSeconds);

        QueryCount
            .WithLabels(queryType, table, "DOD")
            .Inc();

        return actualResult;
    }

    private static string ExtractQueryType(string commandText)
    {
        if (string.IsNullOrWhiteSpace(commandText))
            return "unknown";

        var upperCommand = commandText.TrimStart().ToUpperInvariant();
        
        if (upperCommand.StartsWith("SELECT", StringComparison.Ordinal))
            return "SELECT";
        if (upperCommand.StartsWith("INSERT", StringComparison.Ordinal))
            return "INSERT";
        if (upperCommand.StartsWith("UPDATE", StringComparison.Ordinal))
            return "UPDATE";
        if (upperCommand.StartsWith("DELETE", StringComparison.Ordinal))
            return "DELETE";
        if (upperCommand.StartsWith("EXEC", StringComparison.Ordinal) || 
            upperCommand.StartsWith("CALL", StringComparison.Ordinal))
            return "EXEC";

        return "OTHER";
    }

    private static string ExtractTable(string commandText)
    {
        if (string.IsNullOrWhiteSpace(commandText))
            return "unknown";

        // Try to extract table name from common SQL patterns
        var patterns = new[]
        {
            @"FROM\s+`?(\w+)`?",           // FROM table
            @"JOIN\s+`?(\w+)`?",            // JOIN table
            @"INTO\s+`?(\w+)`?",            // INSERT INTO table
            @"UPDATE\s+`?(\w+)`?",          // UPDATE table
            @"DELETE\s+FROM\s+`?(\w+)`?"     // DELETE FROM table
        };

        foreach (var pattern in patterns)
        {
            var match = Regex.Match(commandText, pattern, RegexOptions.IgnoreCase);
            if (match.Success && match.Groups.Count > 1)
            {
                return match.Groups[1].Value;
            }
        }

        return "unknown";
    }
}
