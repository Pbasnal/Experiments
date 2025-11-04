#!/bin/bash

# AmarKatha Quick Start Script
# Simple script to start/stop/restart the application

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

check_ssl_certificates() {
    if [ ! -f "ssl/localhost.crt" ] || [ ! -f "ssl/localhost.key" ]; then
        print_error "SSL certificates not found!"
        print_status "Run: ./generate_ssl_cert.sh to generate SSL certificates"
        return 1
    fi
    return 0
}

case "${1:-start}" in
    start)
        print_status "Starting AmarKatha with HTTPS..."
        if check_ssl_certificates; then
            docker-compose up -d
            print_success "AmarKatha started with HTTPS!"
            print_success "HTTP (redirects to HTTPS): http://localhost:5000"
            print_success "HTTPS: https://localhost:5002"
            print_warning "Note: You'll see a browser warning about the self-signed certificate."
            print_warning "This is normal for development. Click 'Advanced' and 'Proceed to localhost'."
        else
            print_error "SSL certificates not found!"
            print_status "Run: ./generate_ssl_cert.sh to generate SSL certificates"
            exit 1
        fi
        ;;
    start-http)
        print_status "Starting AmarKatha with HTTP only..."
        docker-compose -f docker-compose.yml -f docker-compose.http.yml up -d
        print_success "AmarKatha started with HTTP!"
        print_success "HTTP: http://localhost:5000"
        ;;
    start-https)
        if check_ssl_certificates; then
            print_status "Starting AmarKatha with HTTPS..."
            docker-compose -f docker-compose.yml -f docker-compose.https.yml up -d
            print_success "AmarKatha started with HTTPS!"
            print_success "HTTP (redirects to HTTPS): http://localhost:5000"
            print_success "HTTPS: https://localhost:5002"
            print_warning "Note: You'll see a browser warning about the self-signed certificate."
            print_warning "This is normal for development. Click 'Advanced' and 'Proceed to localhost'."
        else
            exit 1
        fi
        ;;
    stop)
        print_status "Stopping AmarKatha..."
        docker-compose down
        print_success "AmarKatha stopped!"
        ;;
    restart)
        print_status "Restarting AmarKatha..."
        docker-compose restart
        print_success "AmarKatha restarted!"
        ;;
    restart-https)
        if check_ssl_certificates; then
            print_status "Restarting AmarKatha with HTTPS..."
            docker-compose -f docker-compose.yml -f docker-compose.https.yml restart
            print_success "AmarKatha restarted with HTTPS!"
        else
            exit 1
        fi
        ;;
    logs)
        print_status "Showing logs..."
        docker-compose logs -f
        ;;
    logs-https)
        print_status "Showing HTTPS logs..."
        docker-compose -f docker-compose.yml -f docker-compose.https.yml logs -f
        ;;
    status)
        print_status "Container status:"
        docker-compose ps
        ;;
    status-https)
        print_status "HTTPS container status:"
        docker-compose -f docker-compose.yml -f docker-compose.https.yml ps
        ;;
    build)
        print_status "Rebuilding containers..."
        docker-compose up --build -d
        print_success "Containers rebuilt and started!"
        ;;
    build-https)
        if check_ssl_certificates; then
            print_status "Rebuilding containers with HTTPS..."
            docker-compose -f docker-compose.yml -f docker-compose.https.yml up --build -d
            print_success "Containers rebuilt and started with HTTPS!"
        else
            exit 1
        fi
        ;;
    ssl-check)
        print_status "Checking SSL certificates..."
        if check_ssl_certificates; then
            print_success "SSL certificates are present and valid!"
            print_status "Certificate: ssl/localhost.crt"
            print_status "Private key: ssl/localhost.key"
        else
            print_error "SSL certificates are missing or invalid!"
        fi
        ;;
    clean)
        print_warning "This will remove all containers and volumes!"
        read -p "Are you sure? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker-compose down -v
            print_success "All containers and volumes removed!"
        fi
        ;;
    *)
        echo "Usage: $0 {start|start-http|start-https|stop|restart|restart-https|logs|logs-https|status|status-https|build|build-https|ssl-check|clean}"
        echo ""
        echo "Commands:"
        echo "  start        - Start the application with HTTPS"
        echo "  start-http   - Start the application with HTTP only"
        echo "  start-https  - Start the application with HTTPS"
        echo "  stop         - Stop the application"
        echo "  restart      - Restart the application"
        echo "  restart-https - Restart the application with HTTPS"
        echo "  logs         - Show application logs"
        echo "  logs-https   - Show HTTPS application logs"
        echo "  status       - Show container status"
        echo "  status-https - Show HTTPS container status"
        echo "  build        - Rebuild and start containers"
        echo "  build-https  - Rebuild and start containers with HTTPS"
        echo "  ssl-check    - Check SSL certificate status"
        echo "  clean        - Remove all containers and volumes"
        echo ""
        echo "HTTPS Setup:"
        echo "  1. Run: ./generate_ssl_cert.sh to generate SSL certificates"
        echo "  2. Run: $0 start-https to start with HTTPS"
        echo "  3. Access at: https://localhost:5002"
        exit 1
        ;;
esac 