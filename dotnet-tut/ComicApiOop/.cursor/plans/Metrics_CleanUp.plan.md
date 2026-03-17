---
name: Metrics and cleanup standardization
overview: Centralize all custom metrics and pipeline configuration in Common (IAppMetrics with generic counter/histogram/gauge + fixed superset of labels), remove direct Prometheus usage from OOP and DOD, standardize middleware/pipeline layout, and apply the agreed cleanup (DB logging, message bus wiring, docker-entrypoint, doc move).
todos:
  - id: common-iappmetrics-api
    content: Extend IAppMetrics with Inc, Observe, Set; define fixed label superset in Common
    status: completed
  - id: common-appmetrics-impl
    content: Implement generic counter/histogram/gauge in AppMetrics with label merging
    status: completed
  - id: common-metric-names
    content: Add metric name constants in Common for shared names (optional)
    status: completed
  - id: oop-remove-prometheus
    content: Replace all Prometheus usage in OOP MetricsConfiguration and middleware with IAppMetrics
    status: completed
  - id: oop-runtime-metrics
    content: Move OOP GC/memory/request-wait metrics to IAppMetrics calls
    status: completed
  - id: dod-remove-prometheus-config
    content: Replace DOD MetricsConfiguration Prometheus with IAppMetrics
    status: completed
  - id: dod-delete-metric-cs
    content: Delete Metric.cs; inline RecordReqMetrics in ComicVisibilityService via IAppMetrics
    status: completed
  - id: dod-delete-visibility-metrics-service
    content: Delete VisibilityMetricsService (unused)
    status: completed
  - id: dod-comic-visibility-iapp
    content: "ComicVisibilityService: remove RequestsInBatch/RequestWaitTime Prometheus; use IAppMetrics"
    status: completed
  - id: dod-query-interceptor-iapp
    content: "QueryMetricsInterceptor: use IAppMetrics instead of direct Prometheus"
    status: completed
  - id: dod-dodsqlhelper-iapp
    content: "DodSqlHelper: remove FetchPhaseDuration; use IAppMetrics RecordLatency/Observe"
    status: completed
  - id: dod-cleanup-logging-bus
    content: "ProgramDod: remove verbose DB logging; remove unused messageBus retrieval"
    status: completed
  - id: dod-array-empty
    content: "MessageProcessingHostedService: use Array.Empty<IValue>()"
    status: completed
  - id: dod-remove-docker-entrypoint
    content: Remove ComicApiDod/docker-entrypoint.sh
    status: completed
  - id: doc-move-article
    content: Move Medium-Article-Request-Batching-Template.md to root docs/
    status: completed
  - id: standardize-pipeline
    content: "Standardize pipeline: DOD ApplicationPipeline or clear ConfigureMetrics role; OOP keep Extensions"
    status: completed
  - id: grafana-oop-dashboard
    content: Update comic-api-performance.json to use app_metric_counter, app_metric_histogram, app_metric_gauge with metric and api_type labels
    status: completed
  - id: grafana-dod-dashboard
    content: Update comic-api-dod-performance.json to use app_metric_* with metric and api_type labels
    status: completed
  - id: grafana-performance-dashboard
    content: Update performance-dashboard.json app-related panels to use app_metric_* where applicable; leave non-Comic API metrics unchanged
    status: completed
isProject: false
---

# Metrics and Cleanup Standardization Plan

## Target state

- **Common**: Single metric system. All custom metrics are declared once (generic counter, histogram, gauge with a fixed superset of labels). Emitter classes only call `IAppMetrics`; no `Metrics.Create`* in OOP or DOD.
- **OOP and DOD**: Pipeline and middleware use `IAppMetrics` only; no direct Prometheus in feature code. Middleware/pipeline layout standardized per project.
- **Cleanup**: Verbose DB logging off by default; message bus wiring simplified; `docker-entrypoint.sh` removed; article doc moved to root `docs/`.

## 1. Common: extend IAppMetrics and implement generic metrics

**1.1 Define fixed label superset**

Use a single set of label names for all metrics (missing keys filled with `"unknown"`):

`metric`, `api_type`, `type`, `operation`, `stage`, `endpoint`, `method`, `status`, `query_type`, `table`, `generation`, `batch_size`

**1.2 Extend [Common/Metrics/IAppMetrics.cs](Common/Metrics/IAppMetrics.cs)**

Add:

