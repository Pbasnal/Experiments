# Creator Dashboard Implementation Plan

## 🚀 Phase 1: Core Dashboard (MVP)

### 1.1 Database Schema Updates

#### Add Series Model
```python
class Series(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False)
    description = db.Column(db.Text)
    cover_image = db.Column(db.String(200))
    genre = db.Column(db.String(50))
    status = db.Column(db.String(20), default='ongoing')
    author_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    comics = db.relationship('Comic', backref='series', lazy='dynamic')
    author = db.relationship('User', backref='series')
```

#### Update Comic Model
```python
# Add to existing Comic model
series_id = db.Column(db.Integer, db.ForeignKey('series.id'), nullable=True)
content_type = db.Column(db.String(20), default='comic')  # comic, story, mixed
tags = db.Column(db.Text)  # JSON string of tags
```

### 1.2 Enhanced Creator Routes

#### Update Dashboard Route
```python
@bp.route('/dashboard')
@login_required
def dashboard():
    """Enhanced creator dashboard with overview stats"""
    if not current_user.is_artist:
        flash('You need to be an artist to access the creator dashboard.', 'error')
        return redirect(url_for('main.index'))
    
    comics = current_user.comics.order_by(Comic.updated_at.desc()).all()
    series = current_user.series.order_by(Series.updated_at.desc()).all()
    recent_chapters = Chapter.query.join(Comic).filter(
        Comic.author_id == current_user.id
    ).order_by(Chapter.created_at.desc()).limit(5).all()
    
    stats = calculate_creator_stats(current_user.id)
    
    return render_template('creator/dashboard.html',
                         comics=comics,
                         series=series,
                         recent_chapters=recent_chapters,
                         stats=stats)
```

#### Add Series Management Routes
```python
@bp.route('/series/new', methods=['GET', 'POST'])
@login_required
def new_series():
    """Create a new series"""
    if request.method == 'POST':
        name = request.form.get('name', '').strip()
        description = request.form.get('description', '').strip()
        genre = request.form.get('genre', '').strip()
        
        if not name:
            flash('Series name is required.', 'error')
            return render_template('creator/new_series.html')
        
        cover_image = None
        if 'cover_image' in request.files:
            file = request.files['cover_image']
            if file and file.filename and allowed_file(file.filename):
                filename = secure_filename(f"{uuid.uuid4()}_{file.filename}")
                file.save(os.path.join(current_app.config['UPLOAD_FOLDER'], filename))
                cover_image = f"uploads/{filename}"
        
        series = Series(
            name=name,
            description=description,
            genre=genre,
            cover_image=cover_image,
            author_id=current_user.id
        )
        
        db.session.add(series)
        db.session.commit()
        
        flash('Series created successfully!', 'success')
        return redirect(url_for('creator.series_detail', series_id=series.id))
    
    return render_template('creator/new_series.html')
```

### 1.3 Enhanced Templates

