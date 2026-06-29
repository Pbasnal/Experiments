# AmarKatha — Engineering (V0)

Solution design for the **Java greenfield V0** validation launch. Supersedes the Flask prototype in [ARCHITECTURE.md](../ARCHITECTURE.md) for new development.

**Product context:** [mvp-goal.md](../product/mvp-goal.md) · [publishing-tool-hl-prd.md](../product/publishing-tool-hl-prd.md) · [india-market-analysis-feedback.md](../product/india-market-analysis-feedback.md)

## Documents

| Document | Contents |
|----------|----------|
| [solution-v0-overview.md](./solution-v0-overview.md) | North star, constraints, decision log, scope deltas vs product docs |
| [architecture.md](./architecture.md) | Modular monolith, ECS (Dominion), request coalescing, module boundaries |
| [domain-model.md](./domain-model.md) | Entities, ERD, business rules, URL scheme |
| [scheduling-engine.md](./scheduling-engine.md) | Cadence, skip/hiatus, publish visibility, IST semantics |
| [frontend.md](./frontend.md) | Thymeleaf creator + React reader, i18n, OG tags |
| [media-pipeline.md](./media-pipeline.md) | Upload, WebP conversion, storage, bandwidth |
| [identity-and-auth.md](./identity-and-auth.md) | Google OAuth, invite tokens, roles, feature flags |
| [analytics-and-observability.md](./analytics-and-observability.md) | In-house events, reader cookie, Grafana |
| [infrastructure.md](./infrastructure.md) | Azure (Singapore), deployment, domain plan |
| [security-ops-legal.md](./security-ops-legal.md) | IT Rules, grievance, report/takedown |
| [scope-and-timeline.md](./scope-and-timeline.md) | 4-week plan, in/out scope, risks |
| [v1-foundation-hooks.md](./v1-foundation-hooks.md) | Subscriptions, stipends, early access — stubs only in V0 |

## Status

| Field | Value |
|-------|-------|
| Version | 0.1 |
| Last updated | June 2026 |
| Stack | Spring Boot 3 · Java 25 · PostgreSQL · React reader · Thymeleaf creator |
| Target ship | 4 weeks (solo + AI) |
