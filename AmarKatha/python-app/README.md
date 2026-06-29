# AmarKatha - Indian Comic Platform

A Flask-based web application for Indian comic creators and readers, built to test if Indian comic creators and readers want a platform that prioritizes storytelling quality, creator-first tools, and consistent discovery.

## Project status

**This README is for setup and orientation.** For an accurate feature-by-feature status (what works, what is broken, what is planned), see:

- **[docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md)** — source of truth (updated May 2026)
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** — structure and stack
- **[docs/README.md](docs/README.md)** — index of all documentation

**Short version:** early prototype (~40% of MVP). Creator dashboard and data models are largely in place; the **reader path** (chapter viewing, search UI, follow/rate/comment) and several **templates** are still missing. Do not assume features listed below are fully working until confirmed in the status doc.

## MVP goal

"To test if Indian comic creators and readers want a platform that prioritizes storytelling quality, creator-first tools, and consistent discovery."

See [docs/product/mvp-goal.md](docs/product/mvp-goal.md) for **V0 validation launch** scope (lean; share-link first). Publishing: [publishing-tool-hl-prd.md](docs/product/publishing-tool-hl-prd.md). Market context: [india-market-analysis-feedback.md](docs/product/india-market-analysis-feedback.md).

## Implementation snapshot (high level)

| Area | Status |
|------|--------|
| Auth (register, login, become creator, optional Google OAuth) | Mostly working |
| Creator dashboard, series, comic CRUD | Mostly working |
| Chapter upload routes | Routes exist; some templates missing |
| Reader home / detail / search / chapter reader | Incomplete or broken |
| Follow, rate, comment, view analytics | Models only; routes not wired |
| Admin moderation UI | Routes exist; most templates missing |

Details: [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md).

## 🛠️ Technology Stack

- **Backend**: Flask 3.0.2
- **Database**: PostgreSQL 15 (with SQLite fallback)
- **Authentication**: Flask-Login
- **File Uploads**: Werkzeug with Pillow for image processing
- **Frontend**: HTML/CSS/JavaScript (templates included)
- **Containerization**: Docker & Docker Compose
- **Caching**: Redis (optional)

## Docker (recommended for local testing)

One file: **`docker-compose.yml`** — PostgreSQL, Flask app, Redis.

Full guide: **[docs/docker.md](docs/docker.md)**

### Quick start

```bash
git clone <repository-url>
cd AmarKatha
./setup.sh
```

Open **http://localhost:5000** (creator dashboard: `/creator/dashboard`).

### Common commands

```bash
./scripts/start.sh start      # docker compose up -d
./scripts/start.sh stop
./scripts/start.sh logs
./scripts/dev.sh help         # db shell, migrations, backup, etc.
```

Or directly:

```bash
docker compose up --build -d
docker compose exec web flask init-db
docker compose down
```

### Without the setup script

```bash
cp env.example .env
docker compose up --build -d
docker compose exec web flask init-db
```

## 📁 Project Structure

```
AmarKatha/
├── app/
│   ├── __init__.py          # Flask app factory
│   ├── models.py            # Database models
│   ├── routes/
│   │   ├── main.py          # Reader routes (discovery, reading)
│   │   ├── auth.py          # Authentication routes
│   │   ├── creator.py       # Creator dashboard routes
│   │   └── admin.py         # Admin moderation routes
│   ├── static/
│   │   └── uploads/         # Uploaded images
│   └── templates/           # HTML templates
├── docs/                    # All project documentation
├── scripts/
│   ├── start.sh             # Docker start/stop
│   └── dev.sh               # Dev tasks (shell, DB, migrate)
├── docker-compose.yml       # Local dev stack (only compose file)
├── Dockerfile
├── setup.sh                 # First-time Docker setup
├── requirements.txt
├── run.py
└── README.md
```

## 🚀 Traditional Setup (Without Docker)

### Prerequisites
- Python 3.8+
- pip
- PostgreSQL (optional, SQLite is default)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd AmarKatha
   ```

2. **Create virtual environment**
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

4. **Set up environment variables**
   ```bash
   # Create .env file
   echo "SECRET_KEY=your-secret-key-here" > .env
   echo "DATABASE_URL=sqlite:///app.db" >> .env
   ```

5. **Initialize database**
   ```bash
   flask init-db
   ```

6. **Create admin user**
   ```bash
   flask create-admin
   ```

7. **Run the application**
   ```bash
   python run.py
   ```

8. **Access the application**
   - Reader app: http://localhost:5000
   - Creator dashboard: http://localhost:5000/creator/dashboard
   - Admin panel: http://localhost:5000/admin/dashboard

### 🔐 HTTPS Setup (Traditional)

For Google OAuth and other features that require HTTPS:

1. **Generate SSL certificates**
   ```bash
   ./generate_ssl_cert.sh
   ```

2. **Run with HTTPS**
   ```bash
   python run.py --https
   ```

3. **Access the application**
   - HTTPS: https://localhost:5000
   - Creator dashboard: https://localhost:5000/creator/dashboard
   - Admin panel: https://localhost:5000/admin/dashboard

4. **Update Google OAuth settings**
   - Add `https://localhost:5000` to Authorized JavaScript origins
   - Add `https://localhost:5000/google/authorized` to Authorized redirect URIs

