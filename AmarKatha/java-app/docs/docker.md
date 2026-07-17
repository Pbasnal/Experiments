# Docker — local development (Java V0)

One Compose file runs the full stack: **PostgreSQL** + **Spring Boot app** (serves the React reader SPA).

## Quick start

```bash
cp env.example .env
# Optional: set GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET / ADMIN_BOOTSTRAP_EMAILS

./scripts/dev.sh up
# or
docker compose up --build -d
```

**App URL:** http://localhost:8080  
**Health check:** http://localhost:8080/actuator/health

## Everyday commands

| Task | Command |
|------|---------|
| Start (build + run) | `./scripts/dev.sh up` or `docker compose up --build -d` |
| Stop | `./scripts/dev.sh down` or `docker compose down` |
| Logs | `./scripts/dev.sh logs` or `docker compose logs -f app` |
| Postgres only | `./scripts/dev.sh db` |
| Host-native app | `./scripts/dev.sh run` |

## Services

| Service | Port | Purpose |
|---------|------|---------|
| `app` | 8080 | Spring Boot + bundled React reader |
| `postgres` | 5432 | Primary database |

The React frontend is built inside the Docker image (multi-stage) and served as static assets by Spring Boot — same origin, no separate nginx container in V0.

## Environment

Copy `env.example` to `.env`. Inside Compose, `POSTGRES_HOST` is forced to `postgres` for the app container.

| Variable | Default | Notes |
|----------|---------|-------|
| `SERVER_PORT` | `8080` | Host port mapped to app |
| `POSTGRES_*` | `amarkatha` | DB name/user/password |
| `GOOGLE_CLIENT_ID` | placeholder | Required for real OAuth |
| `GOOGLE_CLIENT_SECRET` | placeholder | Required for real OAuth |
| `ADMIN_BOOTSTRAP_EMAILS` | empty | Comma-separated admin emails |

## Troubleshooting

**Port 5432 or 8080 in use**

```bash
lsof -i :5432
lsof -i :8080
```

Change `POSTGRES_PORT` or `SERVER_PORT` in `.env`.

**App unhealthy / OAuth errors**

```bash
docker compose logs -f app
```

Set real Google credentials in `.env` and recreate: `docker compose up -d --force-recreate app`.

**Reset database (deletes all data)**

```bash
docker compose down -v
./scripts/dev.sh up
```

## Production

This Compose setup is for **local/dev**. Production adds Nginx TLS, managed Postgres, secrets management, and Azure Blob — see `docs/engineering/infrastructure.md`.
