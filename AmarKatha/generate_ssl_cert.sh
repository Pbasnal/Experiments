#!/bin/bash

# AmarKatha SSL Certificate Generator
# This script generates self-signed SSL certificates for localhost HTTPS development

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

# Check if OpenSSL is installed
check_openssl() {
    print_status "Checking OpenSSL installation..."
    
    if ! command_exists openssl; then
        print_error "OpenSSL is not installed. Please install OpenSSL first."
        print_status "On macOS: brew install openssl"
        print_status "On Ubuntu/Debian: sudo apt-get install openssl"
        print_status "On CentOS/RHEL: sudo yum install openssl"
        exit 1
    fi
    
    print_success "OpenSSL is available!"
}

# Create SSL directory
create_ssl_directory() {
    print_status "Creating SSL certificates directory..."
    
    if [ ! -d "ssl" ]; then
        mkdir -p ssl
        print_success "Created ssl/ directory"
    else
        print_status "SSL directory already exists"
    fi
}

# Generate SSL certificate
generate_certificate() {
    print_status "Generating self-signed SSL certificate..."
    
    # Generate private key
    openssl genrsa -out ssl/localhost.key 2048
    
    # Generate certificate signing request
    openssl req -new -key ssl/localhost.key -out ssl/localhost.csr -subj "/C=US/ST=State/L=City/O=AmarKatha/OU=Development/CN=localhost"
    
    # Generate self-signed certificate
    openssl x509 -req -days 365 -in ssl/localhost.csr -signkey ssl/localhost.key -out ssl/localhost.crt
    
    # Clean up CSR file
    rm ssl/localhost.csr
    
    print_success "SSL certificate generated successfully!"
}

# Set proper permissions
set_permissions() {
    print_status "Setting proper permissions for SSL files..."
    
    chmod 600 ssl/localhost.key
    chmod 644 ssl/localhost.crt
    
    print_success "SSL file permissions set correctly!"
}

# Update .gitignore
update_gitignore() {
    print_status "Updating .gitignore to exclude SSL files..."
    
    if [ -f ".gitignore" ]; then
        if ! grep -q "ssl/" .gitignore; then
            echo "" >> .gitignore
            echo "# SSL certificates" >> .gitignore
            echo "ssl/" >> .gitignore
            print_success "Added ssl/ to .gitignore"
        else
            print_status "SSL directory already in .gitignore"
        fi
    else
        print_warning ".gitignore not found, creating one..."
        echo "# SSL certificates" > .gitignore
        echo "ssl/" >> .gitignore
        print_success "Created .gitignore with SSL exclusion"
    fi
}

# Display instructions
show_instructions() {
    print_success "SSL certificate generation completed!"
    echo ""
    print_status "Next steps:"
    echo "1. Update your Google OAuth settings:"
    echo "   - Add https://localhost:5002 to Authorized JavaScript origins"
    echo "   - Add https://localhost:5002/auth/google/authorized to Authorized redirect URIs"
    echo ""
    echo "2. To run with HTTPS:"
    echo "   - Traditional: python run.py --https --port 5002"
    echo "   - Docker: ./scripts/start.sh start-https"
    echo ""
    echo "3. Access your application at: https://localhost:5002"
    echo ""
    print_warning "Note: You'll see a browser warning about the self-signed certificate."
    print_warning "This is normal for development. Click 'Advanced' and 'Proceed to localhost'."
}

# Main execution
main() {
    echo "🔐 AmarKatha SSL Certificate Generator"
    echo "======================================"
    echo ""
    
    check_openssl
    create_ssl_directory
    generate_certificate
    set_permissions
    update_gitignore
    show_instructions
}

# Run main function
main "$@" 