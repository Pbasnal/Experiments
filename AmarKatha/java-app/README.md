# AmarKatha (Java V0)

Greenfield validation launch for an Indian comics platform: **publish on your rhythm, share a link, readers know when you're back.**

## Quick start

### Full stack in Docker (recommended)

```bash
cp env.example .env
chmod +x scripts/dev.sh
./scripts/dev.sh up
```

Open **http://localhost:8080** — Postgres + Spring Boot + React reader in containers.

Stop with `./scripts/dev.sh down`. Details: [`docs/docker.md`](docs/docker.md).

### Host-native (Postgres in Docker, app on host)

```bash
./scripts/dev.sh run
```

### What you'll see

- **Homepage** — hero, value props, mock chronological catalog (6 series)
- **Platform map** — every V0 route with Preview / Planned status; clickable links
- **Series hub preview** — click any series card → schedule strip + chapter list stub
- **Creator portal** — `/creator` (Thymeleaf)
- **Admin portal** — `/admin` (invite tokens)

### Dev modes

| Command | Purpose |
|---------|---------|
| `./scripts/dev.sh up` | Full stack in Docker Compose |
| `./scripts/dev.sh down` | Stop Compose stack |
| `./scripts/dev.sh logs` | Tail app container logs |
| `./scripts/dev.sh run` | Host Spring Boot + Postgres container |
| `./scripts/dev.sh db` | Start PostgreSQL only |
| `./scripts/dev.sh build` | Build reader only, copy to `static/` |
| `./scripts/dev.sh frontend` | Vite hot reload on :5173 |

Copy `env.example` to `.env` and set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `ADMIN_BOOTSTRAP_EMAILS` for auth.

### Creator / admin auth flow

1. Set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `ADMIN_BOOTSTRAP_EMAILS` in `.env`
2. **Sign up** (`/creator/signup`): optional invite code → Continue with Google
   - Email in `ADMIN_BOOTSTRAP_EMAILS` → **admin** → `/admin` (invite not required)
   - New creator with valid invite → `/creator`
3. **Sign in** (`/creator/login`): Continue with Google for existing accounts
4. Admins generate invites at `/admin/invites` (no public Admin nav)

Health check: http://localhost:8080/actuator/health

## Stack

- **Backend:** Spring Boot 3.4, Java 17+
- **Reader UI:** React 19 + Vite + TypeScript
- **Creator/Admin:** Thymeleaf (placeholders for now)

## Docs

See [`docs/README.md`](docs/README.md) for product and engineering specs.

## Project tracker

Obsidian: `Work/2_Projects/AmarKatha/AmarKatha V0.md`
