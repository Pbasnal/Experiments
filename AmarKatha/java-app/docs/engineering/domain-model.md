# Domain Model

## Hierarchy

```
User
  └── Series (1..n, max 5 ONGOING)
        └── Chapter (1..n)
              └── ChapterPage (1..n, typically 20–30)
```

**One-shot comics:** a series with exactly one chapter. UI may label “One-shot” when `chapter_count == 1`; no separate entity.

**No `Comic` entity** in the greenfield model (unlike the Flask prototype).

---

## Entity relationship diagram

```mermaid
erDiagram
    User ||--o{ Series : creates
    User ||--o{ InviteToken : consumes
    User ||--o{ CreatorStipend : receives
    Series ||--o{ Chapter : contains
    Chapter ||--o{ ChapterPage : contains
    Series ||--o{ ScheduleEvent : logs
    Chapter ||--o{ AnalyticsEvent : tracks
    Series ||--o{ AnalyticsEvent : tracks
    User ||--o{ ContentReport : files

    User {
        uuid id PK
        string google_sub UK
        string email UK
        string display_name
        enum role READER_CREATOR_ADMIN
        string locale
        datetime created_at
    }

    InviteToken {
        uuid id PK
        string token UK
        uuid created_by FK
        uuid used_by FK
        datetime used_at
        datetime expires_at
    }

    Series {
        uuid id PK
        uuid creator_id FK
        string slug UK
        string title
        text description
        string cover_storage_key
        string content_language
        jsonb genres
        enum status ONGOING_COMPLETED_HIATUS
        enum cadence OFF_WEEKLY_BIWEEKLY
        int day_of_week
        datetime next_expected_at
        datetime last_published_at
        string skip_message
        int version
        datetime created_at
    }

    Chapter {
        uuid id PK
        uuid series_id FK
        float chapter_number
        string title
        enum state DRAFT_SCHEDULED_PUBLISHED
        datetime scheduled_at
        datetime published_at
        datetime listed_at
        boolean list_early
        string slug UK
        datetime created_at
    }

    ChapterPage {
        uuid id PK
        uuid chapter_id FK
        int sort_order
        string original_storage_key
        string webp_storage_key
        int width
        int height
        bigint bytes_original
        bigint bytes_webp
    }

    ScheduleEvent {
        uuid id PK
        uuid series_id FK
        enum type SKIP_HIATUS_RESUME_PUBLISH_EARLY
        string skip_message
        jsonb metadata
        datetime created_at
    }

    AnalyticsEvent {
        bigint id PK
        enum type CHAPTER_VIEW_SERIES_VIEW
        uuid series_id
        uuid chapter_id
        string reader_id
        string referrer
        datetime occurred_at
    }

    CreatorStipend {
        uuid id PK
        uuid creator_id FK
        decimal amount_inr
        string period
        enum status PENDING_PAID
        uuid recorded_by FK
        datetime created_at
    }

    ContentReport {
        uuid id PK
        uuid series_id
        uuid chapter_id
        string reporter_email
        text reason
        enum status OPEN_RESOLVED
        datetime created_at
    }
```

---

## Business rules

### Series

| Rule | Enforcement |
|------|-------------|
| Max **5 ONGOING** series per creator | Check on create + status change to ONGOING |
| COMPLETED / HIATUS series don't count toward cap | Status filter |
| `slug` globally unique | DB unique index; generate from title + suffix on collision |
| `content_language` | Free-text or BCP-47 tag (e.g. `hi`, `en`, `ta`) — display only in V0 |
| `genres` | JSON array; India-first tags: romance, mythology, slice-of-life, horror, campus, drama |

### Chapters

| Rule | Enforcement |
|------|-------------|
| `chapter_number` is float | Supports 1, 1.5, 2, etc. |
| Unique `(series_id, chapter_number)` among non-draft | Partial unique index |
| Max pages per chapter | Config default **40** (typical 20–30) |
| Max file size | **16 MB** per image (align with product PRD) |
| Copyright attestation | Required boolean on publish action |

