#!/usr/bin/env bash
# Start/stop Docker Compose: Flask app + Jellyfin in separate containers, shared ./downloads.
#
# Usage:
#   ./scripts/setup-jellyfin.sh          # start (default)
#   ./scripts/setup-jellyfin.sh up
#   ./scripts/setup-jellyfin.sh down
#   ./scripts/setup-jellyfin.sh logs     # follow all service logs
#   ./scripts/setup-jellyfin.sh logs-flask
#   ./scripts/setup-jellyfin.sh logs-jellyfin
#
# URLs:
#   Flask:  http://localhost:5001
#   Jellyfin: http://localhost:8096
# Jellyfin library folder (inside container): /media/downloads

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/jellyfin-docker-compose.yml"
ACTION="${1:-up}"

cd "${REPO_ROOT}"

if ! command -v docker >/dev/null 2>&1; then
  echo "error: docker is not installed or not on PATH" >&2
  exit 1
fi

DOCKER_COMPOSE=(docker compose)
if ! docker compose version >/dev/null 2>&1; then
  if command -v docker-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE=(docker-compose)
  else
    echo "error: need 'docker compose' (v2) or docker-compose" >&2
    exit 1
  fi
fi

mkdir -p \
  "${REPO_ROOT}/downloads" \
  "${REPO_ROOT}/data/jellyfin-config" \
  "${REPO_ROOT}/data/jellyfin-cache" \
  "${REPO_ROOT}/data/app-data"

run_compose() {
  "${DOCKER_COMPOSE[@]}" --project-directory "${REPO_ROOT}" -f "${COMPOSE_FILE}" "$@"
}

case "${ACTION}" in
  up|start)
    echo "Starting Flask + Jellyfin (separate containers, shared downloads/)..."
    run_compose build flask-app
    run_compose pull jellyfin
    run_compose up -d
    echo ""
    echo "Flask app:  http://localhost:5001"
    echo "Jellyfin:   http://localhost:8096"
    echo ""
    echo "Shared media folder on host: ${REPO_ROOT}/downloads"
    echo "Inside both containers it is mounted at: /media/downloads"
    echo ""
    echo "In Jellyfin, add a library pointing to: /media/downloads"
    echo ""
    echo "Logs (all):     ./scripts/setup-jellyfin.sh logs"
    echo "Logs (Flask):   ./scripts/setup-jellyfin.sh logs-flask"
    echo "Logs (Jellyfin): ./scripts/setup-jellyfin.sh logs-jellyfin"
    echo "Stop:           ./scripts/setup-jellyfin.sh down"
    ;;
  down|stop)
    echo "Stopping Flask + Jellyfin..."
    run_compose down
    ;;
  logs)
    run_compose logs -f
    ;;
  logs-flask)
    run_compose logs -f flask-app
    ;;
  logs-jellyfin)
    run_compose logs -f jellyfin
    ;;
  *)
    echo "usage: $0 [up|down|logs|logs-flask|logs-jellyfin]" >&2
    exit 1
    ;;
esac
