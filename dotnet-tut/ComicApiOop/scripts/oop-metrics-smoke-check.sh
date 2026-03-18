#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
COMPUTE_PATH="${COMPUTE_PATH:-/api/comics/compute-visibilities}"
START_ID="${START_ID:-1}"
LIMIT="${LIMIT:-1}"
WAIT_SECONDS="${WAIT_SECONDS:-6}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

check_contains() {
  local pattern="$1"
  if ! grep -Eq "${pattern}" <<<"${METRICS}"; then
    fail "Missing expected metrics pattern: ${pattern}"
  fi
}

echo "==> Checking API health at ${BASE_URL}/health"
curl -fsS "${BASE_URL}/health" >/dev/null

echo "==> Triggering compute endpoint (${COMPUTE_PATH}?startId=${START_ID}&limit=${LIMIT})"
# Endpoint is expected to succeed, but for smoke checks we only care that it executes
# enough instrumentation to emit the key gauge/counter series.
curl -fsS "${BASE_URL}${COMPUTE_PATH}?startId=${START_ID}&limit=${LIMIT}" >/dev/null || true

echo "==> Waiting ${WAIT_SECONDS}s for hosted runtime metrics"
sleep "${WAIT_SECONDS}"

echo "==> Fetching ${BASE_URL}/metrics"
METRICS="$(curl -fsS "${BASE_URL}/metrics")"

echo "==> Verifying expected OOP /metrics series"

# HTTP request metrics (custom middleware)
check_contains 'metric="api_http_requests_total".*api_type="OOP"'
check_contains 'metric="http_request_duration_seconds".*api_type="OOP"'

# DB/EF query metrics (Common standardized names)
check_contains 'metric="db_query_duration_seconds".*api_type="OOP"'
check_contains 'metric="db_query_count_total".*api_type="OOP"'

# Runtime metrics
check_contains 'metric="dotnet_gc_collection_count".*api_type="OOP".*generation="0"'
check_contains 'metric="ef_change_tracker_entities".*api_type="OOP"'

# Per-operation memory gauge
check_contains 'metric="memory_allocated_bytes_per_operation".*api_type="OOP"'

echo "OK: Expected metrics are present."

