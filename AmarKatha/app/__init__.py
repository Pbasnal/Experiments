from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from flask_login import LoginManager
from flask_migrate import Migrate
from config import get_config
import os
import json

db = SQLAlchemy()
migrate = Migrate()
login_manager = LoginManager()
login_manager.login_view = 'auth.login'
login_manager.login_message = 'Please log in to access this page.'

def load_google_credentials():
    """Load Google OAuth credentials from JSON file"""
    try:
        # Try different possible filenames
        credential_files = [
            'google_credentials.json',
            'credentials.json',
            'client_secret.json'
        ]
        
        for filename in credential_files:
            if os.path.exists(filename):
                with open(filename, 'r') as f:
                    creds = json.load(f)
                    # Handle different credential file formats
                    if 'web' in creds:
                        # Standard Google OAuth format
                        return creds['web']['client_id'], creds['web']['client_secret']
                    elif 'client_id' in creds:
                        # Direct format
                        return creds['client_id'], creds['client_secret']
        
        # Fallback to environment variables
        return os.environ.get('GOOGLE_OAUTH_CLIENT_ID'), os.environ.get('GOOGLE_OAUTH_CLIENT_SECRET')
    except Exception as e:
        print(f"Warning: Could not load Google credentials: {e}")
        return None, None

def create_app(config_class=None):
    app = Flask(__name__)
    
    # Use config factory if no specific config provided
    if config_class is None:
        config_class = get_config()
    
    app.config.from_object(config_class)

    # Set OAuth transport security based on config
    if app.config['OAUTH_INSECURE_TRANSPORT']:
        os.environ['OAUTHLIB_INSECURE_TRANSPORT'] = '1'
    else:
        os.environ.pop('OAUTHLIB_INSECURE_TRANSPORT', None)

    db.init_app(app)
    migrate.init_app(app, db)
    login_manager.init_app(app)

    # Register blueprints
    from app.routes import main, auth, creator, admin
    app.register_blueprint(main.bp)
    app.register_blueprint(auth.bp)
    app.register_blueprint(creator.bp)
    app.register_blueprint(admin.bp)

    # Google OAuth blueprint (optional)
    try:
        from flask_dance.contrib.google import make_google_blueprint
        client_id, client_secret = load_google_credentials()
        if client_id and client_secret:
            # Create Google OAuth blueprint with correct scope including openid
            google_bp = make_google_blueprint(
                client_id=client_id,
                client_secret=client_secret,
                scope=["openid", "https://www.googleapis.com/auth/userinfo.profile", 
                      "https://www.googleapis.com/auth/userinfo.email"],
                redirect_to='auth.handle_google_user'
            )
            app.register_blueprint(google_bp)
            print("Google OAuth configured successfully")
            if app.config['OAUTH_INSECURE_TRANSPORT']:
                print("Warning: OAuth running in insecure mode (HTTP) - not recommended for production!")
        else:
            print("Warning: Google OAuth not configured - missing credentials")
    except ImportError:
        print("Warning: Flask-Dance not installed. Google OAuth will not be available.")
        print("To enable OAuth, install: pip install Flask-Dance==7.0.0 oauthlib==3.2.2")

    # Create upload directory if it doesn't exist
    os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

    return app

from app import models