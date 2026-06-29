# AmarKatha (Java V0)

Greenfield validation launch for an Indian comics platform: **publish on your rhythm, share a link, readers know when you're back.**

## Quick start — homepage UI preview

```bash
# One command: build React reader + start Spring Boot
chmod +x scripts/dev.sh
./scripts/dev.sh run
```

Open **http://localhost:8080**

### What you'll see

- **Homepage** — hero, value props, mock chronological catalog (6 series)
- **Platform map** — every V0 route with Preview / Planned status; clickable links
- **Series hub preview** — click any series card → schedule strip + chapter list stub
- **Creator portal** — `/creator` (Thymeleaf placeholder)
- **Admin portal** — `/admin` (Thymeleaf placeholder)

### Dev modes

| Command | Purpose |
|---------|---------|
| `./scripts/dev.sh run` | Build reader + Spring Boot (recommended) |
| `./scripts/dev.sh build` | Build reader only, copy to `static/` |
| `./scripts/dev.sh frontend` | Vite hot reload on :5173 (run backend separately) |
| `./mvnw spring-boot:run` | Backend only (after `./scripts/dev.sh build`) |

Health check: http://localhost:8080/actuator/health

## Stack

- **Backend:** Spring Boot 3.4, Java 17+
- **Reader UI:** React 19 + Vite + TypeScript
- **Creator/Admin:** Thymeleaf (placeholders for now)

## Docs

See [`docs/README.md`](docs/README.md) for product and engineering specs.

## Project tracker

Obsidian: `Work/2_Projects/AmarKatha/AmarKatha V0.md`
