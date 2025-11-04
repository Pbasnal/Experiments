from flask import Blueprint, render_template, request, redirect, url_for, flash, jsonify
from flask_login import login_required, current_user
from app.models import Comic, Chapter, User, Comment, ViewLog, Rating
from app import db
from datetime import datetime, timedelta
from sqlalchemy import func
from functools import wraps

bp = Blueprint('admin', __name__, url_prefix='/admin')

def admin_required(f):
    """Decorator to check if user is admin"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if not current_user.is_authenticated or not current_user.is_artist:
            flash('Admin access required.', 'error')
            return redirect(url_for('main.index'))
        return f(*args, **kwargs)
    return decorated_function

@bp.route('/dashboard')
@login_required
@admin_required
def dashboard():
    """Admin dashboard with key metrics"""
    # Get basic stats
    total_comics = Comic.query.count()
    total_users = User.query.count()
    total_artists = User.query.filter_by(is_artist=True).count()
    total_views = db.session.query(func.sum(Comic.total_views)).scalar() or 0
    
    # Get recent activity
    recent_comics = Comic.query.order_by(Comic.created_at.desc()).limit(5).all()
    recent_users = User.query.order_by(User.created_at.desc()).limit(5).all()
    
    # Get trending comics (last 7 days)
    week_ago = datetime.utcnow() - timedelta(days=7)
    trending_comics = Comic.query.join(ViewLog).filter(
        ViewLog.viewed_at >= week_ago,
        Comic.is_published == True
    ).group_by(Comic.id).order_by(
        func.count(ViewLog.id).desc()
    ).limit(10).all()
    
    # Get top rated comics
    top_rated = Comic.query.filter(
        Comic.rating_count > 0,
        Comic.is_published == True
    ).order_by(Comic.total_rating.desc()).limit(10).all()
    
    return render_template('admin/dashboard.html',
                         total_comics=total_comics,
                         total_users=total_users,
                         total_artists=total_artists,
                         total_views=total_views,
                         recent_comics=recent_comics,
                         recent_users=recent_users,
                         trending_comics=trending_comics,
                         top_rated=top_rated)

@bp.route('/comics')
@login_required
@admin_required
def comics():
    """Manage all comics"""
    page = request.args.get('page', 1, type=int)
    status = request.args.get('status', '')
    
    query = Comic.query
    
    if status == 'published':
        query = query.filter_by(is_published=True)
    elif status == 'unpublished':
        query = query.filter_by(is_published=False)
    elif status == 'editor_picks':
        query = query.filter_by(is_editor_pick=True)
    
    comics = query.order_by(Comic.created_at.desc()).paginate(
        page=page, per_page=20, error_out=False
    )
    
    return render_template('admin/comics.html', comics=comics, status=status)

@bp.route('/comic/<int:comic_id>/toggle_publish', methods=['POST'])
@login_required
@admin_required
def toggle_publish(comic_id):
    """Toggle comic publish status"""
    comic = Comic.query.get_or_404(comic_id)
    comic.is_published = not comic.is_published
    
    if comic.is_published:
        flash(f'Comic "{comic.title}" has been published.', 'success')
    else:
        flash(f'Comic "{comic.title}" has been unpublished.', 'success')
    
    db.session.commit()
    return redirect(url_for('admin.comics'))

@bp.route('/comic/<int:comic_id>/toggle_editor_pick', methods=['POST'])
@login_required
@admin_required
def toggle_editor_pick(comic_id):
    """Toggle editor pick status"""
    comic = Comic.query.get_or_404(comic_id)
    comic.is_editor_pick = not comic.is_editor_pick
    
    if comic.is_editor_pick:
        flash(f'Comic "{comic.title}" has been added to editor picks.', 'success')
    else:
        flash(f'Comic "{comic.title}" has been removed from editor picks.', 'success')
    
    db.session.commit()
    return redirect(url_for('admin.comics'))

@bp.route('/comic/<int:comic_id>/delete', methods=['POST'])
@login_required
@admin_required
def delete_comic(comic_id):
    """Delete a comic"""
    comic = Comic.query.get_or_404(comic_id)
    title = comic.title
    
    db.session.delete(comic)
    db.session.commit()
    
    flash(f'Comic "{title}" has been deleted.', 'success')
    return redirect(url_for('admin.comics'))

@bp.route('/comments')
@login_required
@admin_required
def comments():
    """Moderate comments"""
    page = request.args.get('page', 1, type=int)
    
    comments = Comment.query.order_by(Comment.created_at.desc()).paginate(
        page=page, per_page=50, error_out=False
    )
    
    return render_template('admin/comments.html', comments=comments)

@bp.route('/comment/<int:comment_id>/delete', methods=['POST'])
@login_required
@admin_required
def delete_comment(comment_id):
    """Delete a comment"""
    comment = Comment.query.get_or_404(comment_id)
    
    db.session.delete(comment)
    db.session.commit()
    
    flash('Comment has been deleted.', 'success')
    return redirect(url_for('admin.comments'))

@bp.route('/users')
@login_required
@admin_required
def users():
    """Manage users"""
    page = request.args.get('page', 1, type=int)
    role = request.args.get('role', '')
    
    query = User.query
    
    if role == 'artists':
        query = query.filter_by(is_artist=True)
    elif role == 'readers':
        query = query.filter_by(is_artist=False)
    
    users = query.order_by(User.created_at.desc()).paginate(
        page=page, per_page=20, error_out=False
    )
    
    return render_template('admin/users.html', users=users, role=role)

@bp.route('/user/<int:user_id>/toggle_artist', methods=['POST'])
@login_required
@admin_required
def toggle_artist(user_id):
    """Toggle user artist status"""
    user = User.query.get_or_404(user_id)
    user.is_artist = not user.is_artist
    
    if user.is_artist:
        flash(f'User "{user.username}" has been granted artist privileges.', 'success')
    else:
        flash(f'User "{user.username}" artist privileges have been revoked.', 'success')
    
    db.session.commit()
    return redirect(url_for('admin.users'))

@bp.route('/analytics')
@login_required
@admin_required
def analytics():
    """Detailed analytics dashboard"""
    # Get date range
    days = request.args.get('days', 30, type=int)
    end_date = datetime.utcnow()
    start_date = end_date - timedelta(days=days)
    
    # Views over time
    daily_views = db.session.query(
        func.date(ViewLog.viewed_at).label('date'),
        func.count(ViewLog.id).label('views')
    ).filter(
        ViewLog.viewed_at >= start_date
    ).group_by(
        func.date(ViewLog.viewed_at)
    ).order_by(
        func.date(ViewLog.viewed_at)
    ).all()
    
    # Top comics by views
    top_comics_views = Comic.query.join(ViewLog).filter(
        ViewLog.viewed_at >= start_date,
        Comic.is_published == True
    ).group_by(Comic.id).order_by(
        func.count(ViewLog.id).desc()
    ).limit(10).all()
    
    # Top comics by rating
    top_comics_rating = Comic.query.filter(
        Comic.rating_count > 0,
        Comic.is_published == True
    ).order_by(Comic.total_rating.desc()).limit(10).all()
    
    # Genre distribution
    genre_stats = db.session.query(
        Comic.genre,
        func.count(Comic.id).label('count'),
        func.avg(Comic.total_rating).label('avg_rating')
    ).filter(
        Comic.genre.isnot(None),
        Comic.is_published == True
    ).group_by(Comic.genre).all()
    
    # User engagement
    total_users = User.query.count()
    active_users = db.session.query(func.count(func.distinct(ViewLog.user_id))).filter(
        ViewLog.viewed_at >= start_date,
        ViewLog.user_id.isnot(None)
    ).scalar()
    
    return render_template('admin/analytics.html',
                         days=days,
                         daily_views=daily_views,
                         top_comics_views=top_comics_views,
                         top_comics_rating=top_comics_rating,
                         genre_stats=genre_stats,
                         total_users=total_users,
                         active_users=active_users)

@bp.route('/api/stats')
@login_required
@admin_required
def api_stats():
    """API endpoint for dashboard stats"""
    total_comics = Comic.query.count()
    total_users = User.query.count()
    total_artists = User.query.filter_by(is_artist=True).count()
    total_views = db.session.query(func.sum(Comic.total_views)).scalar() or 0
    
    return jsonify({
        'total_comics': total_comics,
        'total_users': total_users,
        'total_artists': total_artists,
        'total_views': total_views
    }) 