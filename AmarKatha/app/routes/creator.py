from flask import Blueprint, render_template, request, redirect, url_for, flash, current_app, jsonify
from flask_login import login_required, current_user
from werkzeug.utils import secure_filename
from app.models import Comic, Chapter, ChapterPage, User, Series, ViewLog, Rating, Comment
from app import db
from datetime import datetime, timedelta
import os
import uuid
from collections import Counter

bp = Blueprint('creator', __name__, url_prefix='/creator')

def allowed_file(filename):
    """Check if file extension is allowed"""
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'pdf'}
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def calculate_creator_stats(user_id):
    """Calculate comprehensive creator statistics"""
    comics = Comic.query.filter_by(author_id=user_id).all()
    
    # Basic stats
    total_views = sum(comic.total_views for comic in comics)
    total_followers = sum(comic.follows.count() for comic in comics)
    
    # Calculate average rating
    rated_comics = [c for c in comics if c.rating_count > 0]
    if rated_comics:
        total_rating = sum(comic.average_rating() for comic in rated_comics)
        avg_rating = total_rating / len(rated_comics)
    else:
        avg_rating = 0
    
    # Get recent activity (last 7 days)
    week_ago = datetime.utcnow() - timedelta(days=7)
    recent_views = ViewLog.query.join(Comic).filter(
        Comic.author_id == user_id,
        ViewLog.viewed_at >= week_ago
    ).count()
    
    # Get total comments
    total_comments = Comment.query.join(Chapter).join(Comic).filter(
        Comic.author_id == user_id
    ).count()
    
    return {
        'total_views': total_views,
        'total_followers': total_followers,
        'avg_rating': round(avg_rating, 1),
        'total_content': len(comics),
        'recent_views': recent_views,
        'total_comments': total_comments
    }

def get_recent_activity(user_id, limit=10):
    """Get recent activity for the creator"""
    activities = []
    
    # Get recent chapters
    recent_chapters = Chapter.query.join(Comic).filter(
        Comic.author_id == user_id
    ).order_by(Chapter.created_at.desc()).limit(5).all()
    
    for chapter in recent_chapters:
        activities.append({
            'type': 'chapter_created',
            'title': f'New chapter: {chapter.title}',
            'subtitle': f'Added to {chapter.comic.title}',
            'time': chapter.created_at,
            'url': url_for('creator.chapter_edit', comic_id=chapter.comic_id, chapter_id=chapter.id)
        })
    
    # Get recent comments
    recent_comments = Comment.query.join(Chapter).join(Comic).filter(
        Comic.author_id == user_id
    ).order_by(Comment.created_at.desc()).limit(5).all()
    
    for comment in recent_comments:
        activities.append({
            'type': 'comment_received',
            'title': f'New comment on {comment.chapter.title}',
            'subtitle': f'by {comment.user.username}',
            'time': comment.created_at,
            'url': url_for('main.chapter_view', comic_id=comment.chapter.comic_id, chapter_id=comment.chapter.id)
        })
    
    # Get recent ratings
    recent_ratings = Rating.query.join(Comic).filter(
        Comic.author_id == user_id
    ).order_by(Rating.created_at.desc()).limit(5).all()
    
    for rating in recent_ratings:
        activities.append({
            'type': 'rating_received',
            'title': f'New {rating.rating}★ rating',
            'subtitle': f'on {rating.comic.title}',
            'time': rating.created_at,
            'url': url_for('main.comic_view', comic_id=rating.comic_id)
        })
    
    # Sort all activities by time and return the most recent
    activities.sort(key=lambda x: x['time'], reverse=True)
    return activities[:limit]

def calculate_series_analytics(series_id):
    """Calculate series-specific analytics (views, followers, rating)."""
    series = Series.query.get(series_id)
    if not series:
        return {
            'total_views': 0,
            'total_followers': 0,
            'avg_rating': 0,
            'total_comics': 0
        }
    comics = series.comics.all()
    total_views = sum(c.total_views for c in comics)
    total_followers = sum(c.follows.count() for c in comics)
    rated_comics = [c for c in comics if c.rating_count > 0]
    if rated_comics:
        total_rating = sum(c.average_rating() for c in rated_comics)
        avg_rating = total_rating / len(rated_comics)
    else:
        avg_rating = 0
    return {
        'total_views': total_views,
        'total_followers': total_followers,
        'avg_rating': round(avg_rating, 1),
        'total_comics': len(comics)
    }

