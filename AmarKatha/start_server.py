#!/usr/bin/env python3
"""
AmarKatha Server Startup Script
Runs both HTTP and HTTPS servers simultaneously
"""

import os
import ssl
import threading
import argparse
from app import create_app

def check_ssl_files():
    """Check if SSL certificate files exist"""
    cert_file = 'ssl/localhost.crt'
    key_file = 'ssl/localhost.key'
    
    if not os.path.exists(cert_file):
        print("❌ SSL certificate file not found: ssl/localhost.crt")
        print("💡 Run: ./generate_ssl_cert.sh to generate SSL certificates")
        return False
    
    if not os.path.exists(key_file):
        print("❌ SSL private key file not found: ssl/localhost.key")
        print("💡 Run: ./generate_ssl_cert.sh to generate SSL certificates")
        return False
    
    return True

def run_http_server(app, host, port):
    """Run HTTP server in a separate thread"""
    print(f"🌐 Starting HTTP server on http://{host}:{port}")
    app.run(host=host, port=port, debug=False, use_reloader=False)

def run_https_server(app, host, port):
    """Run HTTPS server in a separate thread"""
    if check_ssl_files():
        print(f"🔐 Starting HTTPS server on https://{host}:{port}")
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.load_cert_chain('ssl/localhost.crt', 'ssl/localhost.key')
        app.run(host=host, port=port, debug=False, use_reloader=False, ssl_context=context)
    else:
        print("❌ Cannot start HTTPS server - SSL certificates missing")

def main():
    parser = argparse.ArgumentParser(description='AmarKatha Server Startup')
    parser.add_argument('--http-port', type=int, default=5000, help='HTTP port (default: 5000)')
    parser.add_argument('--https-port', type=int, default=5002, help='HTTPS port (default: 5002)')
    parser.add_argument('--host', default='0.0.0.0', help='Host to bind to (default: 0.0.0.0)')
    parser.add_argument('--http-only', action='store_true', help='Run HTTP only')
    parser.add_argument('--https-only', action='store_true', help='Run HTTPS only')
    
    args = parser.parse_args()
    
    app = create_app()
    
    # Add health endpoint
    @app.route('/health')
    def health_check():
        """Health check endpoint for Docker."""
        return {'status': 'healthy', 'message': 'AmarKatha is running'}, 200
    
    print("🚀 Starting AmarKatha Server...")
    print(f"🌐 HTTP: http://{args.host}:{args.http_port}")
    print(f"🔐 HTTPS: https://{args.host}:{args.https_port}")
    print("⚠️  Note: You'll see a browser warning about the self-signed certificate.")
    print("   This is normal for development. Click 'Advanced' and 'Proceed to localhost'.")
    
    if args.http_only:
        # HTTP only
        run_http_server(app, args.host, args.http_port)
    elif args.https_only:
        # HTTPS only
        run_https_server(app, args.host, args.https_port)
    else:
        # Both HTTP and HTTPS
        http_thread = threading.Thread(target=run_http_server, args=(app, args.host, args.http_port))
        https_thread = threading.Thread(target=run_https_server, args=(app, args.host, args.https_port))
        
        http_thread.daemon = True
        https_thread.daemon = True
        
        http_thread.start()
        https_thread.start()
        
        # Keep main thread alive
        try:
            while True:
                import time
                time.sleep(1)
        except KeyboardInterrupt:
            print("\n🛑 Shutting down servers...")

if __name__ == '__main__':
    main() 