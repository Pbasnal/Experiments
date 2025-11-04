#!/bin/bash

# AmarKatha Development Script
# Script for common development tasks

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

case "${1:-help}" in
    shell)
        print_status "Opening Flask shell..."
        docker-compose exec web flask shell
        ;;
    db-shell)
        print_status "Opening PostgreSQL shell..."
        docker-compose exec postgres psql -U amarkatha_user -d amarkatha
        ;;
    init-db)
        print_status "Initializing database..."
        docker-compose exec web flask init-db
        print_success "Database initialized!"
        ;;
    create-admin)
        print_status "Creating admin user..."
        docker-compose exec web flask create-admin
        ;;
    migrate)
        print_status "Running database migrations..."
        docker-compose exec web flask db upgrade
        ;;
    test)
        print_status "Running tests..."
        docker-compose exec web python -m pytest
        ;;
    lint)
        print_status "Running linting..."
        docker-compose exec web flake8 .
        ;;
    format)
        print_status "Formatting code..."
        docker-compose exec web black .
        ;;
    logs-web)
        print_status "Showing web container logs..."
        docker-compose logs -f web
        ;;
    logs-db)
        print_status "Showing database logs..."
        docker-compose logs -f postgres
        ;;
    backup)
        print_status "Creating database backup..."
        docker-compose exec postgres pg_dump -U amarkatha_user amarkatha > backup_$(date +%Y%m%d_%H%M%S).sql
        print_success "Backup created!"
        ;;
    restore)
        if [ -z "$2" ]; then
            print_error "Please specify backup file: $0 restore <backup_file.sql>"
            exit 1
        fi
        print_status "Restoring database from $2..."
        docker-compose exec -T postgres psql -U amarkatha_user -d amarkatha < "$2"
        print_success "Database restored!"
        ;;
    reset-db)
        print_warning "This will delete all data in the database!"
        read -p "Are you sure? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            print_status "Resetting database..."
            docker-compose exec web flask init-db
            print_success "Database reset!"
        fi
        ;;
    install-deps)
        print_status "Installing new dependencies..."
        docker-compose exec web pip install -r requirements.txt
        print_success "Dependencies installed!"
        ;;
    help)
        echo "Usage: $0 {command}"
        echo ""
        echo "Development Commands:"
        echo "  shell         - Open Flask shell"
        echo "  db-shell      - Open PostgreSQL shell"
        echo "  init-db       - Initialize database tables"
        echo "  create-admin  - Create admin user"
        echo "  migrate       - Run database migrations"
        echo "  test          - Run tests"
        echo "  lint          - Run code linting"
        echo "  format        - Format code with black"
        echo "  logs-web      - Show web container logs"
        echo "  logs-db       - Show database logs"
        echo "  backup        - Create database backup"
        echo "  restore <file> - Restore database from backup"
        echo "  reset-db      - Reset database (delete all data)"
        echo "  install-deps  - Install new dependencies"
        echo "  help          - Show this help message"
        ;;
    *)
        print_error "Unknown command: $1"
        print_error "Use '$0 help' for available commands"
        exit 1
        ;;
esac 