### Publish visibility (V0-specific)

When a chapter transitions to **PUBLISHED**:

| Field | Behavior |
|-------|----------|
| `published_at` | Set to now (or explicit early-publish time) |
| `scheduled_at` | Target “go live on catalog” time (may equal `published_at` for publish-now) |
| `listed_at` | When chapter appears on series page + homepage |
| `list_early` | Creator chose to list before `scheduled_at` |

**Default rule:**

```
if list_early OR now >= scheduled_at:
    listed_at = now (or scheduled_at if in past)
else:
    listed_at = NULL  → chapter is "published but unlisted"
```

**Reader access:**

| State | Direct link | Series chapter list | Homepage |
|-------|-------------|---------------------|----------|
| DRAFT | Creator only | Hidden | Hidden |
| PUBLISHED, unlisted | **Yes** (share URL works) | Hidden until listed | Hidden |
| PUBLISHED, listed | Yes | Visible | Visible |

This supports: creator shares link before catalog listing time; early-access tiers in V1/V2 can gate the direct link separately.

### Invite tokens

| Rule | Enforcement |
|------|-------------|
| One-time use | `used_at` set on successful signup |
| Feature flag `invite_required` | When false, creator signup skips token |
| Admin generates token | Random secure string (e.g. 32 bytes URL-safe) |

---

## URL scheme

| URL | Purpose |
|-----|---------|
| `/` | Public homepage |
| `/read/s/{seriesSlug}` | React series hub + schedule strip |
| `/read/s/{seriesSlug}/c/{chapterSlug}` | React vertical reader |
| `/creator` | Creator dashboard |
| `/creator/series/{id}` | Series detail |
| `/creator/series/{id}/chapters/{id}/edit` | Chapter editor |
| `/admin` | Admin portal |
| `/api/reader/v1/...` | JSON API for React |

**Stable slugs:** never expose raw UUIDs in public reader URLs. Use `series.slug` and `chapter.slug` (e.g. `chapter-1`, `chapter-1-5`).

**OG crawler path:** Nginx serves pre-rendered or server-rendered HTML with meta tags for `/read/s/*` when `User-Agent` is a crawler, OR Spring endpoint returns minimal HTML shell with OG tags before React hydrates (see [frontend.md](./frontend.md)).

---

## Feature flags (V0)

| Flag | Default | Purpose |
|------|---------|---------|
| `invite_required` | `true` | Gate creator signup on invite token |
| `public_homepage` | `true` | Show homepage (always on per product decision) |
| `webp_required` | `false` | Block publish until WebP ready (strict mode off in V0) |

Store in `feature_flag` table for runtime admin toggle without redeploy.

---

## PostgreSQL notes

- Primary keys: **UUID** (`gen_random_uuid()`) for public-facing entities
- Timestamps: `timestamptz`, always UTC
- Optimistic lock: `series.version` incremented on schedule mutations
- Indexes:
  - `series(creator_id, status)` — ongoing cap check
  - `chapter(series_id, listed_at DESC)` — catalog queries
  - `analytics_event(series_id, reader_id, occurred_at)` — return-after-skip
  - `chapter(slug)` unique within series via `(series_id, slug)`

---

## Flyway migration order (suggested)

1. `V1__users_roles`
2. `V2__invite_tokens`
3. `V3__series`
4. `V4__chapters_pages`
5. `V5__schedule_events`
6. `V6__analytics_events`
7. `V7__creator_stipends`
8. `V8__content_reports`
9. `V9__feature_flags`
10. `V10__payment_stubs` (see [v1-foundation-hooks.md](./v1-foundation-hooks.md))

---

## Related documents

- [scheduling-engine.md](./scheduling-engine.md) — cadence and skip math
- [identity-and-auth.md](./identity-and-auth.md) — roles and OAuth
- [media-pipeline.md](./media-pipeline.md) — storage keys on `ChapterPage`
