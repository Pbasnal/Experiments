#!/bin/bash

# AmarKatha Docker Setup Script
# This script sets up the entire AmarKatha application using Docker Compose

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
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

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check Docker and Docker Compose
check_docker() {
    print_status "Checking Docker installation..."
    
    if ! command_exists docker; then
        print_error "Docker is not installed. Please install Docker Desktop for macOS first."
        print_status "Visit: https://docs.docker.com/desktop/install/mac-install/"
        exit 1
    fi
    
    if ! command_exists docker-compose; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    # Check if Docker daemon is running
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker daemon is not running. Please start Docker Desktop."
        exit 1
    fi
    
    print_success "Docker and Docker Compose are ready!"
}

# Function to create .env file
setup_environment() {
    print_status "Setting up environment variables..."
    
    if [ ! -f .env ]; then
        if [ -f env.example ]; then
            cp env.example .env
            print_success "Created .env file from env.example"
        else
            print_warning "env.example not found, creating basic .env file..."
            cat > .env << EOF
# Flask Configuration
SECRET_KEY=$(python3 -c 'import secrets; print(secrets.token_hex(32))')
FLASK_ENV=development
FLASK_DEBUG=1

# Database Configuration
DATABASE_URL=postgresql://amarkatha_user:amarkatha_password@postgres:5432/amarkatha

# File Upload Configuration
MAX_CONTENT_LENGTH=16777216
UPLOAD_FOLDER=app/static/uploads

# PostgreSQL Docker Configuration
POSTGRES_DB=amarkatha
POSTGRES_USER=amarkatha_user
POSTGRES_PASSWORD=amarkatha_password
POSTGRES_HOST=postgres
POSTGRES_PORT=5432

# Redis Configuration
REDIS_URL=redis://redis:6379/0

# Docker Configuration
DOCKER_ENV=development
EOF
            print_success "Created .env file with default values"
        fi
    else
        print_warning ".env file already exists, skipping..."
    fi
}

# Function to create necessary directories
create_directories() {
    print_status "Creating necessary directories..."
    
    mkdir -p app/static/uploads
    mkdir -p logs
    
    print_success "Directories created!"
}

# Function to generate SSL certificates
generate_ssl_certificates() {
    print_status "Checking SSL certificates..."
    
    if [ ! -f "ssl/localhost.crt" ] || [ ! -f "ssl/localhost.key" ]; then
        print_status "Generating SSL certificates..."
        ./generate_ssl_cert.sh
        print_success "SSL certificates generated!"
    else
        print_success "SSL certificates already exist!"
    fi
}

# Function to build and start containers
start_containers() {
    print_status "Building and starting Docker containers..."
    
    # Stop any existing containers
    docker-compose down 2>/dev/null || true
    
    # Build and start containers
    docker-compose up --build -d
    
    print_success "Containers started successfully!"
}

# Function to wait for services to be ready
wait_for_services() {
    print_status "Waiting for services to be ready..."
    
    # Wait for PostgreSQL
    print_status "Waiting for PostgreSQL..."
    timeout=60
    counter=0
    while ! docker-compose exec -T postgres pg_isready -U amarkatha_user -d amarkatha >/dev/null 2>&1; do
        sleep 2
        counter=$((counter + 2))
        if [ $counter -ge $timeout ]; then
            print_error "PostgreSQL failed to start within $timeout seconds"
            exit 1
        fi
    done
    print_success "PostgreSQL is ready!"
    
    # Wait for Flask app (check both HTTP and HTTPS)
    print_status "Waiting for Flask application..."
    timeout=60
    counter=0
    while ! curl -f http://localhost:5000/health >/dev/null 2>&1 && ! curl -k -f https://localhost:5002/health >/dev/null 2>&1; do
        sleep 2
        counter=$((counter + 2))
        if [ $counter -ge $timeout ]; then
            print_error "Flask application failed to start within $timeout seconds"
            exit 1
        fi
    done
    print_success "Flask application is ready!"
}

# Function to initialize database
initialize_database() {
    print_status "Initializing database..."
    
    # Initialize database tables
    docker-compose exec -T web flask init-db
    
    print_success "Database initialized!"
}

# Function to create admin user
create_admin_user() {
    print_status "Setting up admin user..."
    
    echo ""
    print_warning "You will be prompted to create an admin user."
    print_warning "This is required to access the admin panel."
    echo ""
    
    read -p "Do you want to create an admin user now? (y/n): " -n 1 -r
    echo ""
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose exec -T web flask create-admin
        print_success "Admin user created!"
    else
        print_warning "Skipping admin user creation. You can create one later with:"
        print_warning "docker-compose exec web flask create-admin"
    fi
}

# Function to show status
show_status() {
    print_status "Checking container status..."
    docker-compose ps
    
    echo ""
    print_success "AmarKatha is now running!"
    echo ""
    echo -e "${GREEN}Access URLs:${NC}"
    echo -e "  HTTP (redirects to HTTPS): ${BLUE}http://localhost:5000${NC}"
    echo -e "  HTTPS: ${BLUE}https://localhost:5002${NC}"
    echo -e "  Creator Dashboard: ${BLUE}https://localhost:5002/creator/dashboard${NC}"
    echo -e "  Admin Panel: ${BLUE}https://localhost:5002/admin/dashboard${NC}"
    echo ""
    print_warning "Note: You'll see a browser warning about the self-signed certificate."
    print_warning "This is normal for development. Click 'Advanced' and 'Proceed to localhost'."
    echo ""
    echo -e "${GREEN}Useful Commands:${NC}"
    echo -e "  View logs: ${BLUE}docker-compose logs -f${NC}"
    echo -e "  Stop services: ${BLUE}docker-compose down${NC}"
    echo -e "  Restart services: ${BLUE}docker-compose restart${NC}"
    echo -e "  Access database: ${BLUE}docker-compose exec postgres psql -U amarkatha_user -d amarkatha${NC}"
    echo ""
}

# Function to clean up on error
cleanup() {
    print_error "Setup failed. Cleaning up..."
    docker-compose down 2>/dev/null || true
    exit 1
}

# Set up error handling
trap cleanup ERR

# Main setup function
main() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  AmarKatha Docker Setup${NC}"
    echo -e "${BLUE}================================${NC}"
    echo ""
    
    check_docker
    setup_environment
    create_directories
    generate_ssl_certificates
    start_containers
    wait_for_services
    initialize_database
    create_admin_user
    show_status
    
    echo -e "${GREEN}Setup completed successfully!${NC}"
}

# Check if script is run with arguments
case "${1:-}" in
    --help|-h)
        echo "Usage: $0 [OPTIONS]"
        echo ""
        echo "Options:"
        echo "  --help, -h     Show this help message"
        echo "  --skip-admin   Skip admin user creation"
        echo "  --prod         Use production configuration"
        echo ""
        echo "Examples:"
        echo "  $0              # Full setup with admin user creation"
        echo "  $0 --skip-admin # Setup without admin user creation"
        echo "  $0 --prod       # Production setup"
        exit 0
        ;;
    --skip-admin)
        SKIP_ADMIN=true
        main
        ;;
    --prod)
        print_status "Using production configuration..."
        export COMPOSE_FILE="docker-compose.yml:docker-compose.prod.yml"
        main
        ;;
    "")
        main
        ;;
    *)
        print_error "Unknown option: $1"
        print_error "Use --help for usage information"
        exit 1
        ;;
esac 