@bp.route('/dashboard')
@login_required
def dashboard():
    """Enhanced creator dashboard with overview stats and recent activity"""
    if not current_user.is_artist:
        flash('You need to be an artist to access the creator dashboard.', 'error')
        return redirect(url_for('main.index'))
    
    # Get user's content
    comics = current_user.comics.order_by(Comic.updated_at.desc()).limit(8).all()
    series = current_user.series.order_by(Series.updated_at.desc()).limit(6).all()
    
    # Calculate comprehensive stats
    stats = calculate_creator_stats(current_user.id)
    
    # Get recent activity
    recent_activities = get_recent_activity(current_user.id, limit=8)
    
    # Get content by type for quick overview
    content_by_type = {
        'comic': len([c for c in comics if c.content_type == 'comic']),
        'story': len([c for c in comics if c.content_type == 'story']),
        'mixed': len([c for c in comics if c.content_type == 'mixed']),
        'standalone': len([c for c in comics if c.content_type == 'standalone'])
    }
    
    return render_template('creator/dashboard.html',
                         comics=comics,
                         series=series,
                         stats=stats,
                         recent_activities=recent_activities,
                         content_by_type=content_by_type)

@bp.route('/comic/new', methods=['GET', 'POST'])
@login_required
def new_comic():
    """Create a new comic"""
    if not current_user.is_artist:
        flash('You need to be an artist to create comics.', 'error')
        return redirect(url_for('main.index'))
    
    if request.method == 'POST':
        title = request.form.get('title', '').strip()
        description = request.form.get('description', '').strip()
        genre = request.form.get('genre', '').strip()
        status = request.form.get('status', 'ongoing')
        schedule = request.form.get('schedule', '')
        content_type = request.form.get('content_type', 'comic')
        series_id = request.form.get('series_id', '').strip()
        
        if not title:
            flash('Title is required.', 'error')
            return render_template('creator/new_comic.html')
        
        # Handle cover image upload
        cover_image = None
        if 'cover_image' in request.files:
            file = request.files['cover_image']
            if file and file.filename and allowed_file(file.filename):
                filename = secure_filename(f"{uuid.uuid4()}_{file.filename}")
                file.save(os.path.join(current_app.config['UPLOAD_FOLDER'], filename))
                cover_image = f"uploads/{filename}"
        
        comic = Comic(
            title=title,
            description=description,
            genre=genre,
            status=status,
            schedule=schedule,
            content_type=content_type,
            cover_image=cover_image,
            author_id=current_user.id
        )
        
        # Set series if provided
        if series_id:
            try:
                series_id = int(series_id)
                series = Series.query.get(series_id)
                if series and series.author_id == current_user.id:
                    comic.series_id = series_id
            except (ValueError, TypeError):
                pass  # Invalid series_id, ignore
        
        db.session.add(comic)
        db.session.commit()
        
        flash('Comic created successfully!', 'success')
        return redirect(url_for('creator.dashboard'))
    
    return render_template('creator/new_comic.html')

@bp.route('/series/new', methods=['GET', 'POST'])
@login_required
def new_series():
    """Create a new series"""
    if not current_user.is_artist:
        flash('You need to be an artist to create series.', 'error')
        return redirect(url_for('main.index'))
    
    if request.method == 'POST':
        name = request.form.get('name', '').strip()
        description = request.form.get('description', '').strip()
        genre = request.form.get('genre', '').strip()
        status = request.form.get('status', 'ongoing')
        schedule = request.form.get('schedule', '')
        
        if not name:
            flash('Series name is required.', 'error')
            return render_template('creator/new_series.html')
        
        # Handle cover image upload
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
            status=status,
            schedule=schedule,
            cover_image=cover_image,
            author_id=current_user.id
        )
        
        db.session.add(series)
        db.session.commit()
        
        flash('Series created successfully!', 'success')
        return redirect(url_for('creator.dashboard'))
    
    return render_template('creator/new_series.html')

