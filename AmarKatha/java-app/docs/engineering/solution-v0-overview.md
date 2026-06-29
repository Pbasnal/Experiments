# Solution V0 — Overview

## Purpose

Define the **lean engineering solution** for AmarKatha V0: validate whether Indian indie creators will publish on a flexible schedule and readers will return after skips, distributed primarily via **creator share links**.

This is a **greenfield Java rebuild**. The existing Flask prototype ([ARCHITECTURE.md](../ARCHITECTURE.md)) informs domain concepts only; it is not carried forward.

---

## North star (engineering)

Ship in **4 weeks** (solo + AI) a system where:

1. An invited creator can upload a chapter, set optional cadence, skip/hiatus, and copy a share link in under 5 minutes.
2. A reader on a **4G mobile browser** in a low-bandwidth region can open that link, scroll vertically through WebP pages, and see an accurate schedule strip.
3. We can measure **reader return within 7 days of stated next date** using in-house analytics + Grafana.

---

## Constraints

| Constraint | Decision |
|------------|----------|
| Team | Solo developer + AI assistance |
| Timeline | **4 weeks** to validation launch |
| Infra budget | Free Azure VM + ~₹10k/month credits |
| Architecture | **Modular monolith** — one deployable, strict module boundaries |
| Backend paradigm | **Entity Component System** via [dominion-ecs-java](https://github.com/dominion-dev/dominion-ecs-java) |
| Performance pattern | **Request coalescing** for hot read paths ([article](https://medium.com/@pankajbasnal17/request-coalescing-for-backend-apis-c2d4a1702a50)) |
| Frontend split | **Thymeleaf** (creator + admin) · **React** (reader) |
| Java version | **25** (LTS policy: pin to latest stable LTS at project bootstrap if 25 is pre-release) |

---

## Product wedge (V0)

> “Publish on your rhythm, share a link, readers know when you’re back.”

Not: “The Indian WEBTOON.”

**Primary distribution:** creator share links (Instagram, WhatsApp, WEBTOON bio).  
**Secondary:** public homepage (chronological catalog — no Editor’s Picks in V0).

---

## Scope deltas vs product docs

The written product docs ([mvp-goal.md](../product/mvp-goal.md)) assume Editor’s Picks and a seed-catalog gate. V0 engineering scope **intentionally differs**:

| Product doc | V0 engineering decision | Rationale |
|-------------|-------------------------|-----------|
| Editor’s Picks on homepage | **Removed** | Too few series at launch; no curation ops yet |
| Seed catalog gate (≥10 series) | **Removed** | Creators recruited post-build; homepage shows whatever is public |
| Public homepage | **Yes** | Chronological / recently updated only |
| Unlisted until schedule time | **Added** | Published chapters hidden from catalog until `scheduled_at`; creator can list early |
| WebP conversion | **Added** | India remote/low-bandwidth audience |
| Reader subscriptions (Phase 1.5) | **Foundation only** | Schema + interfaces; launch soon after V0 |

---

## Core capabilities (must ship)

### Creator path

- Google OAuth sign-in
- Invite-token signup (feature-flagged, one-time tokens)
- Series CRUD — max **5 ONGOING** series per creator; unlimited completed/hiatus
- Chapter upload: 20–30 images typical; multi-image reorder
- Lifecycle: draft → scheduled → published
- **Publish visibility:** published but **unlisted** until scheduled time; optional **list early**
- Weekly / biweekly cadence (off until after 2nd chapter prompt)
- Skip next slot + optional skip note (≤280 chars)
- Hiatus / resume (no return date in V0)
- Early publish advances calendar
- Copyright ownership checkbox on publish
- Shareable series/chapter URLs

### Reader path

- **Anonymous** read — no account required
- Mobile-first **vertical image scroll** (React)
- Series page with schedule strip + hiatus banner + skip note
- Open Graph / WhatsApp preview meta tags
- WebP-served pages with original retained for creator download

### Ops / admin

- Generate invite tokens (admin portal)
- Creator stipend tracking (admin portal)
- Email-based content report (`mailto:` + logged report ID)
- Manual takedown within 24h SLA
- Grafana dashboards on in-house metrics

### i18n

- App UI: **English + Hindi** via message module; extensible to more locales
- Comic content: any language the creator chooses (metadata tag on series)

---

## Explicitly out of V0

| Feature | When |
|---------|------|
| Editor’s Picks curation UI | Post-traction |
| Search | V1 |
| Comments, ratings, follows | V1 |
| Trending algorithm | V1 (volume threshold) |
| Auto-publish cron | V1 |
| Native mobile app | V1+ if retention justifies |
| Email/push notifications | V1 |
| Paid subscription checkout | Phase 1.5 (foundation in V0) |
| Early-access / patron unlisted tiers | V1/V2 |
| Full IT Rules compliance officer | V0: founder as interim; formalize before scale |

---

## Validation metrics (90 days)

| Metric | Target | Source |
|--------|--------|--------|
| Invited creators with ≥2 chapters | ≥20 / 50 | DB |
| Series using skip or hiatus | ≥30% active | `schedule_event` |
| Reader return after skip (7d window) | ≥40% | `analytics_event` + cookie |
| Creators with weekly/biweekly set | ≥50% of active | `series.schedule` |
| Creator 30-day retention | ≥60% | DB |
| Share-link vs homepage read ratio | Baseline | `analytics_event.referrer` |

See [analytics-and-observability.md](./analytics-and-observability.md) for event schema.

---

## Decision log

| # | Topic | Decision |
|---|-------|----------|
| 1 | Content hierarchy | **Series → Chapter → Page** only; one-shot = single-chapter series |
| 2 | Ongoing series cap | 5 per creator |
| 3 | Chapter numbers | Float (special chapters between main numbers) |
| 4 | Pre-schedule visibility | Published chapters **unlisted** until `scheduled_at`; list-early allowed |
| 5 | Default cadence | Off until 2nd chapter |
| 6 | Early publish | Advances calendar |
| 7 | Skip note | Yes — optional ≤280 chars on reader series page |
| 8 | Hiatus return date | Not in V0 |
| 9 | Reader auth | Anonymous |
| 10 | Creator auth | Google OAuth |
| 11 | Admin | `ADMIN` role; ops team small (founder only at launch) |
| 12 | Invite flow | Manual token generation; one-time use; feature flag |
| 13 | Homepage | Public; chronological; no editor picks |
| 14 | Analytics | In-house DB events; Grafana dashboards |
| 15 | Same reader | `reader_id` cookie + `series_id` |
| 16 | Report flow | Email-based |
| 17 | Takedown SLA | 24h manual |
| 18 | Grievance officer | Founder interim — document on site before public marketing |
| 19 | Region | Azure **Southeast Asia (Singapore)** — verify tax/legal separately |
| 20 | Domain/CDN | Not yet; plan Cloudflare in front when domain acquired |
| 21 | Copyright | Checkbox attestation on publish |
| 22 | Image format | WebP for reader; store original + WebP |
| 23 | Page count | ~20–30 typical; enforce max per chapter in config |
| 24 | Payments | Subscription model foundation; no checkout in V0 |
| 25 | Creator stipend | Admin portal tracking |

---

## Document map

Implementation detail lives in sibling docs under `docs/engineering/`. Start with [architecture.md](./architecture.md) and [scope-and-timeline.md](./scope-and-timeline.md).
