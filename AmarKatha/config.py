import os
from dotenv import load_dotenv

basedir = os.path.abspath(os.path.dirname(__file__))
load_dotenv(os.path.join(basedir, '.env'))

class Config:
    # Basic Flask config
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'you-will-never-guess'
    SQLALCHEMY_DATABASE_URI = os.environ.get('DATABASE_URL') or \
        'sqlite:///' + os.path.join(basedir, 'app.db')
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    UPLOAD_FOLDER = os.path.join(basedir, 'app/static/uploads')
    MAX_CONTENT_LENGTH = 16 * 1024 * 1024  # 16MB max file size

    FLASK_ENV = os.environ.get('FLASK_ENV', 'development')

    # OAuth — HTTP allowed in dev when OAUTH_INSECURE_TRANSPORT=true
    OAUTH_INSECURE_TRANSPORT = os.environ.get(
        'OAUTH_INSECURE_TRANSPORT',
        'true' if os.environ.get('FLASK_ENV', 'development') == 'development' else 'false',
    ).lower() == 'true'

    # Google OAuth (env vars override JSON file)
    GOOGLE_OAUTH_CLIENT_ID = os.environ.get('GOOGLE_OAUTH_CLIENT_ID')
    GOOGLE_OAUTH_CLIENT_SECRET = os.environ.get('GOOGLE_OAUTH_CLIENT_SECRET')
    GOOGLE_OAUTH_CREDENTIALS_FILE = os.environ.get(
        'GOOGLE_OAUTH_CREDENTIALS_FILE', 'google_credentials.json'
    )

    # Public base URL for OAuth redirect documentation (no trailing slash)
    APP_BASE_URL = os.environ.get('APP_BASE_URL', 'http://localhost:5000')

class DevelopmentConfig(Config):
    DEBUG = True
    OAUTH_INSECURE_TRANSPORT = True

class ProductionConfig(Config):
    DEBUG = False
    OAUTH_INSECURE_TRANSPORT = False

def get_config():
    env = os.environ.get('FLASK_ENV', 'development')
    if env == 'production':
        return ProductionConfig()
    return DevelopmentConfig()
