---
name: ComicApiOop_Metrics_And_Cleanup
overview: Standardize ComicApiOop metrics to emit only through Common `IAppMetrics` generic `app_metric_*` series (fixed label superset, shared metric names), remove default Prometheus HTTP metrics middleware, align runtime + DB/EF + operation metrics with dashboards, and apply the equivalent cleanup items for the OOP app and its deployment files.
todos:
  - id: oop-pipeline-remove-httpmetrics
    content: Remove `app.UseHttpMetrics()` from `ComicApiOop/Extensions/ApplicationBuilderExtensions.cs`; keep `app.UseMetricServer()` so `/metrics` remains available.
    status: completed
  - id: oop-http-metrics-via-iappmetrics
    content: Ensure `ComicApiOop/Middleware/MetricsMiddleware.cs` is the sole HTTP-request metrics source (Inc/Observe with `MetricNames.ApiHttpRequestsTotal` + `MetricNames.HttpRequestDuration` and labels `endpoint`,`method`,`status`).
    status: completed
  - id: oop-db-metrics-standardize-names
    content: "Standardize OOP DB/EF query metrics to use Common names: duration = `MetricNames.DbQueryDuration` and count = `db_query_count_total` (or add a `MetricNames.DbQueryCountTotal` constant), rather than `oop_db_query_*` names, so dashboards can be consistent across OOP+DOD."
    status: completed
  - id: oop-db-metrics-add-labels
    content: Ensure OOP DB/EF metrics include `query_type` and (where possible) `table` labels; keep `status` label for counts/durations when meaningful.
    status: completed
  - id: oop-ef-change-tracker-metric
    content: Confirm/align OOP EF change tracker gauge to `metric="ef_change_tracker_entities"` with `operation` label via `IAppMetrics.Set` (used by dashboards).
    status: completed
  - id: oop-memory-per-operation-metric
    content: Enable emitting `memory_allocated_bytes_per_operation` for OOP operations (decide which operations to track and ensure `operation` label is set).
    status: completed
  - id: oop-gc-panels-verify-labels
    content: Verify OOP runtime GC count emissions include `generation` label values "0","1","2" and `api_type="OOP"`, matching GC and GC/1000req panels.
    status: completed
  - id: grafana-oop-db-ef-panels-align
    content: Update `grafana/dashboards/comic-api-performance.json` DB/EF panels to query the finalized OOP metric names/labels (especially if moving off `oop_db_query_*`).
    status: completed
  - id: grafana-oop-memory-ef-panels-verify
    content: Verify `EF Core change tracker` and `Memory per operation` panels in `comic-api-performance.json` match the emitted `metric` names and use `api_type="OOP"`.
    status: completed
  - id: prometheus-scrape-oop-metrics-endpoint
    content: Confirm Prometheus scrape config targets the OOP `/metrics` endpoint and Grafana provisioning reloads updated dashboards (restart container or re-provision if needed).
    status: completed
  - id: oop-smoke-check-metrics-output
    content: Run OOP locally or via docker-compose, hit a few endpoints, and confirm `/metrics` shows expected `app_metric_*` time series for HTTP, DB, GC, EF tracker, and memory-per-operation.
    status: completed
isProject: false
---

# ComicApiOop metrics + cleanup standardization

## Goal

- **ComicApiOop emits app metrics only via `Common.Metrics.IAppMetrics`**, i.e. `app_metric_counter`, `app_metric_histogram`/`_bucket`, `app_metric_gauge` with labels including `metric` and `api_type="OOP"`.
- **Keep `/metrics`**, but **remove built-in HTTP metrics** (`UseHttpMetrics`) so Grafana panels rely on `app_metric_`* + runtime metrics only.
- Ensure panels for **GC, GC per 1000 req, DB/EF query duration & count, EF change tracker, memory per operation** have consistent logical metric names and labels.

## Current state (what I found)

- Pipeline currently uses Prometheus HTTP metrics middleware:
  - `ComicApiOop/Extensions/ApplicationBuilderExtensions.cs` calls `app.UseMetricServer();` and `app.UseHttpMetrics();`.
- Custom HTTP metrics already exist via `IAppMetrics`:
  - `ComicApiOop/Middleware/MetricsMiddleware.cs` calls `Inc(MetricNames.ApiHttpRequestsTotal)` and `Observe(MetricNames.HttpRequestDuration)`.
- Runtime GC/memory metrics already emitted via `IAppMetrics`:
  - `ComicApiOop/Services/OopRuntimeMetricsHostedService.cs` emits `MetricNames.DotNetGcCollectionCount`, `DotNetMemoryAllocatedBytes`, `GcPauseTimeRatio`.
- OOP DB/operation tracking exists via `MetricsReporter` but needs to be made consistent with dashboard expectations (DB naming/labels, memory-per-operation enabled).

## Implementation outline (files)

- **Pipeline**: `ComicApiOop/Extensions/ApplicationBuilderExtensions.cs`
- **HTTP metrics**: `ComicApiOop/Middleware/MetricsMiddleware.cs`
- **Operation + DB/EF instrumentation**: `ComicApiOop/Services/MetricsReporter.cs`, `ComicApiOop/Services/VisibilityComputationService.cs`
- **Runtime metrics**: `ComicApiOop/Services/OopRuntimeMetricsHostedService.cs`
- **Shared schema**: `Common/Metrics/`* (`AppMetrics.cs`, `MetricLabels.cs`, `MetricNames.cs`, `IAppMetrics.cs`)
- **Dashboards**: `grafana/dashboards/comic-api-performance.json` (and optionally `performance-dashboard.json` if it mixes OOP+DOD panels)

## PromQL/label conventions to enforce

- DB query duration: `app_metric_histogram_bucket{metric="db_query_duration_seconds", api_type="OOP", query_type="...", table="..."}`
- DB query count: `app_metric_counter{metric="db_query_count_total", api_type="OOP", query_type="...", table="...", status="ok|error"}`
- EF change tracker: `app_metric_gauge{metric="ef_change_tracker_entities", api_type="OOP", operation="..."}`
- Memory per operation: `app_metric_gauge{metric="memory_allocated_bytes_per_operation", api_type="OOP", operation="..."}`
- GC count: `app_metric_counter{metric="dotnet_gc_collection_count", api_type="OOP", generation="0|1|2"}`
- Requests total: `app_metric_counter{metric="api_http_requests_total", api_type="OOP", endpoint="...", method="...", status="..."}`

## Test/verification

- Run OOP app, hit `/api/comics/compute-visibilities?...`, then confirm `/metrics` contains the `app_metric_`* series above.
- Re-provision/reload Grafana dashboards and confirm the listed panels show data.

