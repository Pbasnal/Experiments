# AmarKatha - Indian Comic Platform

A Flask-based web application for Indian comic creators and readers, built to test if Indian comic creators and readers want a platform that prioritizes storytelling quality, creator-first tools, and consistent discovery.

## 🎯 MVP Goal

"To test if Indian comic creators and readers want a platform that prioritizes storytelling quality, creator-first tools, and consistent discovery."

## ✅ Core MVP Features Implemented

### 1. Reader App – Discovery & Reading
- **🖼️ Home feed** (Trending + New + Editor Picks) - Let readers discover good content quickly
- **📚 Comic viewer** (vertical scroll) - Mobile-first reading experience
- **🔍 Search + Genre Filters** - Explore by themes like "Shonen", "Romance", "Drama"
- **➕ Follow/Subscribe to comics** - Builds user–series relationship

### 2. Creator Dashboard – Upload & Schedule
- **🖋️ Upload chapter** (image upload) - Basic creator publishing capability
- **📆 Set chapter schedule** (e.g., weekly) - Validates habit-forming behavior
- **👤 Simple creator profile page** - Creates brand space for each artist

### 3. Community & Feedback
- **💬 Comment system** (per chapter) - Enables reader–creator interaction
- **⭐️ Simple rating** (1–5 stars) - Collect quality signals for ranking

### 4. Discovery Boost Controls
- **🎖️ "Editor's Picks" slot** - Manual control to promote high-quality titles
- **📈 Trending algorithm** (based on read + dwell time) - Gives momentum to good content

### 5. Basic Admin Tools
- **⚠️ Content moderation** (basic report & remove) - Keeps platform clean
- **🧠 Data dashboard** (simple metrics) - Tracks reads, subscriptions, ratings, drop-off

## 🛠️ Technology Stack

- **Backend**: Flask 3.0.2
- **Database**: PostgreSQL 15 (with SQLite fallback)
- **Authentication**: Flask-Login
- **File Uploads**: Werkzeug with Pillow for image processing
- **Frontend**: HTML/CSS/JavaScript (templates included)
- **Containerization**: Docker & Docker Compose
- **Caching**: Redis (optional)

## 🐳 Docker Setup (Recommended)

### Prerequisites
- Docker Desktop for macOS
- Git

### Quick Start with Docker

1. **Clone and setup**
   ```bash
   git clone <repository-url>
   cd AmarKatha
   ```

2. **Run the setup script**
   ```bash
   ./setup.sh
   ```

3. **Access the application**
   - HTTP (redirects to HTTPS): http://localhost:5000
   - HTTPS: https://localhost:5002
   - Creator dashboard: https://localhost:5002/creator/dashboard
   - Admin panel: https://localhost:5002/admin/dashboard

**Note**: You'll see a browser warning about the self-signed certificate. This is normal for development. Click "Advanced" and "Proceed to localhost".

### 🔐 HTTPS Setup (Recommended for OAuth)

For Google OAuth and other features that require HTTPS:

1. **Generate SSL certificates**
   ```bash
   ./generate_ssl_cert.sh
   ```

2. **Start with HTTPS**
   ```bash
   ./scripts/start.sh start-https
   ```

3. **Access the application**
   - HTTP (redirects to HTTPS): http://localhost:5000
   - HTTPS: https://localhost:5002
   - Creator dashboard: https://localhost:5002/creator/dashboard
   - Admin panel: https://localhost:5002/admin/dashboard

4. **Update Google OAuth settings**
   - Add `https://localhost:5002` to Authorized JavaScript origins
   - Add `https://localhost:5002/google/authorized` to Authorized redirect URIs

**Note**: You'll see a browser warning about the self-signed certificate. This is normal for development. Click "Advanced" and "Proceed to localhost".

### Docker Scripts

#### Main Setup Script
```bash
# Full setup with admin user creation
./setup.sh

# Setup without admin user creation
./setup.sh --skip-admin

# Production setup
./setup.sh --prod

# Show help
./setup.sh --help
```

