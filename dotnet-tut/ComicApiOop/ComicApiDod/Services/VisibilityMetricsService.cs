using Prometheus;
using System.Diagnostics;

namespace ComicApiDod.Services;

/// <summary>
/// Centralized metrics service for visibility computation analysis
/// Captures detailed metrics for DOD performance analysis
/// </summary>
public class VisibilityMetricsService
{
    // Batch-level metrics
    private static readonly Histogram BatchProcessingDuration = Metrics.CreateHistogram(
        "visibility_batch_processing_duration_seconds",
        "Time to process an entire batch of comics",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.01, 2, 10),
            LabelNames = new[] { "batch_size", "status" }
        });

    private static readonly Histogram BatchSize = Metrics.CreateHistogram(
        "visibility_batch_size",
        "Number of comics processed in a batch",
        new HistogramConfiguration
        {
            Buckets = new[] { 1.0, 5.0, 10.0, 20.0, 50.0, 100.0 }
        });

    // Data fetching metrics
    private static readonly Histogram DataFetchDuration = Metrics.CreateHistogram(
        "visibility_data_fetch_duration_seconds",
        "Time to fetch all data for batch",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.001, 2, 10),
            LabelNames = new[] { "operation" }
        });

    private static readonly Counter DataFetchCount = Metrics.CreateCounter(
        "visibility_data_fetch_total",
        "Total number of data fetch operations",
        new CounterConfiguration
        {
            LabelNames = new[] { "operation", "status" }
        });

    // Computation metrics
    private static readonly Histogram ComputationDuration = Metrics.CreateHistogram(
        "visibility_computation_duration_seconds",
        "Time to compute visibilities (CPU-bound)",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.0001, 2, 10),
            LabelNames = new[] { "comic_count", "visibilities_per_comic" }
        });

    private static readonly Gauge ComputedVisibilitiesCount = Metrics.CreateGauge(
        "visibility_computed_count",
        "Number of visibilities computed",
        new[] { "comic_id" });

    private static readonly Histogram VisibilitiesPerComic = Metrics.CreateHistogram(
        "visibility_visibilities_per_comic",
        "Number of visibilities computed per comic",
        new HistogramConfiguration
        {
            Buckets = new[] { 0.0, 1.0, 5.0, 10.0, 20.0, 50.0, 100.0 }
        });

    // Save metrics
    private static readonly Histogram SaveDuration = Metrics.CreateHistogram(
        "visibility_save_duration_seconds",
        "Time to save computed visibilities",
        new HistogramConfiguration
        {
            Buckets = Histogram.ExponentialBuckets(0.001, 2, 10),
            LabelNames = new[] { "save_type", "record_count" }
        });

    private static readonly Counter SaveCount = Metrics.CreateCounter(
        "visibility_save_total",
        "Total number of save operations",
        new CounterConfiguration
        {
            LabelNames = new[] { "save_type", "status" }
        });

    // Throughput metrics
    private static readonly Gauge ProcessingThroughput = Metrics.CreateGauge(
        "visibility_processing_throughput_comics_per_second",
        "Comics processed per second");

    private static readonly Gauge VisibilityThroughput = Metrics.CreateGauge(
        "visibility_throughput_visibilities_per_second",
        "Visibilities computed per second");

    // Error metrics
    private static readonly Counter ErrorCount = Metrics.CreateCounter(
        "visibility_errors_total",
        "Total number of errors",
        new CounterConfiguration
        {
            LabelNames = new[] { "error_type", "stage" }
        });

    public void RecordBatchProcessing(int batchSize, TimeSpan duration, string status)
    {
        BatchProcessingDuration
            .WithLabels(batchSize.ToString(), status)
            .Observe(duration.TotalSeconds);
        BatchSize.Observe(batchSize);
    }

    public void RecordDataFetch(string operation, TimeSpan duration, string status = "success")
    {
        DataFetchDuration
            .WithLabels(operation)
            .Observe(duration.TotalSeconds);
        DataFetchCount
            .WithLabels(operation, status)
            .Inc();
    }

    public void RecordComputation(int comicCount, int totalVisibilities, TimeSpan duration)
    {
        var avgVisibilitiesPerComic = comicCount > 0 ? (double)totalVisibilities / comicCount : 0;
        
        ComputationDuration
            .WithLabels(comicCount.ToString(), avgVisibilitiesPerComic.ToString("F1"))
            .Observe(duration.TotalSeconds);
        
        VisibilitiesPerComic.Observe(avgVisibilitiesPerComic);
    }

    public void RecordComputedVisibilities(long comicId, int count)
    {
        ComputedVisibilitiesCount
            .WithLabels(comicId.ToString())
            .Set(count);
    }

    public void RecordSave(string saveType, int recordCount, TimeSpan duration, string status = "success")
    {
        SaveDuration
            .WithLabels(saveType, recordCount.ToString())
            .Observe(duration.TotalSeconds);
        SaveCount
            .WithLabels(saveType, status)
            .Inc();
    }

    public void RecordThroughput(int comicsProcessed, int visibilitiesComputed, TimeSpan totalDuration)
    {
        if (totalDuration.TotalSeconds > 0)
        {
            ProcessingThroughput.Set(comicsProcessed / totalDuration.TotalSeconds);
            VisibilityThroughput.Set(visibilitiesComputed / totalDuration.TotalSeconds);
        }
    }

    public void RecordError(string errorType, string stage)
    {
        ErrorCount
            .WithLabels(errorType, stage)
            .Inc();
    }
}
