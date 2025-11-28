#!/bin/bash
set -e

echo "Starting to seed data..."
# Check if the seed directory exists, create it if not
#mkdir -p /docker-entrypoint-initdb.d/seed

# Copy seed data from the known location
#cp /docker-entrypoint-initdb.d/SeedData.sql /docker-entrypoint-initdb.d/seed/SeedData.sql

# Seed the database
mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/seed/SeedData.sql
echo "Data seeding completed!"
