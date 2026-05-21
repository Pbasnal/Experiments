# Docker — local development

One Compose file runs the full stack for local testing: **PostgreSQL**, **Flask web app**, and **Redis** (reserved for future caching).

## Quick start

```bash
# First time (creates .env, starts containers, init DB, optional admin user)
./setup.sh

# Or manually
cp env.example .env
docker compose up --build -d
docker compose exec web flask init-db
docker compose exec web flask create-admin   # interactive
```

**App URL:** http://localhost:5000  
**Health check:** http://localhost:5000/health

## Everyday commands

| Task | Command |
|------|---------|
| Start | `docker compose up -d` or `./scripts/start.sh start` |
| Stop | `docker compose down` or `./scripts/start.sh stop` |
| Logs | `docker compose logs -f web` |
| Rebuild | `docker compose up --build -d` |
| DB shell | `docker compose exec postgres psql -U amarkatha_user -d amarkatha` |
| Flask shell | `docker compose exec web flask shell` |
| Migrations | `docker compose exec web flask db upgrade` |

See also `./scripts/dev.sh help` for backup, lint, and other helpers.

## Services

| Service | Port | Purpose |
|---------|------|---------|
| `web` | 5000 | Flask app (live code mount from project root) |
| `postgres` | 5432 | Primary database |
| `redis` | 6379 | Optional; not required by app code today |

## Environment

Copy `env.example` to `.env`. For Docker, `DATABASE_URL` is set in `docker-compose.yml` to use the `postgres` hostname. Defaults:

| Variable | Default |
|----------|---------|
| `POSTGRES_DB` | `amarkatha` |
| `POSTGRES_USER` | `amarkatha_user` |
| `POSTGRES_PASSWORD` | `amarkatha_password` |
| `WEB_PORT` | `5000` |

## Google OAuth (optional)

With `OAUTH_INSECURE_TRANSPORT=true` (default in compose), Google sign-in works over **HTTP** on port 5000.

Setup: [oauth-google.md](./oauth-google.md) — redirect URI must be `http://localhost:5000/google/authorized`.

## Troubleshooting

**Port 5432 or 5000 in use**

```bash
lsof -i :5432
lsof -i :5000
```

Change `POSTGRES_PORT` or `WEB_PORT` in `.env` if needed.

**Reset database (deletes all data)**

```bash
docker compose down -v
docker compose up -d
docker compose exec web flask init-db
```

**Web container unhealthy**

```bash
docker compose logs web
docker compose exec web curl -f http://localhost:5000/health
```

## Production

This repository ships a **local-dev-only** Compose setup. For production, use a managed database, secrets management, and a WSGI server (e.g. Gunicorn) behind a reverse proxy with TLS — not the dev `command: python run.py` override.
