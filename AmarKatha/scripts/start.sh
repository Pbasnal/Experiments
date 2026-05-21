#!/bin/bash
# AmarKatha — start/stop local Docker stack (single docker-compose.yml)

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Prefer "docker compose" (v2), fall back to docker-compose
dc() {
    if docker compose version >/dev/null 2>&1; then
        docker compose "$@"
    else
        docker-compose "$@"
    fi
}

case "${1:-start}" in
    start)
        print_status "Starting AmarKatha..."
        dc up -d
        print_success "Running at http://localhost:5000"
        ;;
    stop)
        print_status "Stopping AmarKatha..."
        dc down
        print_success "Stopped."
        ;;
    restart)
        print_status "Restarting AmarKatha..."
        dc restart
        print_success "Restarted."
        ;;
    logs)
        dc logs -f "${2:-}"
        ;;
    status)
        dc ps
        ;;
    build)
        print_status "Rebuilding containers..."
        dc up --build -d
        print_success "Rebuild complete. http://localhost:5000"
        ;;
    clean)
        print_warning "This removes all containers and volumes (database data)."
        read -p "Are you sure? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            dc down -v
            print_success "Removed containers and volumes."
        fi
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|logs|status|build|clean}"
        echo ""
        echo "  start    - docker compose up -d"
        echo "  stop     - docker compose down"
        echo "  restart  - restart services"
        echo "  logs     - follow logs (optional service: web, postgres, redis)"
        echo "  status   - container status"
        echo "  build    - rebuild and start"
        echo "  clean    - down -v (wipes DB volume)"
        exit 1
        ;;
esac