#### Quick Management Script
```bash
# Start the application (HTTPS by default)
./scripts/start.sh start

# Start with HTTP only
./scripts/start.sh start-http

# Start with HTTPS explicitly
./scripts/start.sh start-https

# Stop the application
./scripts/start.sh stop

# Restart the application
./scripts/start.sh restart

# Restart with HTTPS
./scripts/start.sh restart-https

# View logs
./scripts/start.sh logs

# View HTTPS logs
./scripts/start.sh logs-https

# Check status
./scripts/start.sh status

# Check HTTPS status
./scripts/start.sh status-https

# Rebuild containers
./scripts/start.sh build

# Rebuild with HTTPS
./scripts/start.sh build-https

# Check SSL certificates
./scripts/start.sh ssl-check

# Clean everything
./scripts/start.sh clean
```

#### Development Script
```bash
# Open Flask shell
./scripts/dev.sh shell

# Open database shell
./scripts/dev.sh db-shell

# Initialize database
./scripts/dev.sh init-db

# Create admin user
./scripts/dev.sh create-admin

# View web logs
./scripts/dev.sh logs-web

# View database logs
./scripts/dev.sh logs-db

# Create database backup
./scripts/dev.sh backup

# Restore database
./scripts/dev.sh restore backup_file.sql

# Show all commands
./scripts/dev.sh help
```

### Manual Docker Commands

If you prefer to run Docker commands manually:

```bash
# Start all services
docker-compose up -d

# Build and start
docker-compose up --build -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Access database
docker-compose exec postgres psql -U amarkatha_user -d amarkatha

# Run Flask commands
docker-compose exec web flask init-db
docker-compose exec web flask create-admin
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
├── scripts/
│   ├── start.sh             # Quick start/stop script
│   └── dev.sh               # Development tasks script
├── docker-compose.yml       # Main Docker Compose config
├── docker-compose.override.yml  # Development overrides
├── docker-compose.prod.yml  # Production overrides
├── Dockerfile               # Flask app container
├── setup.sh                 # Main setup script
├── requirements.txt         # Python dependencies
├── run.py                   # Application entry point
└── README.md               # This file
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

## 🎨 Key Routes

### Reader Routes (`/`)
- `GET /` - Home feed with trending, new, and editor picks
- `GET /search` - Search comics with genre filters
- `GET /comic/<id>` - Comic detail page
- `GET /comic/<id>/chapter/<id>` - Chapter viewer
- `POST /comic/<id>/follow` - Follow/unfollow comic
- `POST /comic/<id>/rate` - Rate comic (1-5 stars)
- `POST /chapter/<id>/rate` - Rate chapter (1-5 stars)
- `POST /chapter/<id>/comment` - Add comment to chapter

### Creator Routes (`/creator`)
- `GET /creator/dashboard` - Creator overview and stats
- `GET /creator/comic/new` - Create new comic
- `GET /creator/comic/<id>/edit` - Edit comic details
- `GET /creator/comic/<id>/chapter/new` - Upload new chapter
- `GET /creator/comic/<id>/chapter/<id>/edit` - Edit chapter and upload pages
- `GET /creator/profile` - Edit creator profile
- `GET /creator/schedule` - Manage chapter schedule

### Admin Routes (`/admin`)
- `GET /admin/dashboard` - Admin overview and metrics
- `GET /admin/comics` - Manage all comics
- `GET /admin/comments` - Moderate comments
- `GET /admin/users` - Manage users
- `GET /admin/analytics` - Detailed analytics dashboard

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
```bash
# Using Docker
./setup.sh --prod

# Traditional
1. Set `FLASK_ENV=production`
2. Use a production WSGI server (Gunicorn, uWSGI)
3. Configure a production database (PostgreSQL recommended)
4. Set up proper file storage (AWS S3, Google Cloud Storage)
```

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