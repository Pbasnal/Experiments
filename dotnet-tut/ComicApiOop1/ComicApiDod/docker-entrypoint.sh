#!/bin/bash
set -e

# Wait for MySQL to be ready
until nc -z -v -w30 mysql 3306
do
    echo "Waiting for database connection..."
    sleep 5
done

# Run database migrations
echo "Running database migrations..."
dotnet ef database update --project ComicApiDod.csproj

# Start the application
echo "Starting the application..."
dotnet ComicApiDod.dll


