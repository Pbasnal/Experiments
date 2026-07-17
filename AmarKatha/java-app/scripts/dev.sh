#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# Use project Java version (Spring Boot 3 requires 17+)
if [[ -f .java-version ]] && command -v asdf &>/dev/null; then
  export JAVA_HOME="$(asdf where java "$(tr -d '[:space:]' < .java-version)" 2>/dev/null || true)"
  if [[ -n "${JAVA_HOME}" ]]; then
    export PATH="${JAVA_HOME}/bin:${PATH}"
  fi
fi

# Load local env if present (docker compose reads .env from project root)
if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

MODE="${1:-run}"

start_db() {
  if ! command -v docker &>/dev/null; then
    echo "→ Docker not found; ensure PostgreSQL is running at ${POSTGRES_HOST:-localhost}:${POSTGRES_PORT:-5432}"
    return 0
  fi
  echo "→ Starting PostgreSQL (docker compose)..."
  docker compose up -d postgres
  echo "→ Waiting for PostgreSQL to be healthy..."
  for _ in $(seq 1 40); do
    if docker compose exec -T postgres pg_isready -U "${POSTGRES_USER:-amarkatha}" -d "${POSTGRES_DB:-amarkatha}" &>/dev/null; then
      echo "→ PostgreSQL is ready"
      return 0
    fi
    sleep 1
  done
  echo "PostgreSQL did not become healthy in time" >&2
  exit 1
}

build_reader() {
  echo "→ Building React reader..."
  cd reader
  if [[ ! -d node_modules ]]; then
    npm install
  fi
  npm run build
  cd "$ROOT"
  mkdir -p src/main/resources/static
  rm -rf src/main/resources/static/*
  cp -R reader/dist/* src/main/resources/static/
  echo "→ Copied reader/dist to src/main/resources/static/"
}

compose_up() {
  if ! command -v docker &>/dev/null; then
    echo "Docker is required for: $0 up" >&2
    exit 1
  fi
  echo "→ Building and starting full stack (postgres + app with React)..."
  docker compose up --build -d
  echo "→ Waiting for app health..."
  for _ in $(seq 1 90); do
    if curl -fsS "http://localhost:${SERVER_PORT:-8080}/actuator/health" &>/dev/null; then
      echo "→ Ready at http://localhost:${SERVER_PORT:-8080}"
      return 0
    fi
    sleep 2
  done
  echo "App did not become healthy in time. Check: docker compose logs -f app" >&2
  exit 1
}

compose_down() {
  docker compose down
}

case "$MODE" in
  db)
    start_db
    ;;
  build)
    build_reader
    ;;
  run)
    start_db
    build_reader
    echo "→ Starting Spring Boot on http://localhost:8080"
    mvn -s .mvn/settings.xml spring-boot:run -DskipTests
    ;;
  backend)
    start_db
    echo "→ Starting Spring Boot only (build reader first with: ./scripts/dev.sh build)"
    mvn -s .mvn/settings.xml spring-boot:run -DskipTests
    ;;
  frontend)
    echo "→ Vite dev server on http://localhost:5173 (proxies API to :8080)"
    cd reader
    npm install
    npm run dev
    ;;
  up)
    compose_up
    ;;
  down)
    compose_down
    ;;
  logs)
    docker compose logs -f "${2:-app}"
    ;;
  *)
    echo "Usage: $0 [db|build|run|backend|frontend|up|down|logs]"
    echo "  up     Full stack in Docker (Postgres + Spring Boot + React SPA)"
    echo "  down   Stop Docker Compose stack"
    echo "  logs   Tail container logs (default: app)"
    exit 1
    ;;
esac