#### Main Dashboard Template
```html
{% extends "base.html" %}
{% block content %}
<div class="creator-dashboard">
    <!-- Quick Stats Bar -->
    <div class="stats-bar">
        <div class="stat-card">
            <h3>{{ stats.total_views }}</h3>
            <p>Total Views</p>
        </div>
        <div class="stat-card">
            <h3>{{ stats.total_followers }}</h3>
            <p>Followers</p>
        </div>
        <div class="stat-card">
            <h3>{{ stats.avg_rating }}</h3>
            <p>Avg Rating</p>
        </div>
        <div class="stat-card">
            <h3>{{ stats.total_content }}</h3>
            <p>Content Pieces</p>
        </div>
    </div>

    <!-- Quick Actions -->
    <div class="quick-actions">
        <a href="{{ url_for('creator.new_comic') }}" class="btn btn-primary">New Comic</a>
        <a href="{{ url_for('creator.new_series') }}" class="btn btn-secondary">New Series</a>
        <a href="{{ url_for('creator.upload') }}" class="btn btn-success">Upload Content</a>
    </div>

    <!-- Content Overview -->
    <div class="content-overview">
        <h2>Your Content</h2>
        <div class="content-grid">
            {% for comic in comics %}
            <div class="content-card">
                <img src="{{ comic.cover_image or '/static/default-cover.jpg' }}" alt="{{ comic.title }}">
                <h3>{{ comic.title }}</h3>
                <p>{{ comic.genre }}</p>
                <div class="content-stats">
                    <span>{{ comic.total_views }} views</span>
                    <span>{{ comic.average_rating() }}★</span>
                </div>
                <a href="{{ url_for('creator.comic_analytics', comic_id=comic.id) }}" class="btn btn-sm">View Analytics</a>
            </div>
            {% endfor %}
        </div>
    </div>

    <!-- Series Overview -->
    <div class="series-overview">
        <h2>Your Series</h2>
        <div class="series-grid">
            {% for series in series %}
            <div class="series-card">
                <img src="{{ series.cover_image or '/static/default-series.jpg' }}" alt="{{ series.name }}">
                <h3>{{ series.name }}</h3>
                <p>{{ series.description[:100] }}...</p>
                <div class="series-stats">
                    <span>{{ series.comics.count() }} comics</span>
                    <span>{{ series.status }}</span>
                </div>
                <a href="{{ url_for('creator.series_detail', series_id=series.id) }}" class="btn btn-sm">View Series</a>
            </div>
            {% endfor %}
        </div>
    </div>
</div>
{% endblock %}
```

## 🚀 Phase 2: Enhanced Upload System

### 2.1 Multi-Format Upload Support

#### Enhanced Upload Route
```python
@bp.route('/upload', methods=['GET', 'POST'])
@login_required
def upload_content():
    """Unified upload interface for all content types"""
    if request.method == 'POST':
        content_type = request.form.get('content_type', 'comic')
        title = request.form.get('title', '').strip()
        description = request.form.get('description', '').strip()
        series_id = request.form.get('series_id', '').strip()
        tags = request.form.get('tags', '').strip()
        
        if not title:
            flash('Title is required.', 'error')
            return render_template('creator/upload.html')
        
        if content_type == 'comic':
            return handle_comic_upload(request, title, description, series_id, tags)
        elif content_type == 'story':
            return handle_story_upload(request, title, description, series_id, tags)
    
    series = current_user.series.all()
    return render_template('creator/upload.html', series=series)

def handle_comic_upload(request, title, description, series_id, tags):
    """Handle comic upload with multiple images"""
    files = request.files.getlist('pages')
    
    if not files or not files[0].filename:
        flash('At least one image is required for comics.', 'error')
        return render_template('creator/upload.html')
    
    comic = Comic(
        title=title,
        description=description,
        series_id=series_id if series_id else None,
        tags=tags,
        content_type='comic',
        author_id=current_user.id
    )
    
    db.session.add(comic)
    db.session.commit()
    
    chapter = Chapter(
        title=title,
        chapter_number=1.0,
        comic_id=comic.id,
        is_published=True,
        published_at=datetime.utcnow()
    )
    
    db.session.add(chapter)
    db.session.commit()
    
    for i, file in enumerate(files):
        if file and file.filename and allowed_file(file.filename):
            filename = secure_filename(f"{uuid.uuid4()}_{file.filename}")
            file.save(os.path.join(current_app.config['UPLOAD_FOLDER'], filename))
            
            page = ChapterPage(
                page_number=i + 1,
                image_path=f"uploads/{filename}",
                chapter_id=chapter.id
            )
            db.session.add(page)
    
    db.session.commit()
    flash('Comic uploaded successfully!', 'success')
    return redirect(url_for('creator.comic_analytics', comic_id=comic.id))
```

