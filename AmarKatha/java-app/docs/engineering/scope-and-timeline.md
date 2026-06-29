# Scope and Timeline

**Goal:** Validation launch in **4 weeks** — solo developer + AI.

---

## In scope (V0)

### Week 1 — Foundation

| Deliverable | Module |
|-------------|--------|
| Spring Boot project skeleton, Java 25, Flyway | `bootstrap` |
| PostgreSQL schema: users, series, chapters, pages | all |
| Google OAuth + roles | `identity` |
| Invite token CRUD + feature flag | `identity`, `admin` |
| i18n scaffolding (en + hi) | `shared` |
| Series CRUD (5 ongoing cap) | `publishing` |
| Dominion ECS + coalescing queue skeleton | `shared`, `scheduling` |

**Exit criteria:** Creator can OAuth, redeem invite, create a series.

### Week 2 — Publishing

| Deliverable | Module |
|-------------|--------|
| Multi-image chapter upload | `publishing`, `media` |
| Page reorder | `publishing` |
| Azure Blob storage (or local dev filesystem) | `media` |
| WebP conversion async pipeline | `media` |
| Draft / scheduled / published states | `publishing` |
| Copyright checkbox on publish | `publishing` |
| Publish visibility (unlisted until `scheduled_at`, list early) | `publishing`, `scheduling` |

**Exit criteria:** Creator uploads 20-page chapter, publishes with schedule listing time.

### Week 3 — Scheduling + Reader

| Deliverable | Module |
|-------------|--------|
| Weekly/biweekly cadence, off-by-default | `scheduling` |
| Skip + skip note; hiatus/resume | `scheduling` |
| Schedule strip logic | `scheduling`, `reader` |
| React reader: series page + vertical scroll | `frontend` |
| Reader JSON API + coalescing | `reader` |
| `reader_id` cookie + chapter view events | `analytics` |
| OG meta endpoints | `reader`, `frontend` |

**Exit criteria:** Share link opens mobile reader; skip updates strip; WebP loads.

### Week 4 — Catalog, Admin, Launch

| Deliverable | Module |
|-------------|--------|
| Public homepage (chronological, no editor picks) | `catalog` |
| Admin: invite tokens, stipends, reports list | `admin` |
| Grafana dashboards (KPI + ops) | `analytics` |
| Legal stub pages (terms, privacy, grievance) | static |
| Azure VM deploy, TLS, smoke tests | `infrastructure` |
| Payment schema stubs only | `payments` |

**Exit criteria:** End-to-end demo on production URL; Grafana shows view counts.

---

## Out of scope (V0)

| Item | Target |
|------|--------|
| Editor’s Picks | Removed from V0 |
| Search, genre filters on home | V1 |
| Comments, ratings, follows | V1 |
| Auto-publish cron | V1 |
| Email notifications | V0.1 |
| Native app | V1+ |
| Subscription checkout | Phase 1.5 |
| Early-access patron gating | V1/V2 |
| PDF upload | V1 |
| Cloudflare / custom domain | When domain ready |
| Automated tests (beyond scheduling unit tests) | Best effort |

---

## Risk register

| Risk | Impact | Mitigation |
|------|--------|------------|
| 4 weeks too tight for React + Spring + WebP | High | Cut HTMX niceties; minimal admin UI; defer email |
| WebP CPU on small VM | Medium | Async queue; limit concurrent conversions to 2 |
| No seed creators at launch | Medium | Homepage works empty; founder dogfoods |
| Singapore latency + no CDN | Medium | Aggressive WebP compression; Cloudflare when domain live |
| ECS learning curve | Medium | Use ECS only on catalog coalescing path; rest is services |
| IT Rules gap | Low at invite-only scale | Publish grievance page before marketing |

---

## Kill criteria (day 90)

From [mvp-goal.md](../product/mvp-goal.md) — unchanged:

| Signal | Action |
|--------|--------|
| &lt;10 creators with ≥2 chapters | Pivot wedge |
| Skip/hiatus &lt;15% adoption | Interview creators |
| Reader return-after-skip &lt;25% | Fix comms / strip UX |
| Zero share-link traffic | Fix creator incentive to distribute |
| &gt;2 moderation incidents without process | Pause growth |

---

## Definition of done (launch)

- [ ] Creator: invite → OAuth → series → upload → publish → share link
- [ ] Reader: anonymous vertical read, WebP, schedule strip, OG preview works in WhatsApp debugger
- [ ] Skip/hiatus updates reader-facing dates and note
- [ ] Homepage lists public listed chapters
- [ ] Admin generates invite token; records stipend
- [ ] Grafana shows chapter views and return-after-skip query
- [ ] Legal/grievance page live
- [ ] Deployed on Azure Singapore with HTTPS

---

## Post-launch (V0.1 — weeks 5–8)

Priority order if validation metrics need help:

1. Email creators on skip reminder / publish confirmation
2. Genre filters on homepage
3. Creator-facing view counts on series detail
4. Cloudflare + custom domain
5. Reduced-width `reader-low.webp` for Save-Data clients

---

## Related documents

- [solution-v0-overview.md](./solution-v0-overview.md) — full capability list
- [architecture.md](./architecture.md) — module map
- [v1-foundation-hooks.md](./v1-foundation-hooks.md) — subscriptions next
