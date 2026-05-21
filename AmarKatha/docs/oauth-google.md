# Google OAuth setup

AmarKatha uses [Flask-Dance](https://github.com/singingwolfboy/flask-dance) for Google sign-in.

## 1. Create OAuth credentials

1. Open [Google Cloud Console → Credentials](https://console.cloud.google.com/apis/credentials).
2. Create **OAuth 2.0 Client ID** → Application type **Web application**.
3. Under **Authorized JavaScript origins**, add:
   - `http://localhost:5000`
4. Under **Authorized redirect URIs**, add **exactly**:
   - `http://localhost:5000/google/authorized`

   This path is required by Flask-Dance. Do **not** use `/auth/google/authorized`.

5. Download the JSON client file.

## 2. Add credentials to the project

**Option A — JSON file (recommended for local dev)**

```bash
cp ~/Downloads/client_secret_*.json google_credentials.json
# or copy from the example:
cp google_credentials.json.example google_credentials.json
# then edit with your real client_id and client_secret
```

The file is gitignored. With Docker, the project folder is mounted at `/app`, so `google_credentials.json` in the repo root is visible inside the container.

**Option B — environment variables**

Add to `.env`:

```bash
GOOGLE_OAUTH_CLIENT_ID=your-id.apps.googleusercontent.com
GOOGLE_OAUTH_CLIENT_SECRET=your-secret
OAUTH_INSECURE_TRANSPORT=true
APP_BASE_URL=http://localhost:5000
```

## 3. Verify and restart

```bash
python setup_google_oauth.py
docker compose restart web
```

On startup you should see:

```text
Google OAuth configured successfully
  Register this redirect URI in Google Cloud Console: http://localhost:5000/google/authorized
```

Check routes:

```bash
docker compose exec web python -c "
from app import create_app
app = create_app()
print([r.rule for r in app.url_map.iter_rules() if 'google' in r.rule])
"
```

Expected: `/google`, `/google/authorized`, `/auth/google`.

## 4. Test the flow

1. Open http://localhost:5000/login
2. Click **Sign in with Google** (button only appears when OAuth is configured)
3. Complete Google consent
4. You should return logged in (new users are prompted to become a creator)

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| No Google button on login | Credentials missing; run `python setup_google_oauth.py` |
| `redirect_uri_mismatch` | Redirect URI in Google Console must be `http://localhost:5000/google/authorized` |
| `Google OAuth is not configured` in logs | Add `google_credentials.json` or env vars; restart `web` |
| Works on host but not Docker | Ensure file is in project root and volume `.:/app` is active |
| `insecure_transport` error | Set `OAUTH_INSECURE_TRANSPORT=true` in `.env` for HTTP local dev |

## Production

- Set `OAUTH_INSECURE_TRANSPORT=false`
- Use HTTPS and `APP_BASE_URL=https://your-domain.com`
- Register `https://your-domain.com/google/authorized` as redirect URI