- `void Inc(string metric, double value = 1, IReadOnlyDictionary<string, string>? labels = null)`
- `void Observe(string metric, double valueSeconds, IReadOnlyDictionary<string, string>? labels = null)`
- `void Set(string metric, double value, IReadOnlyDictionary<string, string>? labels = null)`

Keep existing `CaptureLatency`*, `CaptureCount`, `RecordLatency`; they can be implemented via `Observe`/`Inc` with a convention (e.g. process name = metric name, status in labels).

**1.3 Implement in [Common/Metrics/AppMetrics.cs*](Common/Metrics/AppMetrics.cs)*

- Define **one** Prometheus counter, **one** histogram, **one** gauge, each with the full label superset above.
- In `Inc`/`Observe`/`Set`, merge provided labels with defaults (`unknown` for missing keys), always include `api_type` (must be passed by caller or set at app startup).
- Implement existing `RecordLatency`/`CaptureCount` by delegating to `Observe`/`Inc` so existing call sites keep working during migration.
- Optional: add a small **metric name constants** class in Common (e.g. `MetricNames.HttpRequestDuration`, `MetricNames.DbQueryDuration`) so both apps use the same logical names.

**1.4 Optional: runtime metrics in Common**

Move periodic GC/memory/threadpool updates into a Common helper (e.g. `RuntimeMetricsCollector`) that takes `IAppMetrics` and `api_type` and calls `Set`/`Inc` with the appropriate metric names and labels. Both OOP and DOD can call this from a timer. If you prefer to keep timers in each app, they still call only `IAppMetrics.Set`/`Inc` with the same metric names.

## 2. OOP: switch to IAppMetrics only

**2.1 Remove direct Prometheus from [ComicApiOop/Metrics/MetricsConfiguration.cs](ComicApiOop/Metrics/MetricsConfiguration.cs)**

- Remove all `Prometheus.Metrics.Create`* and static metric fields.
- Replace with:
  - A middleware that uses `IAppMetrics` (inject `IAppMetrics` and optional `api_type`): on each request call `Observe` for duration, `Inc` for count, with labels for method, endpoint, status; and call the unified `api_http_requests_total`-style count (same metric name, label `api_type=OOP`).
  - Periodic runtime updates (GC, memory, GC pause ratio) that call `IAppMetrics.Set`/`Inc` with the same metric names as today (e.g. `dotnet_memory_allocated_bytes`, `dotnet_gc_collection_count`, `gc_pause_time_ratio`).
- Keep `MetricPusher` and `DotNetRuntimeStatsBuilder` in OOP if desired; only *custom* metrics move to IAppMetrics.

**2.2 Update [ComicApiOop/Middleware/MetricsMiddleware.cs*](ComicApiOop/Middleware/MetricsMiddleware.cs)*

- Use only `IAppMetrics` (Observe for duration, Inc for count). Remove any direct Prometheus references.

**2.3 Update [ComicApiOop/Services/MetricsReporter.cs](ComicApiOop/Services/MetricsReporter.cs)** (if it uses static MetricsConfiguration)

- Replace any `MetricsConfiguration.`* or Prometheus calls with `IAppMetrics` (RecordLatency, Inc, Set as needed).

**2.4 Request wait time (OOP)**

- Today `RequestWaitTimeSeconds` is created in MetricsConfiguration and observed in the service. Replace with `IAppMetrics.Observe(metricName, waitSeconds, labels)` from the service; metric name defined in Common.

**2.5 Pipeline location (OOP)**

- Keep pipeline in [ComicApiOop/Extensions/ApplicationBuilderExtensions.cs](ComicApiOop/Extensions/ApplicationBuilderExtensions.cs) (`UseComicApiPipeline`). Ensure it only registers middleware that uses `IAppMetrics`. No change to file location if you want to keep “Extensions” for OOP.

## 3. DOD: switch to IAppMetrics only and simplify

**3.1 Remove direct Prometheus from [ComicApiDod/Configuration/MetricsConfiguration.cs](ComicApiDod/Configuration/MetricsConfiguration.cs)**

- Remove all `Metrics.Create`* and static metric fields.
- HTTP and runtime metrics: use `IAppMetrics` (Observe/Inc/Set) with same metric names as OOP where applicable (e.g. `http_request_duration_seconds`, `api_http_requests_total`, `dotnet_gc_collection_count`). Use labels to differentiate (e.g. `api_type=DOD`).
- Keep middleware order: stamp request time first, then custom metrics middleware that calls IAppMetrics. Timer for GC/memory/threadpool: same pattern as OOP, calling IAppMetrics.

**3.2 Delete [ComicApiDod/Services/Metric.cs*](ComicApiDod/Services/Metric.cs)*