### 2.2 Enhanced Upload Template
```html
{% extends "base.html" %}
{% block content %}
<div class="upload-container">
    <h1>Upload Content</h1>
    
    <form method="POST" enctype="multipart/form-data" id="upload-form">
        <div class="form-group">
            <label>Content Type</label>
            <select name="content_type" id="content-type" required>
                <option value="comic">Comic (Multiple Images)</option>
                <option value="story">Story (Text/PDF)</option>
                <option value="mixed">Mixed Content</option>
            </select>
        </div>

        <div class="form-group">
            <label>Title</label>
            <input type="text" name="title" required>
        </div>

        <div class="form-group">
            <label>Description</label>
            <textarea name="description" rows="3"></textarea>
        </div>

        <div class="form-group">
            <label>Series (Optional)</label>
            <select name="series_id">
                <option value="">No Series</option>
                {% for series in series %}
                <option value="{{ series.id }}">{{ series.name }}</option>
                {% endfor %}
            </select>
        </div>

        <div class="form-group">
            <label>Tags (comma-separated)</label>
            <input type="text" name="tags" placeholder="action, comedy, drama">
        </div>

        <div id="comic-upload" class="upload-area">
            <label>Comic Pages (Multiple Images)</label>
            <input type="file" name="pages" multiple accept="image/*">
            <div class="upload-preview" id="comic-preview"></div>
        </div>

        <div id="story-upload" class="upload-area" style="display: none;">
            <label>Story Content</label>
            <textarea name="story_content" rows="10" placeholder="Enter your story here..."></textarea>
            <p>OR</p>
            <input type="file" name="story_file" accept=".pdf,.txt,.doc,.docx">
        </div>

        <button type="submit" class="btn btn-primary">Upload Content</button>
    </form>
</div>

<script>
document.getElementById('content-type').addEventListener('change', function() {
    const comicUpload = document.getElementById('comic-upload');
    const storyUpload = document.getElementById('story-upload');
    
    if (this.value === 'comic') {
        comicUpload.style.display = 'block';
        storyUpload.style.display = 'none';
    } else if (this.value === 'story') {
        comicUpload.style.display = 'none';
        storyUpload.style.display = 'block';
    }
});
</script>
{% endblock %}
```

## 🚀 Phase 3: Analytics Implementation

### 3.1 Individual Content Analytics

#### Analytics Route
```python
@bp.route('/comic/<int:comic_id>/analytics')
@login_required
def comic_analytics(comic_id):
    """Detailed analytics for individual content"""
    comic = Comic.query.get_or_404(comic_id)
    
    if comic.author_id != current_user.id:
        flash('You can only view analytics for your own content.', 'error')
        return redirect(url_for('creator.dashboard'))
    
    analytics = get_comic_analytics(comic_id)
    
    return render_template('creator/comic_analytics.html',
                         comic=comic,
                         analytics=analytics)

def get_comic_analytics(comic_id):
    """Get comprehensive analytics for a comic"""
    comic = Comic.query.get(comic_id)
    
    view_logs = ViewLog.query.filter_by(comic_id=comic_id).all()
    total_views = len(view_logs)
    unique_views = len(set(log.user_id for log in view_logs if log.user_id))
    
    now = datetime.utcnow()
    week_ago = now - timedelta(days=7)
    month_ago = now - timedelta(days=30)
    
    weekly_views = ViewLog.query.filter(
        ViewLog.comic_id == comic_id,
        ViewLog.viewed_at >= week_ago
    ).count()
    
    monthly_views = ViewLog.query.filter(
        ViewLog.comic_id == comic_id,
        ViewLog.viewed_at >= month_ago
    ).count()
    
    avg_dwell_time = sum(log.dwell_time for log in view_logs) / len(view_logs) if view_logs else 0
    
    ratings = Rating.query.filter_by(comic_id=comic_id).all()
    rating_distribution = {}
    for i in range(1, 6):
        rating_distribution[i] = len([r for r in ratings if r.rating == i])
    
    return {
        'total_views': total_views,
        'unique_views': unique_views,
        'weekly_views': weekly_views,
        'monthly_views': monthly_views,
        'avg_dwell_time': round(avg_dwell_time, 1),
        'rating_distribution': rating_distribution,
        'total_ratings': len(ratings),
        'avg_rating': comic.average_rating(),
        'total_comments': comic.chapters.join(Chapter.comments).count(),
        'total_followers': comic.follows.count()
    }
```