@bp.route('/series/<int:series_id>/edit', methods=['GET', 'POST'])
@login_required
def series_edit(series_id):
    """Edit series details"""
    series = Series.query.get_or_404(series_id)
    
    if series.author_id != current_user.id:
        flash('You can only edit your own series.', 'error')
        return redirect(url_for('creator.dashboard'))
    
    if request.method == 'POST':
        series.name = request.form.get('name', '').strip()
        series.description = request.form.get('description', '').strip()
        series.genre = request.form.get('genre', '').strip()
        series.status = request.form.get('status', 'ongoing')
        series.schedule = request.form.get('schedule', '')
        
        # Handle cover image upload
        if 'cover_image' in request.files:
            file = request.files['cover_image']
            if file and file.filename and allowed_file(file.filename):
                filename = secure_filename(f"{uuid.uuid4()}_{file.filename}")
                file.save(os.path.join(current_app.config['UPLOAD_FOLDER'], filename))
                series.cover_image = f"uploads/{filename}"
        
        db.session.commit()
        flash('Series updated successfully!', 'success')
        return redirect(url_for('creator.series_edit', series_id=series.id))
    
    # Get comics in this series
    comics_in_series = series.comics.order_by(Comic.created_at.desc()).all()
    
    return render_template('creator/series_edit.html', 
                         series=series, 
                         comics_in_series=comics_in_series)

@bp.route('/series/<int:series_id>', strict_slashes=False)
@login_required
def series_detail(series_id):
    """View series details, analytics, and contained comics"""
    series = Series.query.get_or_404(series_id)

    # Access control: creators can only view their own series
    if series.author_id != current_user.id:
        flash('You can only view your own series.', 'error')
        return redirect(url_for('creator.dashboard'))

    # Get analytics and comics inside the series
    analytics = calculate_series_analytics(series_id)
    comics_in_series = series.comics.order_by(Comic.created_at.desc()).all()

    return render_template('creator/series_detail.html',
                           series=series,
                           comics=comics_in_series,
                           analytics=analytics)

@bp.route('/comic/<int:comic_id>/edit', methods=['GET', 'POST'])
@login_required
def comic_edit(comic_id):
    """Edit comic details"""
    comic = Comic.query.get_or_404(comic_id)
    
    if comic.author_id != current_user.id:
        flash('You can only edit your own comics.', 'error')
        return redirect(url_for('creator.dashboard'))
    
    if request.method == 'POST':
        comic.title = request.form.get('title', '').strip()
        comic.description = request.form.get('description', '').strip()
        comic.genre = request.form.get('genre', '').strip()
        comic.status = request.form.get('status', 'ongoing')
        comic.schedule = request.form.get('schedule', '')
        comic.content_type = request.form.get('content_type', 'comic')
        comic.is_published = 'is_published' in request.form
        
        # Handle series selection
        series_id = request.form.get('series_id', '').strip()
        if series_id:
            try:
                series_id = int(series_id)
                series = Series.query.get(series_id)
                if series and series.author_id == current_user.id:
                    comic.series_id = series_id
                else:
                    comic.series_id = None
            except (ValueError, TypeError):
                comic.series_id = None
        else:
            comic.series_id = None
        
        # Handle cover image upload
        if 'cover_image' in request.files:
            file = request.files['cover_image']
            if file and file.filename and allowed_file(file.filename):
                filename = secure_filename(f"{uuid.uuid4()}_{file.filename}")
                file.save(os.path.join(current_app.config['UPLOAD_FOLDER'], filename))
                comic.cover_image = f"uploads/{filename}"
        
        db.session.commit()
        flash('Comic updated successfully!', 'success')
        return redirect(url_for('creator.comic_edit', comic_id=comic.id))
    
    return render_template('creator/comic_edit.html', comic=comic)