- Inline the only usage: in [ComicApiDod/Services/ComicVisibilityService.cs](ComicApiDod/Services/ComicVisibilityService.cs), replace `Metric.RecordReqMetrics(...)` with direct `_appMetrics.RecordLatency` and `_appMetrics.CaptureCount` (and `_appMetrics.Set` for batch size gauge if you keep that). Use metric names/labels from Common.

**3.3 Delete [ComicApiDod/Services/VisibilityMetricsService.cs](ComicApiDod/Services/VisibilityMetricsService.cs)**

- It is unused (no references). Remove it. If you later want any of its metrics, add them as IAppMetrics calls at the right call sites with the same logical names/labels.

**3.4 [ComicApiDod/Services/ComicVisibilityService.cs](ComicApiDod/Services/ComicVisibilityService.cs)**

- Remove static `RequestsInBatch` and `RequestWaitTimeSeconds` Prometheus definitions.
- Replace with `_appMetrics.Set(metricName, value, labels)` for batch size and `_appMetrics.Observe(metricName, waitSeconds, labels)` for request wait time. Use Common metric names.

**3.5 [ComicApiDod/Data/QueryMetricsInterceptor.cs](ComicApiDod/Data/QueryMetricsInterceptor.cs)**

- Inject `IAppMetrics` (or a scoped wrapper). In reader executing, call `IAppMetrics.Observe` for duration and `IAppMetrics.Inc` for count with labels: `query_type`, `table`, `api_type=DOD`. Remove direct Prometheus Create*.

**3.6 [ComicApiDod/Data/DodSqlHelper.cs*](ComicApiDod/Data/DodSqlHelper.cs)*

- Remove static `FetchPhaseDuration` histogram. Inject or resolve `IAppMetrics` and call `RecordLatency` (or `Observe`) with a process name like `dod_fetch_phase` and labels (e.g. `operation=db_call` / `result_processing`).

**3.7 Standardize DOD pipeline**

- Keep [ComicApiDod/Configuration/RouteConfiguration.cs](ComicApiDod/Configuration/RouteConfiguration.cs) for Map* endpoints.
- Keep [ComicApiDod/Configuration/MetricsConfiguration.cs](ComicApiDod/Configuration/MetricsConfiguration.cs) but only for pipeline (UseMiddleware, Use) and periodic timer; no Prometheus types. Optionally rename to `ApplicationPipeline.cs` and expose `ConfigurePipeline(WebApplication app)` for clarity. Same pattern as OOP: one place that configures middleware order.

## 4. Cleanup (agreed items)

**4.1 [ComicApiDod/ProgramDod.cs](ComicApiDod/ProgramDod.cs)**

- Remove `.EnableSensitiveDataLogging()` and `.LogTo(Console.WriteLine, LogLevel.Information)` from DbContext configuration (or gate behind `if (builder.Environment.IsDevelopment())`).
- Remove the unused line that retrieves `messageBus` after build (`var messageBus = app.Services.GetRequiredService<SimpleMessageBus>();`).

**4.2 [ComicApiDod/Services/MessageProcessingHostedService.cs](ComicApiDod/Services/MessageProcessingHostedService.cs)**

- Replace `return new IValue[0];` with `return Array.Empty<IValue>();`.

**4.3 Remove [ComicApiDod/docker-entrypoint.sh](ComicApiDod/docker-entrypoint.sh)**

- Dockerfile uses `ENTRYPOINT ["dotnet", "ComicApiDod.dll"]`; script is unused.

**4.4 Move article doc**

- Move [ComicApiDod/docs/Medium-Article-Request-Batching-Template.md](ComicApiDod/docs/Medium-Article-Request-Batching-Template.md) to [docs/Medium-Article-Request-Batching-Template.md](docs/Medium-Article-Request-Batching-Template.md) (create `docs/` at repo root if needed). Remove empty `ComicApiDod/docs/` if it becomes empty.

## 5. Dependency and registration

- **Common**: No dependency on ASP.NET Core or Prometheus “HTTP” features if possible; only Prometheus-net core and the generic metric types. Expose `IAppMetrics` and `AppMetrics` as before.
- **OOP**: Register `IAppMetrics` with a way to set `api_type` (e.g. options or a factory that injects `"OOP"`). Same for DOD with `"DOD"`.
- **DOD**: Register `QueryMetricsInterceptor` with access to `IAppMetrics` (singleton is fine). Register `ComicVisibilityService` and `DodSqlHelper` with `IAppMetrics` as today.

