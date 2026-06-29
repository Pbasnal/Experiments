# MVP goal — lean validation launch

> **Product docs:** North star (this file) · [Publishing tool HL-PRD](./publishing-tool-hl-prd.md) (V0 vs V1 scope) · [India market feedback](./india-market-analysis-feedback.md) · [Creator dashboard requirements](../planning/creator-dashboard-requirements.md) (post-validation depth)

## North star

“To test if Indian comic creators and readers want a platform that prioritizes storytelling quality, creator-first tools, and consistent discovery.”

**Reality check (June 2025 market review):** India’s comics market is real but crowded. AmarKatha cannot validate its hypothesis by shipping a full reader marketplace on day one. The **first launch must be lean**: prove creators will publish on a flexible schedule and that readers will return for updates — using **shareable links and a small seeded catalog**, not competing head-on with WEBTOON or Toonsutra.

---

## Launch strategy: two phases

| Phase | Name | Goal | Audience |
|-------|------|------|----------|
| **V0** | Validation launch | Learn whether scheduling + skip/hiatus reduces creator drop-off and readers return after skips | 20–50 invite-only creators; readers via creator share links + small public catalog |
| **V1** | Platform expansion | Test discovery at modest scale | Open signup; only if V0 kill criteria pass |

**V0 wedge:** “Publish on your rhythm, share a link, readers know when you’re back” — not “the Indian WEBTOON.”

---

## V0 — must ship (validation launch)

### Creator path

| Feature | Why it’s in V0 |
|---------|----------------|
| Upload chapter (images; PDF deferred) | Minimum viable publish; keep storage/bandwidth predictable |
| One series + chapters | Enough structure to test schedule behavior |
| Weekly / biweekly schedule + **skip one cycle** + **hiatus** | Core differentiation; see [publishing-tool-hl-prd.md](./publishing-tool-hl-prd.md) §V0 |
| Publish now or pick next slot (manual) | Validates scheduling intent without auto-publish complexity |
| Shareable reader link per series/chapter | Creators bring audience from Instagram/WEBTOON; no cold-start marketplace required |
| Simple creator profile | Legitimacy for share links |

### Reader path (minimal)

| Feature | Why it’s in V0 |
|---------|----------------|
| Vertical-scroll chapter reader (mobile web) | Matches ~41% webtoon-format consumption; no native app in V0 |
| Public series page with schedule strip | “Last update / next expected” supports retention hypothesis |
| Small **seed catalog** home | Editorial trust without algorithm cold-start |

**Seed catalog gate (before any public marketing):** ≥10 series, ≥30 published chapters, hand-curated — Editor’s Picks only. No open upload on homepage until this bar is met.

### Ops (minimal)

| Feature | Why it’s in V0 |
|---------|----------------|
| Invite-only creator onboarding | Quality cohort; avoids moderation firehose |
| Admin: report & remove + editor-pick flag | IT Rules–aware minimum; no full admin suite |
| Basic metrics | Chapter views, creator 30-day retention, repeat visits on schedule days |

### Discovery (cold-start only)

| Feature | Rule |
|---------|------|
| Home feed | **Chronological + Editor’s Picks only** — no trending until ≥500 daily chapter reads platform-wide |
| Genre filters | India-proven tags: romance, mythology, slice-of-life, horror, campus, drama — not anime-only taxonomy (“Shonen” deferred) |
| Search | **Deferred** — catalog too small to matter |

---

## V0 — explicitly deferred

| Feature | Why defer |
|---------|-----------|
| Follow / subscribe | Validate via share links + return visits first; follows add complexity before supply exists |
| Comments & ratings | UGC moderation load; defer until seed catalog stable |
| Trending / dwell algorithm | Noise at low volume; see cold-start rule above |
| Full publishing HL-PRD | Auto-queue, auto-publish, calendar, 6 cadence types, notifications center |
| Multi-format upload (PDF, text, batch 100) | Images-only for V0 |
| Per-chapter analytics, pacing graphs | Post-validation |
| Native mobile app | Mobile **web** first; push notifications when app exists |
| Paid subscriptions | Too early for business validation |
| Open public creator signup | Invite-only until V0 metrics pass |

---

## Phase 1.5 — after V0 signal (not blocking launch)

Symbolic creator economics matter in India even before full monetization:

| Feature | Purpose |
|---------|---------|
| Creator stipend / fund (manual, e.g. ₹500–2,000/month for cohort) | Tests “will creators show up without WEBTOON-scale audience?” |
| Tip jar / UPI (optional) | Symbolic revenue story; not full subscription billing |

---

## V1 — only if V0 passes (platform expansion)

Add in order:

1. Follow comic + email / web notification on new chapter (push when app exists)
2. Search + genre browse at scale
3. Comments + star rating (with moderation playbook)
4. Trending (reads + dwell) once volume threshold met
5. Auto-publish queue per [publishing-tool-hl-prd.md](./publishing-tool-hl-prd.md) V1
6. Indic language UI for **one** chosen region/language (pick during V0 based on creator cohort)
7. Native app (if retention data justifies)

---

## Validation questions

| Question | Measured by | V0 target (90 days) |
|----------|-------------|------------------------|
| Will creators publish **without** platform pay? | Active creators uploading ≥2 chapters | ≥20 of 50 invited |
| Will creators use skip/hiatus instead of going silent? | % of active series with ≥1 skip/hiatus | ≥30% |
| Do readers return after a skipped week? | Return rate within 7 days of stated next date | ≥40% of prior chapter readers |
| Will readers open **share links** (off-platform acquisition)? | CTR from creator-shared links | Baseline + week-over-week |
| Will readers browse a **small curated** catalog? | Reads on non-owned share traffic | ≥30% of total reads |
| Does flexible scheduling reduce creator churn? | Creator 30-day retention | ≥60% |
| Will creators configure a cadence if audience is mostly off-platform? | % with active weekly/biweekly schedule | ≥50% of active creators |

**Not validated in V0:** Whether AmarKatha is a viable *business* (ARPU, CAC, LTV). V0 validates **product learning** only.

---

## Kill criteria (stop or pivot at day 90)

| Signal | Action |
|--------|--------|
| &lt;10 creators published ≥2 chapters | Pivot wedge (region, studio B2B, or tooling-only) |
| Skip/hiatus unused (&lt;15% of series) | Scheduling is not the pain; interview creators |
| Reader return-after-skip &lt;25% | Reader comms or schedule strip insufficient |
| Zero organic share-link traffic | Creators won’t distribute; incentive or wedge broken |
| Moderation / IP incidents &gt;2 without process | Pause open growth; tighten invite-only |

---

## Deferred platform features (unchanged intent)

| Feature | Reason |
|---------|--------|
| Peer review system | Needs trust and scale |
| On-demand print | Operationally heavy |
| Full paid subscriptions | After engagement validated |
| Multiple UI themes | Polish, not learning |
| Community doodle feed | Scope creep |

---

## Success tiers (from market analysis)

| Tier | Definition | V0 aim |
|------|------------|--------|
| **A. Useful learning** | 50–200 creators testing scheduling | ✅ Primary goal |
| **B. Niche community** | 10K–50K MAU | V1+ |
| **C. Scaled platform** | 500K+ MAU | Out of scope for current docs |

---

## What V0 does *not* claim

- Competing with WEBTOON, Toonsutra, or Pratilipi on catalog or distribution
- Validating monetization or creator economics at scale
- India-wide multilingual support on day one

See [india-market-analysis-feedback.md](./india-market-analysis-feedback.md) for market context and competitive rationale.