### 3.2 Analytics Template
```html
{% extends "base.html" %}
{% block content %}
<div class="analytics-container">
    <div class="analytics-header">
        <h1>{{ comic.title }} - Analytics</h1>
        <p>{{ comic.description }}</p>
    </div>

    <div class="stats-grid">
        <div class="stat-card">
            <h3>{{ analytics.total_views }}</h3>
            <p>Total Views</p>
        </div>
        <div class="stat-card">
            <h3>{{ analytics.unique_views }}</h3>
            <p>Unique Views</p>
        </div>
        <div class="stat-card">
            <h3>{{ analytics.avg_rating }}</h3>
            <p>Average Rating</p>
        </div>
        <div class="stat-card">
            <h3>{{ analytics.total_followers }}</h3>
            <p>Followers</p>
        </div>
    </div>

    <div class="charts-section">
        <div class="chart-container">
            <h3>Views Over Time</h3>
            <canvas id="viewsChart"></canvas>
        </div>
        
        <div class="chart-container">
            <h3>Rating Distribution</h3>
            <canvas id="ratingChart"></canvas>
        </div>
    </div>

    <div class="metrics-section">
        <div class="metric-card">
            <h4>Engagement</h4>
            <p>Average Reading Time: {{ analytics.avg_dwell_time }} seconds</p>
            <p>Total Comments: {{ analytics.total_comments }}</p>
        </div>
        
        <div class="metric-card">
            <h4>Recent Activity</h4>
            <p>Views This Week: {{ analytics.weekly_views }}</p>
            <p>Views This Month: {{ analytics.monthly_views }}</p>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script>
const viewsCtx = document.getElementById('viewsChart').getContext('2d');
new Chart(viewsCtx, {
    type: 'line',
    data: {
        labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4'],
        datasets: [{
            label: 'Views',
            data: [12, 19, 3, 5],
            borderColor: 'rgb(75, 192, 192)',
            tension: 0.1
        }]
    }
});

const ratingCtx = document.getElementById('ratingChart').getContext('2d');
new Chart(ratingCtx, {
    type: 'bar',
    data: {
        labels: ['1★', '2★', '3★', '4★', '5★'],
        datasets: [{
            label: 'Ratings',
            data: [
                {{ analytics.rating_distribution[1] }},
                {{ analytics.rating_distribution[2] }},
                {{ analytics.rating_distribution[3] }},
                {{ analytics.rating_distribution[4] }},
                {{ analytics.rating_distribution[5] }}
            ],
            backgroundColor: 'rgba(54, 162, 235, 0.2)',
            borderColor: 'rgba(54, 162, 235, 1)',
            borderWidth: 1
        }]
    }
});
</script>
{% endblock %}
```

## 🎨 CSS Styling

### Dashboard Styles
```css
.creator-dashboard {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
}

.stats-bar {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 30px;
}

.stat-card {
    background: white;
    padding: 20px;
    border-radius: 10px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    text-align: center;
}

.stat-card h3 {
    font-size: 2rem;
    color: #333;
    margin: 0;
}

.quick-actions {
    display: flex;
    gap: 15px;
    margin-bottom: 30px;
}

.content-grid, .series-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 20px;
    margin-top: 20px;
}

.content-card, .series-card {
    background: white;
    border-radius: 10px;
    overflow: hidden;
    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    transition: transform 0.2s;
}

.content-card:hover, .series-card:hover {
    transform: translateY(-5px);
}

.content-card img, .series-card img {
    width: 100%;
    height: 200px;
    object-fit: cover;
}

.content-card .card-content, .series-card .card-content {
    padding: 15px;
}

.content-stats, .series-stats {
    display: flex;
    justify-content: space-between;
    margin-top: 10px;
    font-size: 0.9rem;
    color: #666;
}
```

This implementation plan provides a comprehensive roadmap for building the Creator Dashboard with all the features you requested. The plan is organized into phases that can be implemented incrementally, starting with the core functionality and building up to advanced features. 