@bp.route('/comic/<int:comic_id>/chapter/new', methods=['GET', 'POST'])
@login_required
def new_chapter(comic_id):
    """Upload a new chapter"""
    comic = Comic.query.get_or_404(comic_id)
    
    if comic.author_id != current_user.id:
        flash('You can only add chapters to your own comics.', 'error')
        return redirect(url_for('creator.dashboard'))
    
    if request.method == 'POST':
        title = request.form.get('title', '').strip()
        chapter_number = request.form.get('chapter_number', '').strip()
        scheduled_publish = request.form.get('scheduled_publish', '').strip()
        
        if not title or not chapter_number:
            flash('Title and chapter number are required.', 'error')
            return render_template('creator/new_chapter.html', comic=comic)
        
        try:
            chapter_number = float(chapter_number)
        except ValueError:
            flash('Chapter number must be a valid number.', 'error')
            return render_template('creator/new_chapter.html', comic=comic)
        
        # Check if chapter number already exists
        existing = Chapter.query.filter_by(
            comic_id=comic_id,
            chapter_number=chapter_number
        ).first()
        if existing:
            flash('A chapter with this number already exists.', 'error')
            return render_template('creator/new_chapter.html', comic=comic)
        
        # Parse scheduled publish date
        scheduled_publish_date = None
        if scheduled_publish:
            try:
                scheduled_publish_date = datetime.strptime(scheduled_publish, '%Y-%m-%d %H:%M')
            except ValueError:
                flash('Invalid scheduled publish date format.', 'error')
                return render_template('creator/new_chapter.html', comic=comic)
        
        chapter = Chapter(
            title=title,
            chapter_number=chapter_number,
            comic_id=comic_id,
            scheduled_publish=scheduled_publish_date,
            is_published=scheduled_publish_date is None  # Auto-publish if no schedule
        )
        
        if chapter.is_published:
            chapter.published_at = datetime.utcnow()
        
        db.session.add(chapter)
        db.session.commit()
        
        flash('Chapter created successfully!', 'success')
        return redirect(url_for('creator.chapter_edit', comic_id=comic_id, chapter_id=chapter.id))
    
    return render_template('creator/new_chapter.html', comic=comic)

@bp.route('/comic/<int:comic_id>/chapter/<int:chapter_id>/edit', methods=['GET', 'POST'])
@login_required
def chapter_edit(comic_id, chapter_id):
    """Edit chapter and upload pages"""
    comic = Comic.query.get_or_404(comic_id)
    chapter = Chapter.query.get_or_404(chapter_id)
    
    if comic.author_id != current_user.id:
        flash('You can only edit your own chapters.', 'error')
        return redirect(url_for('creator.dashboard'))
    
    if request.method == 'POST':
        # Handle page uploads
        if 'pages' in request.files:
            files = request.files.getlist('pages')
            page_number = len(chapter.pages.all()) + 1
            
            for file in files:
                if file and file.filename and allowed_file(file.filename):
                    filename = secure_filename(f"{uuid.uuid4()}_{file.filename}")
                    file.save(os.path.join(current_app.config['UPLOAD_FOLDER'], filename))
                    
                    page = ChapterPage(
                        page_number=page_number,
                        image_path=f"uploads/{filename}",
                        chapter_id=chapter.id
                    )
                    db.session.add(page)
                    page_number += 1
        
        # Update chapter details
        chapter.title = request.form.get('title', '').strip()
        chapter.is_published = 'is_published' in request.form
        
        if chapter.is_published and not chapter.published_at:
            chapter.published_at = datetime.utcnow()
        
        db.session.commit()
        flash('Chapter updated successfully!', 'success')
        return redirect(url_for('creator.chapter_edit', comic_id=comic_id, chapter_id=chapter.id))
    
    # Get existing pages
    pages = chapter.pages.order_by(ChapterPage.page_number).all()
    
    return render_template('creator/chapter_edit.html', 
                         comic=comic, 
                         chapter=chapter, 
                         pages=pages)

