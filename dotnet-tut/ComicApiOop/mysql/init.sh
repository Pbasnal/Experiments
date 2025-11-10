#!/bin/bash
set -e

# Wait for MySQL to be ready
until mysql -h"$MYSQL_HOST" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" >/dev/null 2>&1; do
  echo "Waiting for MySQL to be ready..."
  sleep 1
done

# Run the seed script
mysql -h"$MYSQL_HOST" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/SeedData.sql
