using ComicApiDod.Models;
using System.Diagnostics;

namespace ComicApiDod.Data;

/// <summary>
/// Bulk visibility processor using DOD principles
/// Processes all comics in a batch together for better cache locality and CPU efficiency
/// </summary>
public static class BulkVisibilityProcessor
{
    /// <summary>
    /// Process all comics in a batch together - true DOD approach
    /// This processes all comics in one pass for better cache locality
    /// </summary>
    public static BulkComputationResult ProcessBatch(
        IDictionary<long, ComicBatchData> allComicsData,
        DateTime computationTime)
    {
        var sw = Stopwatch.StartNew();
        var result = new BulkComputationResult
        {
            ComicIds = allComicsData.Keys.ToArray(),
            VisibilitiesByComic = new Dictionary<long, ComputedVisibilityData[]>(),
            ProcessingStats = new ProcessingStats()
        };

        // Process all comics in sequence - better for cache locality than parallel
        // In DOD, we want sequential processing of arrays for cache efficiency
        foreach (var (comicId, batchData) in allComicsData)
        {
            try
            {
                // Compute visibilities for this comic
                var visibilities = VisibilityProcessor.ComputeVisibilities(batchData, computationTime);
                
                result.VisibilitiesByComic[comicId] = visibilities;
                result.ProcessingStats.TotalVisibilities += visibilities.Length;
                result.ProcessingStats.SuccessCount++;
                
                if (visibilities.Length == 0)
                {
                    result.ProcessingStats.EmptyResultCount++;
                }
            }
            catch (Exception ex)
            {
                result.ProcessingStats.FailedCount++;
                result.ProcessingStats.Errors.Add(new ProcessingError
                {
                    ComicId = comicId,
                    ErrorMessage = ex.Message
                });
                
                // Store empty array for failed comics
                result.VisibilitiesByComic[comicId] = Array.Empty<ComputedVisibilityData>();
            }
        }

        result.ProcessingStats.ProcessingDuration = sw.Elapsed;
        result.ProcessingStats.AverageVisibilitiesPerComic = result.ComicIds.Length > 0
            ? (double)result.ProcessingStats.TotalVisibilities / result.ComicIds.Length
            : 0;

        return result;
    }

    /// <summary>
    /// Flatten all visibilities from batch result into a single array
    /// Useful for bulk database operations
    /// </summary>
    public static ComputedVisibilityData[] FlattenVisibilities(BulkComputationResult result)
    {
        var allVisibilities = new List<ComputedVisibilityData>();
        
        foreach (var visibilities in result.VisibilitiesByComic.Values)
        {
            allVisibilities.AddRange(visibilities);
        }
        
        return allVisibilities.ToArray();
    }
}

/// <summary>
/// Result of bulk computation processing
/// </summary>
public class BulkComputationResult
{
    public long[] ComicIds { get; set; } = Array.Empty<long>();
    public Dictionary<long, ComputedVisibilityData[]> VisibilitiesByComic { get; set; } = new();
    public ProcessingStats ProcessingStats { get; set; } = new();
}

/// <summary>
/// Statistics about the processing run
/// </summary>
public class ProcessingStats
{
    public int SuccessCount { get; set; }
    public int FailedCount { get; set; }
    public int EmptyResultCount { get; set; }
    public int TotalVisibilities { get; set; }
    public double AverageVisibilitiesPerComic { get; set; }
    public TimeSpan ProcessingDuration { get; set; }
    public List<ProcessingError> Errors { get; set; } = new();
}

/// <summary>
/// Error information for failed processing
/// </summary>
public class ProcessingError
{
    public long ComicId { get; set; }
    public string ErrorMessage { get; set; } = string.Empty;
}
