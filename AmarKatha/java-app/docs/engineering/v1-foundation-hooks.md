# V1 Foundation Hooks

V0 does **not** implement payments or patron early access, but the schema and interfaces should exist so **reader subscriptions** and **creator stipends** can ship soon after validation without migrations that rewrite core tables.

---

## Reader subscriptions (Phase 1.5 — soon after V0)

### Product intent

Readers pay for access — likely:

- Platform-wide premium (ad-free, early access)
- Per-series subscription
- Or hybrid

**Decision deferred** until first creator cohort feedback. Engineering prepares **provider-agnostic** foundation.

### Schema stubs (Flyway `V10__payment_stubs`)

```sql
-- Conceptual; refine at implementation

CREATE TABLE subscription_plan (
    id UUID PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    price_inr DECIMAL(10,2) NOT NULL,
    interval VARCHAR(20) NOT NULL,  -- monthly, yearly
    scope VARCHAR(20) NOT NULL,     -- platform, series
    series_id UUID REFERENCES series(id),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE reader_account (
    id UUID PRIMARY KEY,
    google_sub VARCHAR(255) UNIQUE,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE subscription (
    id UUID PRIMARY KEY,
    reader_account_id UUID NOT NULL REFERENCES reader_account(id),
    plan_id UUID NOT NULL REFERENCES subscription_plan(id),
    status VARCHAR(20) NOT NULL,  -- trialing, active, canceled, past_due
    provider VARCHAR(20),           -- razorpay, stripe
    provider_subscription_id VARCHAR(255),
    current_period_end TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE payment_event (
    id UUID PRIMARY KEY,
    subscription_id UUID REFERENCES subscription(id),
    type VARCHAR(50) NOT NULL,
    amount_inr DECIMAL(10,2),
    provider_event_id VARCHAR(255),
    raw_json JSONB,
    created_at TIMESTAMPTZ DEFAULT now()
);
```

V0: tables exist, **no rows**, no UI, no webhooks.

### Java interfaces (`payments/` module)

```java
public interface PaymentProvider {
    CheckoutSession createCheckout(CreateCheckoutRequest req);
    void handleWebhook(String payload, Map<String, String> headers);
    SubscriptionStatus getStatus(String providerSubscriptionId);
}

public interface EntitlementService {
    boolean canReadChapter(UUID readerAccountId, UUID chapterId);
    boolean hasEarlyAccess(UUID readerAccountId, UUID seriesId);
}
```

V0 `EntitlementService` implementation: **always allow** (anonymous read).

### Integration points to wire in Phase 1.5

| Location | Hook |
|----------|------|
| Reader API | Check entitlement before chapter pages if `chapter.requires_subscription` (future column) |
| Publish visibility | Early access = unlisted + entitlement gate (V1/V2) |
| React reader | Login wall component stubbed behind feature flag |

**Recommended provider for India:** Razorpay Subscriptions or Stripe India — evaluate GST invoicing when implementing.

---

## Creator stipends (V0 admin portal)

Symbolic economics for seed creators — manual payouts, tracked in platform.

### Schema (Flyway `V7__creator_stipends`)

See [domain-model.md](./domain-model.md) — `creator_stipend`:

| Field | Purpose |
|-------|---------|
| `creator_id` | FK to user |
| `amount_inr` | e.g. 500–2000 |
| `period` | `2026-06` month string |
| `status` | `PENDING`, `PAID` |
| `recorded_by` | Admin user |

### Admin UI (V0 — ship)

- `/admin/stipends` — create entry, mark paid, filter by creator/period
- Export CSV for accountant
- No payment rails — founder pays via UPI/bank manually

---

## Early access / patron unlisted (V1/V2)

V0 behavior today:

- Chapter **published but unlisted** until `scheduled_at`
- **Direct link works for everyone**

V1/V2 extension:

| Feature | Mechanism |
|---------|-----------|
| Patron early access | `EntitlementService.hasEarlyAccess` gates direct link before `listed_at` |
| Tiered listing | `chapter.minimum_plan_code` column |
| Creator-managed tiers | Deferred |

No V0 code beyond entitlement interface returning `true`.

---

## Notification outbox (V1 prep)

Empty table in V0:

```sql
CREATE TABLE notification_outbox (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT now()
);
```

Used later for: new chapter email, subscription renewal, report acknowledgment.

---

## Feature flags for Phase 1.5

| Flag | Default |
|------|---------|
| `subscriptions_enabled` | `false` |
| `reader_login_required_for_premium` | `false` |
| `early_access_enabled` | `false` |

---

## Implementation order after V0 passes

1. Reader Google login (reuse OAuth) → `reader_account`
2. Razorpay checkout for one platform plan
3. Webhook handler → `subscription` status updates
4. Gate premium series/chapters via `EntitlementService`
5. Early access on unlisted chapters before catalog list time

---

## Related documents

- [domain-model.md](./domain-model.md) — stipend entity
- [scheduling-engine.md](./scheduling-engine.md) — publish visibility
- [identity-and-auth.md](./identity-and-auth.md) — future reader OAuth
- [solution-v0-overview.md](./solution-v0-overview.md) — Phase 1.5 product note
