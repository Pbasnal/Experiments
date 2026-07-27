# Google OAuth setup (Java / Spring Boot)

AmarKatha uses Spring Security OAuth2 Login with Google.

## 1. Create OAuth credentials

1. Open [Google Cloud Console → Credentials](https://console.cloud.google.com/apis/credentials).
2. Create **OAuth 2.0 Client ID** → Application type **Web application**.
3. Under **Authorized JavaScript origins**, add every origin you use:

   | Environment | Origin |
   |-------------|--------|
   | Local | `http://localhost:8080` |
   | Production | `https://YOUR_DOMAIN` |

4. Under **Authorized redirect URIs**, add **exactly**:

   | Environment | Redirect URI |
   |-------------|--------------|
   | Local | `http://localhost:8080/login/oauth2/code/google` |
   | Production | `https://YOUR_DOMAIN/login/oauth2/code/google` |

   Do **not** use the legacy Flask path `/google/authorized`.

## 2. Configure the app

Copy `env.example` → `.env` and set:

```bash
GOOGLE_CLIENT_ID=your-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-secret
ADMIN_BOOTSTRAP_EMAILS=you@example.com

# Production / HTTPS compose only — no trailing slash
AMARKATHA_PUBLIC_BASE_URL=https://YOUR_DOMAIN
```

Restart the app (or Compose stack) after changing env vars.

## 3. Test locally

1. Open http://localhost:8080/creator/login
2. Continue with Google
3. Expect redirect to `/creator` or `/admin` (bootstrap email)

## 4. Production checklist

1. Deploy behind HTTPS (see [`deploy-https.md`](deploy-https.md))
2. Register the **https** origin + redirect URI in Google Console
3. Set `SPRING_PROFILES_ACTIVE=prod` and `AMARKATHA_PUBLIC_BASE_URL=https://YOUR_DOMAIN`
4. Confirm session cookie is Secure (prod profile) and OAuth completes without `redirect_uri_mismatch`

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `redirect_uri_mismatch` | Google Console URI must match scheme + host + `/login/oauth2/code/google` exactly |
| OAuth works on localhost, fails on domain | Add the HTTPS origin and redirect; keep both if you still develop locally |
| Redirect lands on `http://…:8080` | Nginx must send `X-Forwarded-Proto` / `X-Forwarded-Host`; Spring uses `server.forward-headers-strategy=framework` |
| Share / OG still show localhost | Open the app on the public HTTPS host, or set `AMARKATHA_PUBLIC_BASE_URL` |
