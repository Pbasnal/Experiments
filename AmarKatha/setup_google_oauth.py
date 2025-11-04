#!/usr/bin/env python3
"""
Google OAuth Setup Helper Script
This script helps validate your Google OAuth credentials setup.
"""

import json
import os
import sys

def check_credentials_file():
    """Check if Google credentials file exists and is valid"""
    credential_files = [
        'google_credentials.json',
        'credentials.json', 
        'client_secret.json'
    ]
    
    for filename in credential_files:
        if os.path.exists(filename):
            print(f"✅ Found credentials file: {filename}")
            try:
                with open(filename, 'r') as f:
                    creds = json.load(f)
                
                # Check file format
                if 'web' in creds:
                    client_id = creds['web'].get('client_id')
                    client_secret = creds['web'].get('client_secret')
                    print(f"✅ Standard Google OAuth format detected")
                elif 'client_id' in creds:
                    client_id = creds.get('client_id')
                    client_secret = creds.get('client_secret')
                    print(f"✅ Direct format detected")
                else:
                    print(f"❌ Unknown credentials file format")
                    continue
                
                if client_id and client_secret:
                    print(f"✅ Client ID: {client_id[:20]}...")
                    print(f"✅ Client Secret: {client_secret[:10]}...")
                    return True
                else:
                    print(f"❌ Missing client_id or client_secret in {filename}")
                    
            except json.JSONDecodeError:
                print(f"❌ Invalid JSON in {filename}")
            except Exception as e:
                print(f"❌ Error reading {filename}: {e}")
    
    print("❌ No valid credentials file found")
    return False

def check_environment_variables():
    """Check if Google OAuth environment variables are set"""
    client_id = os.environ.get('GOOGLE_OAUTH_CLIENT_ID')
    client_secret = os.environ.get('GOOGLE_OAUTH_CLIENT_SECRET')
    
    if client_id and client_secret:
        print("✅ Environment variables found:")
        print(f"   Client ID: {client_id[:20]}...")
        print(f"   Client Secret: {client_secret[:10]}...")
        return True
    else:
        print("❌ Environment variables not found")
        return False

def main():
    print("🔍 Checking Google OAuth Setup...")
    print("=" * 50)
    
    # Check credentials file
    file_ok = check_credentials_file()
    print()
    
    # Check environment variables
    env_ok = check_environment_variables()
    print()
    
    if file_ok or env_ok:
        print("✅ Google OAuth is properly configured!")
        print("\n📋 Next steps:")
        print("1. Make sure your Google Cloud Console has these Authorized JavaScript origins:")
        print("   - http://localhost:5000")
        print("2. Make sure your Google Cloud Console has these Authorized redirect URIs:")
        print("   - http://localhost:5000/auth/google/authorized")
        print("3. Restart your Docker containers:")
        print("   docker-compose down && docker-compose up --build -d")
        return 0
    else:
        print("❌ Google OAuth is not configured!")
        print("\n📋 To set up Google OAuth:")
        print("1. Go to https://console.cloud.google.com/apis/credentials")
        print("2. Create OAuth 2.0 Client ID")
        print("3. Download the JSON file")
        print("4. Rename it to 'google_credentials.json' and place it in this directory")
        print("5. Or set environment variables GOOGLE_OAUTH_CLIENT_ID and GOOGLE_OAUTH_CLIENT_SECRET")
        return 1

if __name__ == "__main__":
    sys.exit(main()) 