from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from flask_login import LoginManager
from flask_migrate import Migrate
from config import get_config
import os

db = SQLAlchemy()
migrate = Migrate()
login_manager = LoginManager()
login_manager.login_view = 'auth.login'
login_manager.login_message = 'Please log in to access this page.'


def create_app(config_class=None):
    app = Flask(__name__)

    if config_class is None:
        config_class = get_config()

    app.config.from_object(config_class)

    if app.config['OAUTH_INSECURE_TRANSPORT']:
        os.environ['OAUTHLIB_INSECURE_TRANSPORT'] = '1'
    else:
        os.environ.pop('OAUTHLIB_INSECURE_TRANSPORT', None)

    db.init_app(app)
    migrate.init_app(app, db)
    login_manager.init_app(app)

    from app.routes import main, auth, creator, admin
    app.register_blueprint(main.bp)
    app.register_blueprint(auth.bp)
    app.register_blueprint(creator.bp)
    app.register_blueprint(admin.bp)

    _register_google_oauth(app)

    @app.context_processor
    def inject_auth_helpers():
        return {
            'google_oauth_enabled': 'google' in app.blueprints,
        }

    os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

    return app


def _register_google_oauth(app):
    """Register Google OAuth blueprint when credentials are available."""
    from app.oauth_google import (
        create_google_blueprint,
        google_oauth_redirect_uri,
        load_google_credentials,
    )

    client_id, client_secret = load_google_credentials(
        credentials_file=app.config.get('GOOGLE_OAUTH_CREDENTIALS_FILE'),
        client_id=app.config.get('GOOGLE_OAUTH_CLIENT_ID'),
        client_secret=app.config.get('GOOGLE_OAUTH_CLIENT_SECRET'),
    )

    if not client_id or not client_secret:
        print("Warning: Google OAuth not configured.")
        print("  Place google_credentials.json in the project root, or set")
        print("  GOOGLE_OAUTH_CLIENT_ID and GOOGLE_OAUTH_CLIENT_SECRET in .env")
        print(f"  Redirect URI (when configured): {google_oauth_redirect_uri(app.config['APP_BASE_URL'])}")
        return

    try:
        app.register_blueprint(create_google_blueprint(client_id, client_secret))
        redirect_uri = google_oauth_redirect_uri(app.config['APP_BASE_URL'])
        print("Google OAuth configured successfully")
        print(f"  Register this redirect URI in Google Cloud Console: {redirect_uri}")
        if app.config['OAUTH_INSECURE_TRANSPORT']:
            print("  OAuth insecure transport enabled (HTTP) — dev only")
    except ImportError:
        print("Warning: Flask-Dance not installed. pip install Flask-Dance oauthlib")


from app import models
