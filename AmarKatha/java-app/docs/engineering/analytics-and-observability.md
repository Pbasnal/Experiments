# Analytics and Observability

V0 validates **product learning**, not business LTV. Metrics stay **in-house** (PostgreSQL) with **Grafana** dashboards — no PostHog/Plausible in V0.

Primary metric: **reader return after skip** within 7 days of stated `next_expected_at`.

---

## Design principles

1. **Append-only events** — cheap writes, batch aggregation for reads
2. **No PII in analytics** — `reader_id` cookie only, no link to user account in V0
3. **Grafana for visualization** — SQL datasource or Prometheus metrics exported from app
4. **Product metrics ≠ infra metrics** — both feed Grafana, different panels

---

## Event schema

### Table: `analytics_event`

| Column | Type | Notes |
|--------|------|-------|
| `id` | bigint | PK, serial |
| `type` | enum | `CHAPTER_VIEW`, `SERIES_VIEW` |
| `series_id` | uuid | Required |
| `chapter_id` | uuid | Nullable for series view |
| `reader_id` | varchar(36) | From cookie |
| `referrer` | varchar | `share`, `homepage`, `direct`, `external` |
| `occurred_at` | timestamptz | UTC |

Index: `(series_id, reader_id, occurred_at)`.

### Table: `schedule_event`

Audit log for skips (see [scheduling-engine.md](./scheduling-engine.md)). Used to anchor return-after-skip windows.

---

## Ingestion

### Chapter view

Trigger: React reader loads chapter (fire once per session per chapter — client dedupe + server idempotency key optional).

```
POST /api/reader/v1/events/chapter-view
{
  "seriesSlug": "...",
  "chapterSlug": "...",
  "referrer": "share"
}
```

Server sets `reader_id` cookie if absent; inserts row.

### Series view

Trigger: Series landing page load.

### Referrer classification

| Source | `referrer` value |
|--------|------------------|
| Homepage link | `homepage` |
| Creator copy link / off-site | `share` |
| No referrer | `direct` |
| Other HTTP referrer | `external` |

Use `document.referrer` + UTM params (`?ref=ig`) if creators adopt them.

---

## Reader return after skip (primary KPI)

### Definition

For each `ScheduleEvent(SKIP)` on series S at time T_skip:

1. **Cohort:** distinct `reader_id` who had `CHAPTER_VIEW` on S in the **14 days before** T_skip
2. **Return:** same `reader_id` views any chapter of S within **7 days after** new `next_expected_at`
3. **Rate:** |returned| / |cohort|

### SQL sketch (materialized view or Grafana query)

```sql
-- Conceptual; refine at implementation
WITH skip AS (
  SELECT series_id, created_at AS skipped_at, /* next_expected_at after skip */ ...
  FROM schedule_event WHERE type = 'SKIP'
),
cohort AS (
  SELECT DISTINCT ae.series_id, ae.reader_id, s.skipped_at
  FROM analytics_event ae
  JOIN skip s ON s.series_id = ae.series_id
  WHERE ae.type = 'CHAPTER_VIEW'
    AND ae.occurred_at BETWEEN s.skipped_at - interval '14 days' AND s.skipped_at
)
SELECT ...
```

Run as nightly batch or on-demand in Grafana — V0 does not need real-time.

### Target

**≥40%** return rate (from [mvp-goal.md](../product/mvp-goal.md)).

---

## Creator metrics

| Metric | Query basis |
|--------|-------------|
| Active creators (≥2 chapters) | `chapter` count group by creator |
| Series using skip/hiatus | `schedule_event` distinct `series_id` |
| Creators with cadence set | `series.cadence != OFF` |
| Creator 30-day retention | Last upload timestamp vs signup |
| Share-link vs homepage reads | `referrer` on `CHAPTER_VIEW` |

---

## Grafana setup

### Stack on Azure VM (Docker Compose)

```yaml
services:
  prometheus:
    image: prom/prometheus
  grafana:
    image: grafana/grafana
    ports: ["3000:3000"]
```

### Data sources

| Source | Use |
|--------|-----|
| **PostgreSQL** | Product metrics (views, return-after-skip, creator counts) |
| **Prometheus** | JVM, HTTP latency, coalescing batch sizes, WebP queue depth |

### V0 dashboards

| Dashboard | Panels |
|-----------|--------|
| **Validation KPIs** | Return-after-skip %, active creators, skip adoption % |
| **Traffic** | Daily chapter views, share vs homepage ratio |
| **Creator health** | Uploads/week, series on hiatus count |
| **Ops** | JVM heap, DB connections, blob upload errors, WebP backlog |

Secure Grafana admin behind VPN or strong password; not public internet in V0.

### Spring integration

- Micrometer → Prometheus endpoint `/actuator/prometheus`
- Custom counters: `amarkatha.chapter.views.total`, `amarkatha.schedule.skips.total`
- Coalescing metrics: batch size histogram, queue wait time

---

## Admin portal metrics

Lightweight summary on `/admin` — optional if Grafana is primary:

- Total chapter views (7d)
- Active creators
- Pending content reports

Deep analysis in Grafana only for V0.

---

## Privacy and retention

| Policy | V0 |
|--------|-----|
| `reader_id` | Random UUID, no cross-site tracking |
| Retention | Keep raw events 90 days; aggregate after validation window |
| Export | SQL dump for offline analysis |

---

## What we do not build in V0

- Dwell time / scroll depth (V1 trending input)
- Per-creator analytics UI (Grafana + admin SQL sufficient)
- A/B testing framework
- Third-party analytics SDK

---

## Related documents

- [scheduling-engine.md](./scheduling-engine.md) — skip events
- [identity-and-auth.md](./identity-and-auth.md) — `reader_id` cookie
- [scope-and-timeline.md](./scope-and-timeline.md) — analytics in week 4
