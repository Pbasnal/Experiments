# Frontend

## Split architecture

| App | Stack | Users | Auth |
|-----|-------|-------|------|
| **Creator portal** | Thymeleaf (+ HTMX for modals) | Creators | Google OAuth session |
| **Admin portal** | Thymeleaf | Ops (founder + future team) | Google OAuth + `ADMIN` role |
| **Reader** | React 19 + Vite + TypeScript | Public | **Anonymous** (no login V0) |

Both served from the **same origin** via Nginx to avoid CORS complexity in V0.

```
https://{host}/
  /creator/**     → Spring MVC (Thymeleaf)
  /admin/**       → Spring MVC (Thymeleaf)
  /read/**        → React SPA (static bundle)
  /api/reader/**  → JSON for React
  /assets/**      → static CSS/JS/images
```

---

## Reader (React)

### Core views

| Route | Component | Job |
|-------|-----------|-----|
| `/read/s/:seriesSlug` | `SeriesPage` | Cover, synopsis, schedule strip, chapter list (listed only), hiatus banner, skip note |
| `/read/s/:seriesSlug/c/:chapterSlug` | `ChapterReader` | Vertical scroll image stack |
| `/` | `HomePage` | Recent/public series — calls `/api/reader/v1/home` |

### Vertical scroll implementation

**Simple image stack** — no canvas/WebGL in V0.

```tsx
// Conceptual structure
<div className="reader-column">
  {pages.map(p => (
    <img
      key={p.id}
      src={p.webpUrl}
      loading="lazy"
      decoding="async"
      alt=""
      width={p.width}
      height={p.height}
    />
  ))}
</div>
```

CSS:

- `width: 100%`; `height: auto`; no horizontal scroll
- Optional `scroll-snap-type: y proximity` for panel feel
- Reserve aspect ratio via `width`/`height` attrs to reduce layout shift on slow networks

**PDF scroll:** deferred — images only in V0 per product PRD.

### Data fetching

- REST JSON from `/api/reader/v1/...`
- Coalesced backend reads (see [architecture.md](./architecture.md))
- Client-side: SWR or TanStack Query for cache + retry on flaky networks

### Low bandwidth UX

- Request WebP URLs from API (see [media-pipeline.md](./media-pipeline.md))
- Lazy load below fold
- Optional low-data mode (V0.1): load reduced-width WebP if `?quality=low` or `Save-Data` header

---

## Open Graph / WhatsApp previews

**Required for V0** — India distribution is WhatsApp/Instagram link-heavy.

### Meta tags (per series and chapter)

```html
<meta property="og:title" content="{seriesTitle} — {chapterTitle}" />
<meta property="og:description" content="{truncatedDescription or schedule line}" />
<meta property="og:image" content="https://{host}/media/covers/{seriesCoverWebp}" />
<meta property="og:url" content="https://{host}/read/s/{slug}/c/{chapterSlug}" />
<meta property="og:type" content="article" />
<meta name="twitter:card" content="summary_large_image" />
```

### Crawler strategy (pick at implement time)

| Option | Pros | Cons |
|--------|------|------|
| **A. Spring OG endpoint** | Reliable for all crawlers | Extra MVC route |
| **B. prerender.io-style** | — | Paid |
| **C. `index.html` + SSR shell for bots** | Nginx UA sniff → Spring | UA lists need maintenance |

**Recommendation:** Option A — `GET /read/s/{slug}` and chapter URLs return minimal HTML with OG tags when `Accept: text/html` and no JS, OR dedicated `/og/s/{slug}` redirect. React still handles human browsers.

WhatsApp crawler: verify against [Facebook Sharing Debugger](https://developers.facebook.com/tools/debug/) before launch.

---

## Creator (Thymeleaf)

### V0 pages (maps to HL-PRD §4.1)

| Page | Path | Notes |
|------|------|-------|
| Creator home | `/creator` | Next slot, drafts due, quick links |
| Series list | `/creator/series` | Ongoing cap indicator (3/5) |
| Series detail | `/creator/series/{id}` | Schedule, skip/hiatus, share link copy |
| New/edit series | `/creator/series/new`, `.../edit` | |
| Chapter editor | `/creator/series/{id}/chapters/{id}/edit` | Multi-upload, reorder, publish |
| Schedule settings | inline on series detail | Weekly/biweekly + weekday |
| Skip / hiatus | modal or inline | Skip note textarea |
| Become creator | `/creator/onboard` | After Google OAuth + invite token |

### Upload UX

- Drag-drop multi-file
- Client-side sortable list before submit
- Progress bar per file (important on slow uplinks)
- Copyright checkbox on publish button (disabled until checked)

### Share link

Prominent copy button on series detail:

```
https://{host}/read/s/{slug}
```

Per-chapter link after publish for unlisted-until-scheduled workflow.

---

## Admin (Thymeleaf)

| Page | Path |
|------|------|
| Dashboard | `/admin` |
| Invite tokens | `/admin/invites` — generate, list used/unused |
| Creator stipends | `/admin/stipends` |
| Content reports | `/admin/reports` |
| Takedown | action on series/chapter |

---

## Internationalization (i18n)

### Requirements

- App UI in **English + Hindi** at launch
- All user-facing strings from **language module** — no hardcoded copy in templates or React
- Comic content language is creator metadata, not app translation

### Backend

```properties
# messages_en.properties
schedule.next_update=Next update
schedule.on_hiatus=On hiatus

# messages_hi.properties
schedule.next_update=अगला अपडेट
schedule.on_hiatus=विराम पर
```

- Spring `MessageSource` + `LocaleResolver` (cookie `locale=en|hi`)
- Thymeleaf: `#{schedule.next_update}`
- React: load `/api/reader/v1/i18n?locale=hi` bundle or embed in initial JSON

### Locale switcher

- Footer on reader; header on creator
- Persists in cookie

---

## Design constraints (V0)

| Area | Target |
|------|--------|
| Reader LCP on 4G | &lt; 2.5s for first image (WebP + lazy rest) |
| Mobile width | 360px minimum |
| Accessibility | Semantic HTML, alt text on images optional for decorative pages; WCAG AA deferred |
| Theming | Single light theme |

---

## Build and deploy

| Artifact | Build | Output |
|----------|-------|--------|
| Spring app | `./mvnw package` | JAR |
| React reader | `npm run build` | `dist/` copied to Nginx `/var/www/read/` |

CI (optional V0): single script builds both artifacts into Docker image.

---

## Related documents

- [media-pipeline.md](./media-pipeline.md) — WebP URLs in reader
- [identity-and-auth.md](./identity-and-auth.md) — creator sessions
- [architecture.md](./architecture.md) — API split
