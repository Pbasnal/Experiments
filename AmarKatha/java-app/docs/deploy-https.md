# Deploy with HTTPS (OAuth + shareable links)

Localhost cannot be shared, and Google OAuth on a public host must use **HTTPS**. This guide wires Nginx TLS in front of the existing Compose stack.

## Prerequisites

- A DNS name pointing at your VM (or a temporary public hostname)
- Docker Compose on the host
- Google OAuth client (see [`oauth-google.md`](oauth-google.md))

## 1. TLS certificates

Place certs where Nginx expects them:

```text
docker/nginx/certs/fullchain.pem
docker/nginx/certs/privkey.pem
```

**Let’s Encrypt (recommended on a real domain):** use Certbot on the host (or a one-shot container) and copy/symlink the live files into `docker/nginx/certs/`. The HTTP server block already serves `/.well-known/acme-challenge/` from `docker/nginx/certbot-www/`.

**Self-signed (smoke test only):**

```bash
cd docker/nginx/certs
openssl req -x509 -nodes -days 30 -newkey rsa:2048 \
  -keyout privkey.pem -out fullchain.pem \
  -subj "/CN=localhost"
```

## 2. Environment

In `.env`:

```bash
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
ADMIN_BOOTSTRAP_EMAILS=you@example.com
AMARKATHA_PUBLIC_BASE_URL=https://YOUR_DOMAIN
```

In Google Cloud Console, add:

- Origin: `https://YOUR_DOMAIN`
- Redirect: `https://YOUR_DOMAIN/login/oauth2/code/google`

## 3. Start with the HTTPS overlay

```bash
docker compose -f docker-compose.yml -f docker-compose.https.yml up --build -d
```

What this does:

| Piece | Role |
|-------|------|
| Nginx `:80` / `:443` | TLS termination, HTTP→HTTPS, forwards `X-Forwarded-*` |
| App | `SPRING_PROFILES_ACTIVE=prod` — Secure session cookies + `AMARKATHA_PUBLIC_BASE_URL` for OG |
| Share copy UI | Uses `window.location.origin` → becomes `https://YOUR_DOMAIN/...` when you use the public site |

## 4. Verify

1. `https://YOUR_DOMAIN/actuator/health`
2. Sign in via Google at `/creator/login`
3. Copy a series share link — must start with `https://YOUR_DOMAIN` and include `?ref=share`
4. Facebook Sharing Debugger / WhatsApp preview on `/read/s/{slug}` — `og:url` and `og:image` must be `https://…`

## How URLs are built

| Source | Behavior |
|--------|----------|
| Creator / reader “copy share” | Browser origin + path (`?ref=share`) |
| Open Graph (`og:url`, `og:image`) | `AMARKATHA_PUBLIC_BASE_URL` if set, else request / `X-Forwarded-*` |
| OAuth callback | Spring Security + forwarded HTTPS scheme |

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Nginx won’t start | Missing `fullchain.pem` / `privkey.pem` |
| Compose refuses to start | `AMARKATHA_PUBLIC_BASE_URL` is required by the HTTPS overlay |
| `redirect_uri_mismatch` | Register the HTTPS redirect in Google Console |
| OG still `http://` or `:8080` | Set `AMARKATHA_PUBLIC_BASE_URL`; confirm Nginx sets `X-Forwarded-Proto https` |
