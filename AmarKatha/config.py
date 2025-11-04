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

    # Environment-specific settings
    FLASK_ENV = os.environ.get('FLASK_ENV', 'development')
    
    # OAuth settings
    OAUTH_INSECURE_TRANSPORT = os.environ.get('OAUTH_INSECURE_TRANSPORT', 
        'true' if FLASK_ENV == 'development' else 'false').lower() == 'true'

class DevelopmentConfig(Config):
    DEBUG = True
    OAUTH_INSECURE_TRANSPORT = True

class ProductionConfig(Config):
    DEBUG = False
    OAUTH_INSECURE_TRANSPORT = False

# Factory to get the right config
def get_config():
    env = os.environ.get('FLASK_ENV', 'development')
    if env == 'production':
        return ProductionConfig()
    return DevelopmentConfig()