@bp.route('/comic/<int:comic_id>/chapter/<int:chapter_id>/page/<int:page_id>/delete', methods=['POST'])
@login_required
def delete_page(comic_id, chapter_id, page_id):
    """Delete a page from a chapter"""
    comic = Comic.query.get_or_404(comic_id)
    chapter = Chapter.query.get_or_404(chapter_id)
    page = ChapterPage.query.get_or_404(page_id)
    
    if comic.author_id != current_user.id:
        flash('You can only delete pages from your own chapters.', 'error')
        return redirect(url_for('creator.dashboard'))
    
    # Delete file from filesystem
    if page.image_path:
        file_path = os.path.join(current_app.config['UPLOAD_FOLDER'], page.image_path.replace('uploads/', ''))
        if os.path.exists(file_path):
            os.remove(file_path)
    
    db.session.delete(page)
    db.session.commit()
    
    flash('Page deleted successfully!', 'success')
    return redirect(url_for('creator.chapter_edit', comic_id=comic_id, chapter_id=chapter_id))

@bp.route('/profile', methods=['GET', 'POST'])
@login_required
def profile():
    """Edit creator profile"""
    if request.method == 'POST':
        current_user.username = request.form.get('username', '').strip()
        current_user.bio = request.form.get('bio', '').strip()
        
        # Handle avatar upload
        if 'avatar' in request.files:
            file = request.files['avatar']
            if file and file.filename and allowed_file(file.filename):
                filename = secure_filename(f"{uuid.uuid4()}_{file.filename}")
                file.save(os.path.join(current_app.config['UPLOAD_FOLDER'], filename))
                current_user.avatar = f"uploads/{filename}"
        
        db.session.commit()
        flash('Profile updated successfully!', 'success')
        return redirect(url_for('creator.profile'))
    
    return render_template('creator/profile.html')

@bp.route('/schedule')
@login_required
def schedule():
    """View and manage chapter schedule"""
    if not current_user.is_artist:
        flash('You need to be an artist to access the schedule.', 'error')
        return redirect(url_for('main.index'))
    
    # Get upcoming scheduled chapters
    upcoming_chapters = Chapter.query.join(Comic).filter(
        Comic.author_id == current_user.id,
        Chapter.scheduled_publish.isnot(None),
        Chapter.scheduled_publish > datetime.utcnow()
    ).order_by(Chapter.scheduled_publish).all()
    
    # Get recently published chapters
    recent_chapters = Chapter.query.join(Comic).filter(
        Comic.author_id == current_user.id,
        Chapter.published_at.isnot(None)
    ).order_by(Chapter.published_at.desc()).limit(10).all()
    
    return render_template('creator/schedule.html',
                         upcoming_chapters=upcoming_chapters,
                         recent_chapters=recent_chapters)

@bp.route('/series/<int:series_id>/defaults')
@login_required
def series_defaults(series_id):
    """Return default genre, status, and content_type for a series (JSON)."""
    series = Series.query.get_or_404(series_id)
    if series.author_id != current_user.id:
        return jsonify({'error': 'Unauthorized'}), 403

    # Determine default content type by most common among comics in series
    comics = series.comics.all()
    if comics:
        ct_counter = Counter([c.content_type for c in comics if c.content_type])
        content_type = ct_counter.most_common(1)[0][0] if ct_counter else 'comic'
    else:
        content_type = 'comic'

    return jsonify({
        'genre': series.genre or '',
        'status': series.status or 'ongoing',
        'content_type': content_type,
        'schedule': series.schedule or ''
    })

@bp.route('/comic/<int:comic_id>/delete', methods=['POST'])
@login_required
def delete_comic(comic_id):
    """Delete a comic along with chapters and pages."""
    comic = Comic.query.get_or_404(comic_id)
    if comic.author_id != current_user.id:
        flash('You can only delete your own comics.', 'error')
        return redirect(url_for('creator.dashboard'))

    db.session.delete(comic)
    db.session.commit()
    flash('Comic deleted successfully!', 'success')
    return redirect(url_for('creator.dashboard')) 