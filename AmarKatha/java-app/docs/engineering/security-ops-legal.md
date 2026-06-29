# Security, Ops, and Legal

Minimal compliance for **V0 validation launch** in India. Formalize before scaling traffic or opening public creator signup.

---

## IT Rules 2021 (India)

UGC platforms must designate a **Grievance Officer** and publish contact details.

| Requirement | V0 status |
|-------------|-----------|
| Grievance officer | **Founder (interim)** — name + email on `/legal/grievance` |
| Takedown process | Documented 24h manual SLA |
| Content report channel | Email-based (see below) |

**Before public marketing:** publish static pages:

- `/legal/terms`
- `/legal/privacy`
- `/legal/grievance` — officer name, email, 24h acknowledgment target

Use i18n keys for en/hi versions when Hindi launch matters.

---

## Content reporting

### V0 flow: email-based

1. Reader clicks **“Report content”** on series/chapter page
2. Opens `mailto:reports@{domain}?subject=Report {seriesSlug}&body=...` with pre-filled IDs
3. Optional: `POST /api/reader/v1/report` logs row in `content_report` for admin queue

Admin portal lists open reports with links to takedown actions.

### Report reasons (dropdown in form)

- Copyright infringement
- Hateful or illegal content
- Spam
- Other

---

## Takedown SLA

| Step | Target |
|------|--------|
| Acknowledge report | 24 hours |
| Remove or resolve | 24 hours manual (founder ops) |
| Notify reporter | Email when resolved |

**Admin actions:**

- Unlist chapter (set `listed_at = null`, keep or remove share access — default **remove public access** on serious reports)
- Delete chapter / series
- Suspend creator account

Log actions in admin audit table (V0.1) or application logs.

---

## Copyright (creators)

V0: **checkbox attestation** on publish (see [media-pipeline.md](./media-pipeline.md)).

Platform stance:

- Creators retain ownership
- AmarKatha gets license to host and display for platform operation
- Repeat infringers: manual ban

Full DMCA-style process deferred until volume warrants.

---

## Admin access

| Topic | V0 |
|-------|-----|
| Who is admin | Founder only; `ADMIN` role in DB |
| Future ops team | Assign `ADMIN` via admin UI or SQL |
| Separation | Creators cannot access `/admin` (fix Flask prototype gap) |

---

## Security operations

| Control | Implementation |
|---------|----------------|
| HTTPS | Required |
| Session fixation | Spring Security defaults |
| Upload abuse | Size limits, MIME validation, rate limit |
| SQL injection | JPA parameterized queries |
| XSS | Thymeleaf auto-escape; React default escape |
| CSRF | Creator/admin forms |
| Secrets | Env vars, not committed |

---

## Backup and recovery

| Asset | Backup |
|-------|--------|
| PostgreSQL | Nightly dump → blob |
| Media | Blob redundancy (LRS); originals are source of truth |
| Config | Infrastructure in git |

**RTO/RPO (V0):** best effort — 24h data loss window acceptable for validation.

---

## Incident response (minimal)

1. Content incident → takedown within 24h
2. Security breach → rotate secrets, take app offline if needed
3. Document post-mortem in private notes

---

## Related documents

- [identity-and-auth.md](./identity-and-auth.md) — roles
- [infrastructure.md](./infrastructure.md) — backups
- [domain-model.md](./domain-model.md) — `content_report` table
