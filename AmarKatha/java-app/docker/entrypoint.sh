#!/bin/sh
set -e

MEDIA_ROOT="${AMARKATHA_MEDIA_ROOT:-/data/media}"
mkdir -p "$MEDIA_ROOT"
chown -R amarkatha:amarkatha "$MEDIA_ROOT" 2>/dev/null || true

exec runuser -u amarkatha -- java -jar /app/app.jar
