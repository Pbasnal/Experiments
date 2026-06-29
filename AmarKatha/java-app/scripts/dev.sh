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

MODE="${1:-run}"

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

case "$MODE" in
  build)
    build_reader
    ;;
  run)
    build_reader
    echo "→ Starting Spring Boot on http://localhost:8080"
    mvn -s .mvn/settings.xml spring-boot:run -DskipTests
    ;;
  backend)
    echo "→ Starting Spring Boot only (build reader first with: ./scripts/dev.sh build)"
    mvn -s .mvn/settings.xml spring-boot:run -DskipTests
    ;;
  frontend)
    echo "→ Vite dev server on http://localhost:5173 (proxies API to :8080)"
    cd reader
    npm install
    npm run dev
    ;;
  *)
    echo "Usage: $0 [build|run|backend|frontend]"
    exit 1
    ;;
esac
