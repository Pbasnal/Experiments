"""Google OAuth credential loading and Flask-Dance blueprint factory."""

from __future__ import annotations

import json
import os
from typing import Optional, Tuple

from config import basedir

CREDENTIAL_FILENAMES = (
    'google_credentials.json',
    'credentials.json',
    'client_secret.json',
)


def load_google_credentials(
    credentials_file: Optional[str] = None,
    client_id: Optional[str] = None,
    client_secret: Optional[str] = None,
) -> Tuple[Optional[str], Optional[str]]:
    """
    Resolve Google OAuth client id/secret from explicit args, env vars, or JSON file.
    Search order: arguments -> GOOGLE_OAUTH_* env -> JSON files in project root.
    """
    if client_id and client_secret:
        return client_id, client_secret

    env_id = os.environ.get('GOOGLE_OAUTH_CLIENT_ID')
    env_secret = os.environ.get('GOOGLE_OAUTH_CLIENT_SECRET')
    if env_id and env_secret:
        return env_id, env_secret

    filenames = [credentials_file] if credentials_file else list(CREDENTIAL_FILENAMES)
    for filename in filenames:
        if not filename:
            continue
        path = filename if os.path.isabs(filename) else os.path.join(basedir, filename)
        if not os.path.isfile(path):
            continue
        try:
            with open(path, encoding='utf-8') as handle:
                creds = json.load(handle)
            if 'web' in creds:
                web = creds['web']
                cid, secret = web.get('client_id'), web.get('client_secret')
            else:
                cid, secret = creds.get('client_id'), creds.get('client_secret')
            if cid and secret:
                return cid, secret
        except (json.JSONDecodeError, OSError) as exc:
            print(f"Warning: Could not read Google credentials from {path}: {exc}")

    return None, None


def google_oauth_redirect_uri(base_url: str) -> str:
    """Redirect URI to register in Google Cloud Console (Flask-Dance default path)."""
    return f"{base_url.rstrip('/')}/google/authorized"


def create_google_blueprint(client_id: str, client_secret: str):
    """Register Flask-Dance Google consumer blueprint."""
    from flask_dance.contrib.google import make_google_blueprint

    return make_google_blueprint(
        client_id=client_id,
        client_secret=client_secret,
        scope=[
            'openid',
            'https://www.googleapis.com/auth/userinfo.email',
            'https://www.googleapis.com/auth/userinfo.profile',
        ],
        redirect_to='auth.handle_google_user',
    )
