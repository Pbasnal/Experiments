from app import create_app, db
from app.models import User, Comic, Chapter, ChapterPage, Comment, Rating, ChapterRating, Follow, ComicFollow, ViewLog
import os
import argparse
import ssl

app = create_app()

@app.route('/health')
def health_check():
    """Health check endpoint for Docker."""
    return {'status': 'healthy', 'message': 'AmarKatha is running'}, 200

@app.shell_context_processor
def make_shell_context():
    return {
        'db': db,
        'User': User,
        'Comic': Comic,
        'Chapter': Chapter,
        'ChapterPage': ChapterPage,
        'Comment': Comment,
        'Rating': Rating,
        'ChapterRating': ChapterRating,
        'Follow': Follow,
        'ComicFollow': ComicFollow,
        'ViewLog': ViewLog
    }

@app.cli.command()
def init_db():
    """Initialize the database."""
    db.create_all()
    print('Database initialized!')

@app.cli.command()
def create_admin():
    """Create an admin user."""
    from werkzeug.security import generate_password_hash
    
    username = input('Enter admin username: ')
    email = input('Enter admin email: ')
    password = input('Enter admin password: ')
    
    user = User(
        username=username,
        email=email,
        password_hash=generate_password_hash(password),
        is_artist=True
    )
    
    db.session.add(user)
    db.session.commit()
    print(f'Admin user {username} created successfully!')

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

if __name__ == '__main__':
    # Parse command line arguments
    parser = argparse.ArgumentParser(description='AmarKatha Flask Application')
    parser.add_argument('--https', action='store_true', help='Run with HTTPS (requires SSL certificates)')
    parser.add_argument('--host', default='0.0.0.0', help='Host to bind to (default: 0.0.0.0)')
    parser.add_argument('--port', type=int, default=5000, help='Port to bind to (default: 5000)')
    parser.add_argument('--debug', action='store_true', help='Run in debug mode')
    
    args = parser.parse_args()
    
    # Configure for deployment
    host = os.environ.get('HOST', args.host)
    port = int(os.environ.get('PORT', args.port))
    debug = args.debug or os.environ.get('FLASK_ENV') == 'development'
    
    if args.https:
        if check_ssl_files():
            print("🔐 Starting AmarKatha with HTTPS...")
            print(f"🌐 Access at: https://{host}:{port}")
            print("⚠️  Note: You'll see a browser warning about the self-signed certificate.")
            print("   This is normal for development. Click 'Advanced' and 'Proceed to localhost'.")
            
            # Create SSL context
            context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
            context.load_cert_chain('ssl/localhost.crt', 'ssl/localhost.key')
            
            # Run with SSL context
            app.run(host=host, port=port, debug=debug, ssl_context=context)
        else:
            print("❌ Cannot start with HTTPS - SSL certificates missing")
            exit(1)
    else:
        print("🌐 Starting AmarKatha with HTTP...")
        print(f"🌐 Access at: http://{host}:{port}")
        app.run(host=host, port=port, debug=debug) 