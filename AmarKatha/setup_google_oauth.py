#!/usr/bin/env python3
"""Validate Google OAuth setup and print the exact redirect URI for Google Cloud Console."""

import json
import os
import sys

from config import basedir
from app.oauth_google import CREDENTIAL_FILENAMES, google_oauth_redirect_uri, load_google_credentials


def check_credentials_file():
    for filename in CREDENTIAL_FILENAMES:
        path = os.path.join(basedir, filename)
        if not os.path.isfile(path):
            continue
        print(f"Found credentials file: {path}")
        try:
            with open(path, encoding='utf-8') as handle:
                creds = json.load(handle)
            if 'web' in creds:
                client_id = creds['web'].get('client_id')
                client_secret = creds['web'].get('client_secret')
            else:
                client_id = creds.get('client_id')
                client_secret = creds.get('client_secret')
            if client_id and client_secret:
                print(f"  Client ID: {client_id[:24]}...")
                return True
            print(f"  Missing client_id or client_secret in {filename}")
        except (json.JSONDecodeError, OSError) as exc:
            print(f"  Error reading file: {exc}")
    return False


def check_environment_variables():
    client_id = os.environ.get('GOOGLE_OAUTH_CLIENT_ID')
    client_secret = os.environ.get('GOOGLE_OAUTH_CLIENT_SECRET')
    if client_id and client_secret:
        print("Environment variables set:")
        print(f"  GOOGLE_OAUTH_CLIENT_ID={client_id[:24]}...")
        return True
    return False


def main():
    try:
        from dotenv import load_dotenv
        load_dotenv(os.path.join(basedir, '.env'))
    except ImportError:
        pass

    base_url = os.environ.get('APP_BASE_URL', 'http://localhost:5000')
    redirect_uri = google_oauth_redirect_uri(base_url)

    print("Google OAuth setup check")
    print("=" * 50)

    file_ok = check_credentials_file()
    print()
    env_ok = check_environment_variables()
    print()

    client_id, client_secret = load_google_credentials()
    if client_id and client_secret:
        print("Credentials load: OK (app will register /google routes)")
    else:
        print("Credentials load: FAILED")
        print()
        print("To fix:")
        print("  1. https://console.cloud.google.com/apis/credentials")
        print("  2. Create OAuth 2.0 Client ID (Web application)")
        print("  3. Download JSON -> save as google_credentials.json in project root")
        print("     OR set GOOGLE_OAUTH_CLIENT_ID and GOOGLE_OAUTH_CLIENT_SECRET in .env")
        return 1

    print()
    print("Google Cloud Console — use these EXACT values:")
    print(f"  Authorized JavaScript origins: {base_url}")
    print(f"  Authorized redirect URIs:      {redirect_uri}")
    print()
    print("OAuth flow in this app:")
    print(f"  1. User clicks Sign in with Google -> {base_url}/auth/google")
    print(f"  2. Redirect to Google -> callback {redirect_uri}")
    print(f"  3. App logs user in via auth.handle_google_user")
    print()
    print("Restart after adding credentials:")
    print("  docker compose restart web")
    return 0


if __name__ == '__main__':
    sys.exit(main())
