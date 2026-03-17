using System.Data.Common;
using System.Diagnostics;
using System.Text.RegularExpressions;
using Common.Metrics;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;

namespace ComicApiDod.Data;

/// <summary>
/// EF Core interceptor that automatically tracks all database queries
/// Provides automatic instrumentation without manual code changes
/// </summary>
public class QueryMetricsInterceptor : DbCommandInterceptor
{
    private readonly IAppMetrics _metrics;

    public QueryMetricsInterceptor(IAppMetrics metrics)
    {
        _metrics = metrics;
    }

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
        var labels = new Dictionary<string, string>
        {
            ["query_type"] = queryType,
            ["table"] = table
        };
        _metrics.Observe("ef_query_duration_seconds", sw.Elapsed.TotalSeconds, labels);
        _metrics.Inc("ef_query_count_total", 1, labels);

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
        var labels = new Dictionary<string, string>
        {
            ["query_type"] = queryType,
            ["table"] = table
        };
        _metrics.Observe("ef_query_duration_seconds", sw.Elapsed.TotalSeconds, labels);
        _metrics.Inc("ef_query_count_total", 1, labels);

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
