# AmarKatha — project status

**Last updated:** May 21, 2026  
**Overall:** Early prototype — strong foundation and creator-dashboard work (Sprints 1–2), but the **reader MVP path is incomplete** and several routes reference **missing templates**. The root README overstates what is production-ready.

---

## Executive summary

AmarKatha is a Flask web app aimed at validating whether Indian comic creators and readers want a quality-first platform (see [product/mvp-goal.md](./product/mvp-goal.md) — **V0 lean validation launch**). The codebase has:

- A **complete data model** for the planned MVP (users, comics, chapters, pages, ratings, comments, follows, view logs, series).
- **Working infrastructure** (Docker, migrations, auth basics, creator dashboard UI, series management).
- **Incomplete reader experience** (home feed data mismatch, no chapter reader, no follow/rate/comment APIs, view analytics not recorded).
- **Incomplete admin UI** (backend routes exist; most templates are missing).
- **No automated tests** and minimal static assets (hero images referenced in templates are absent).

**Rough completion vs MVP GOAL:** ~**40%** — backend/schema ahead of user-facing reader flows.

---

## MVP goal (unchanged)

> Test if Indian comic creators and readers want a platform that prioritizes storytelling quality, creator-first tools, and consistent discovery.

Validation still requires: publish → discover → read → engage → measure. Today the **publish** side is furthest along; **read + engage + measure** are largely unbuilt in routes/UI.

---

## Feature status matrix

| MVP area | Feature | Status | Notes |
|----------|---------|--------|-------|
| **Reader** | Home feed (trending / new / editor picks) | ⚠️ Partial | Logic in `main.index()`; `index.html` expects `comics`, route passes `trending_comics`, `new_comics`, `editor_picks` — feed sections will not render correctly |
| **Reader** | Comic detail page | ⚠️ Broken | Route renders `comic/detail.html` — **file does not exist** |
| **Reader** | Chapter viewer (vertical scroll) | ❌ Missing | No `chapter_view` route; `comic/view.html` is legacy (expects `comic.pages`, old `comic.*` URLs) |
| **Reader** | Search + genre filters | ⚠️ Partial | Route exists; **`search.html` missing** |
| **Reader** | Follow comic | ❌ Missing | `ComicFollow` model only; no routes |
| **Reader** | Rate comic / chapter | ❌ Missing | Models only; no routes |
| **Reader** | Comments per chapter | ❌ Missing | Model only; no routes |
| **Reader** | View / dwell tracking | ❌ Missing | `ViewLog` model; nothing writes logs on read |
| **Creator** | Dashboard overview | ✅ Done | Stats, activity feed, series/comic cards |
| **Creator** | Create / edit comic | ✅ Done | Templates present |
| **Creator** | Series create / edit / detail | ✅ Done | Includes basic series analytics |
| **Creator** | New chapter + upload pages | ⚠️ Partial | Routes in `creator.py`; **`new_chapter.html`, `chapter_edit.html` missing** |
| **Creator** | Chapter schedule | ⚠️ Partial | DB + route; **`schedule.html` missing** — target UX in [publishing-tool-hl-prd.md](./product/publishing-tool-hl-prd.md) |
| **Creator** | Creator profile | ⚠️ Partial | Route; **`profile.html` missing** |
| **Creator** | Unified multi-format upload | ❌ Planned | Sprint 3 in [planning/dashboard-execution-plan.md](./planning/dashboard-execution-plan.md) |
| **Creator** | Per-comic analytics UI | ❌ Planned | Helpers partially exist; no `comic_analytics` route/template |
| **Discovery** | Editor picks flag | ✅ Data | `Comic.is_editor_pick`; admin can toggle if admin UI used |
| **Discovery** | Trending algorithm | ⚠️ Partial | Query uses `ViewLog` but logs are not created — trending will stay empty in practice |
| **Admin** | Dashboard / comics / comments / analytics | ⚠️ Partial | Routes in `admin.py`; templates **`dashboard`, `comics`, `comments`, `analytics` missing** (only `users.html` exists) |
| **Admin** | True admin role | ❌ Missing | `admin_required` checks `is_artist` — any creator can access admin routes |
| **Auth** | Register / login / logout | ✅ Done | |
| **Auth** | Become creator | ✅ Done | |
| **Auth** | Google OAuth | ⚠️ Optional | Wired in `app/__init__.py`; needs credentials + HTTPS in dev |
| **Ops** | Docker / HTTPS / migrations | ✅ Done | Scripts and compose files present |
| **Quality** | Tests | ❌ None | |
| **Assets** | Static images (hero, CTA) | ❌ Missing | `index.html` references paths under `static/images/` — directory not present |

Legend: ✅ works end-to-end · ⚠️ partial or broken · ❌ not implemented

---

## What works today (happy paths)

1. **Run the app** — `python run.py`, Docker via `./setup.sh`, or `./scripts/start.sh`.
2. **Register / log in** — local auth; optional Google if configured.
3. **Become a creator** — `/become-creator` sets `is_artist`.
4. **Creator dashboard** — `/creator/dashboard` with stats and activity (when logged in as artist).
5. **Create series and comics** — forms and persistence work.
6. **Edit comic metadata** — including publish flag and series assignment.
7. **Admin user list** — `/admin/users` if logged in as artist (see security note below).

---

## Critical gaps (blocks MVP demo)