**Note**: You'll see a browser warning about the self-signed certificate. This is normal for development. Click "Advanced" and "Proceed to localhost".

## 📊 Database Models

### Core Models
- **User**: Readers and creators with authentication
- **Comic**: Comic series with metadata and stats
- **Chapter**: Individual chapters with pages
- **ChapterPage**: Individual pages within chapters
- **Comment**: User comments on chapters
- **Rating**: User ratings for comics and chapters
- **Follow**: User-to-user and user-to-comic follows
- **ViewLog**: Analytics tracking for views and dwell time

### Key Features
- **Trending Algorithm**: Based on views and dwell time in last 7 days
- **Rating System**: 1-5 star ratings for comics and chapters
- **Follow System**: Users can follow creators and comics
- **Scheduling**: Chapters can be scheduled for future publication
- **Editor Picks**: Manual curation system for quality content

## Key routes

Routes marked *planned* are documented targets but not implemented in `app/routes/` yet. See [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md).

### Reader routes (`/`)
- `GET /` - Home (trending / new / editor picks logic present; template wiring incomplete)
- `GET /search` - Search (route present; template missing)
- `GET /comic/<id>` - Comic detail (route present; template missing)
- `GET /comic/<id>/chapter/<id>` - Chapter viewer *(planned)*
- `POST /comic/<id>/follow` - Follow/unfollow *(planned)*
- `POST /comic/<id>/rate` - Rate comic *(planned)*
- `POST /chapter/<id>/rate` - Rate chapter *(planned)*
- `POST /chapter/<id>/comment` - Comment *(planned)*

### Creator routes (`/creator`) — largely implemented
- `GET /creator/dashboard` - Overview and stats
- `GET /creator/comic/new` - Create comic
- `GET /creator/comic/<id>/edit` - Edit comic
- `GET /creator/comic/<id>/chapter/new` - New chapter (template missing)
- `GET /creator/comic/<id>/chapter/<id>/edit` - Edit chapter / upload pages (template missing)
- `GET /creator/profile` - Profile (template missing)
- `GET /creator/schedule` - Schedule (template missing)
- Series routes: `/creator/series/new`, `/creator/series/<id>`, etc.

### Admin routes (`/admin`) — backend present; most templates missing
- `GET /admin/dashboard`, `/admin/comics`, `/admin/comments`, `/admin/analytics`
- `GET /admin/users` - Works (template exists)
- Access: currently any `is_artist` user (no separate admin flag)

## 🧪 Validation Metrics

The MVP is designed to validate these key questions:

| Question | Validated By |
|----------|-------------|
| Do creators want a quality-first Indian platform? | Chapter uploads + follow system + artist profiles |
| Do readers want to explore Indian storytelling? | Feed + search + genres + read/view stats |
| What makes a comic "sticky"? | Engagement time + subscriptions + chapter ratings |
| Will people return for scheduled updates? | Track repeat visits on scheduled chapter days |
| Can your curation improve trust in the platform? | Editor Picks performance (reads/subs/comments on promoted titles) |

## 🔧 Configuration

### Environment Variables
- `SECRET_KEY`: Flask secret key for sessions
- `DATABASE_URL`: Database connection string
- `UPLOAD_FOLDER`: Path for uploaded files
- `MAX_CONTENT_LENGTH`: Maximum file upload size (default: 16MB)
- `FLASK_ENV`: Environment (development/production)
- `POSTGRES_*`: PostgreSQL configuration for Docker

### File Uploads
- Supported formats: PNG, JPG, JPEG, GIF, PDF
- Files are stored in `app/static/uploads/`
- Unique filenames generated using UUID

## 🚀 Deployment

### Development
```bash
# Using Docker (recommended)
./setup.sh

# Traditional
python run.py
```

### Production

Use a managed PostgreSQL instance, Gunicorn (or similar) behind TLS, and object storage for uploads. The included `docker-compose.yml` is for **local development only** — see [docs/docker.md](docs/docker.md).

## 📈 Analytics & Insights

The platform tracks:
- **View Analytics**: Page views, dwell time, user engagement
- **Content Performance**: Ratings, comments, follows
- **User Behavior**: Search patterns, genre preferences
- **Creator Metrics**: Upload frequency, audience growth
- **Trending Data**: Real-time popularity metrics

## 🔮 Future Enhancements

### Deferred Features (Nice-to-Have)
- Peer review system
- On-demand print
- Paid subscriptions / monetization
- Multiple UI themes per category
- Community feed for doodles

### Potential Additions
- Mobile app
- Advanced analytics
- Creator monetization tools
- Social features
- Translation support

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

---

**AmarKatha** - Where Indian Stories Come Alive 📚✨ 