## 6. Verification

- Run OOP and DOD; hit endpoints; confirm Prometheus scrapes the same logical metric names (with labels) as before where applicable.
- Ensure no `Metrics.Create`* or direct Prometheus references remain in ComicApiOop and ComicApiDod (except optionally in Common).
- Confirm middleware order and behavior (request time stamp, then metrics, then endpoints) for both apps.
- Confirm Grafana dashboards (comic-api-performance, comic-api-dod-performance, performance-dashboard) show data using the new `app_metric_`* queries after section 7 is done.

## 7. Grafana dashboards

After app migration, all app-emitted metrics are exposed only as **app_metric_counter**, **app_metric_histogram** (use `app_metric_histogram_bucket` for quantiles), and **app_metric_gauge**, with the logical name in the **metric** label and **api_type** (OOP / DOD). Every panel that currently uses old metric names must be updated.

**Query mapping (use when rewriting PromQL):**

- `metric_counter{process="X"}` → `app_metric_counter{metric="X", api_type="OOP"}` (or DOD)
- `metric_latency_bucket{process="X"}` → `app_metric_histogram_bucket{metric="X", api_type="OOP"}`
- `rate(dotnet_gc_collection_count{...})` → `rate(app_metric_counter{metric="dotnet_gc_collection_count", generation="...", api_type=~"DOD|OOP"}[5m])`
- `dotnet_memory_allocated_bytes`, `dotnet_memory_total_bytes`, `gc_pause_time_ratio` → `app_metric_gauge{metric="...", api_type=~"DOD|OOP"}`
- `api_http_requests_total` → `app_metric_counter{metric="api_http_requests_total", api_type=~"DOD|OOP"}`
- DOD-only: `http_requests_total_dod` → `app_metric_counter{metric="api_http_requests_total", api_type="DOD"}`; `http_request_duration_seconds_dod_bucket` → `app_metric_histogram_bucket{metric="http_request_duration_seconds", api_type="DOD"}`; `comic_visibility_request_wait_seconds_bucket` → `app_metric_histogram_bucket{metric="request_wait_time_seconds", api_type="DOD"}`; `comic_visibility_request_count_in_batch` → `app_metric_gauge{metric="requests_in_batch", api_type="DOD"}`; `ef_query`_* → `app_metric_histogram_bucket` / `app_metric_counter` with `metric="db_query_duration_seconds"` / count metric and `query_type`, `table`; `comic_visibility_fetch_batch_phase_duration_seconds_bucket` → `app_metric_histogram_bucket{metric="dod_fetch_phase_duration_seconds", api_type="DOD"}` (stage/operation in labels)
- `ef_change_tracker_entities`, `memory_allocated_bytes_per_operation`, `dotnet_threadpool`_* → `app_metric_gauge{metric="...", api_type="..."}`

**Per-dashboard scope (for todos grafana-oop-dashboard, grafana-dod-dashboard, grafana-performance-dashboard):**

- **comic-api-performance.json (OOP):** Replace all `metric_counter` / `metric_latency_bucket` and runtime/HTTP/EF panels with `app_metric_`* and correct `metric` + `api_type="OOP"` (or `api_type=~"DOD|OOP"` where both are shown).
- **comic-api-dod-performance.json (DOD):** Replace DOD-specific metrics and shared runtime metrics with `app_metric_`* and `api_type="DOD"` (or `api_type=~"DOD|OOP"` for shared panels).
- **performance-dashboard.json:** Update only panels that use metrics emitted by the Comic API apps (e.g. HTTP, .NET runtime); leave node_exporter / other scraped metrics unchanged.

## Order of work (suggested)

1. Common: extend IAppMetrics + implement generic metrics (Inc/Observe/Set + label superset).
2. OOP: migrate MetricsConfiguration and middleware to IAppMetrics; remove direct Prometheus.
3. DOD: migrate MetricsConfiguration, ComicVisibilityService, QueryMetricsInterceptor, DodSqlHelper to IAppMetrics; delete Metric.cs and VisibilityMetricsService; then cleanup (DB logging, message bus, Array.Empty, docker-entrypoint, doc move).
4. Standardize pipeline naming/location if desired (e.g. ApplicationPipeline for DOD).
5. **Grafana:** Update comic-api-performance, comic-api-dod-performance, and performance-dashboard per section 7 (todos grafana-oop-dashboard, grafana-dod-dashboard, grafana-performance-dashboard).
6. Final pass: ensure api_type and metric names are consistent and dashboards show data.