1. **Reader cannot read chapters** — no working chapter route + template chain.
2. **Home page data contract** — template/route variable mismatch.
3. **Nine missing templates** (will 500 if hit):

   | Template | Referenced by |
   |----------|----------------|
   | `comic/detail.html` | `main.comic_detail` |
   | `search.html` | `main.search` |
   | `creator/new_chapter.html` | `creator.new_chapter` |
   | `creator/chapter_edit.html` | `creator.chapter_edit` |
   | `creator/profile.html` | `creator.profile` |
   | `creator/schedule.html` | `creator.schedule` |
   | `admin/dashboard.html` | `admin.dashboard` |
   | `admin/comics.html` | `admin.comics` |
   | `admin/comments.html` | `admin.comments` |
   | `admin/analytics.html` | `admin.analytics` |

4. **Engagement loop** — follow, rate, comment not exposed despite models.
5. **Analytics loop** — `ViewLog` never populated; trending and dwell metrics inactive.
6. **Admin access model** — no `is_admin`; creators have admin powers.

---

## Creator dashboard — sprint progress

From [planning/dashboard-execution-plan.md](./planning/dashboard-execution-plan.md) (internal checklist):

| Sprint | Theme | Status |
|--------|-------|--------|
| 1 | Core dashboard, Series model, migrations | ✅ Largely complete |
| 2 | Series management UI | ✅ Complete |
| 3 | Unified upload (comic/story/mixed) | ❌ Not started |
| 4 | Content library, bulk ops | ❌ Not started |
| 5 | Basic per-comic analytics | ❌ Not started |
| 6–8 | Advanced analytics, polish, integrations | ❌ Not started |

Detailed requirements in [planning/creator-dashboard-requirements.md](./planning/creator-dashboard-requirements.md) are **mostly V1+** (analytics, library, upload depth) — not V0. **V0 scope** is in [product/mvp-goal.md](./product/mvp-goal.md) and [product/publishing-tool-hl-prd.md](./product/publishing-tool-hl-prd.md).

---

## Documentation audit

| Document | Role | Trust for “what’s shipped?” |
|----------|------|-----------------------------|
| **`docs/PROJECT_STATUS.md`** (this file) | Status source of truth | ✅ Yes |
| `README.md` | Setup + feature list | ⚠️ Overstates MVP completion — being corrected |
| [product/mvp-goal.md](./product/mvp-goal.md) | Product north star; **V0 vs V1 scope** | ✅ Accurate goals |
| [product/publishing-tool-hl-prd.md](./product/publishing-tool-hl-prd.md) | Publishing V0 subset + V1 vision | ✅ Product spec (not yet implemented) |
| [product/india-market-analysis-feedback.md](./product/india-market-analysis-feedback.md) | Market review; lean launch rationale | ✅ Reference |
| [planning/dashboard-execution-plan.md](./planning/dashboard-execution-plan.md) | Sprint tasks | ✅ Good for creator work; Sprints 3+ open |
| [planning/](./planning/) (creator dashboard docs) | Design / sample code | ❌ Plans only — not live code |
| [design/image-generation-prompts.md](./design/image-generation-prompts.md) | Marketing assets | N/A for code status |
| [docker.md](./docker.md) | Local Docker setup | ✅ Single compose file |

---

## Recommended priorities

**Aligned to [product/mvp-goal.md](./product/mvp-goal.md) V0** — validate scheduling + share-link reading before building marketplace features (follow, comments, trending).

Ordered to reach a **credible V0 demo** (invite creator publishes → share link → reader returns after skip):

### P0 — V0 validation loop

1. Add `main.chapter_view` + chapter reader template (vertical scroll, `ChapterPage` images).
2. Series share page with schedule strip + hiatus banner.
3. Add missing creator templates: `new_chapter.html`, `chapter_edit.html`.
4. Skip next slot + hiatus on series (per [publishing-tool-hl-prd.md](./product/publishing-tool-hl-prd.md) V0).
5. Log chapter views + reader return-after-skip events (minimal analytics).

### P1 — V0 catalog + polish

6. Fix `main.index` ↔ `index.html` — **Editor's Picks + chronological only** (no trending).
7. Add `comic/detail.html` (chapter list, share link).
8. Seed catalog content (≥10 series / 30 chapters) before public home marketing.
9. Add placeholder hero images under `app/static/images/`.

### P2 — V1 (only if V0 kill criteria pass)

10. Follow comic, rate, comment routes.
11. Search + trending once volume threshold met.
12. Auto-publish queue, PDF upload, creator analytics UI.
13. Tests for auth, publish flow, and read path.

### Deferred (per [product/mvp-goal.md](./product/mvp-goal.md))

V0: follow, comments, ratings, search, trending, auto-publish, open signup. V1+: peer review, print, full paid subs, community doodle feed, advanced pacing analytics.

---

## Security & ops notes

- Default `SECRET_KEY` fallback in `config.py` — must override in production.
- `.env` may exist locally; use `env.example` as template; do not commit secrets.
- Upload limit 500MB per request (configurable via `MAX_CONTENT_LENGTH`); files on local disk.
- Redis in compose is optional; not required for current code paths.

---

## How to verify status locally

```bash
# From repo root
python run.py
# Or
docker compose up -d
```

Then manually check:

| URL | Expected today |
|-----|----------------|
| `/` | Landing loads; comic grids may be empty/wrong variables |
| `/creator/dashboard` | Works for artist users |
| `/creator/comic/new` | Works |
| `/comic/1` | Likely **500** (missing template) |
| `/search?q=test` | Likely **500** (missing template) |
| `/admin/dashboard` | Likely **500** for artists (missing template) |

---

## Summary one-liner

**AmarKatha is a well-scaffolded Flask prototype with creator tooling and database design in place, but it is not yet an end-to-end comic platform — the reader experience, engagement features, and several templates must be completed before the MVP hypothesis can be tested with real users.**

For structure and setup, see [ARCHITECTURE.md](./ARCHITECTURE.md) and the root [README.md](../README.md).
