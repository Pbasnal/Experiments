from flask import Blueprint, render_template, request, redirect, url_for, flash, current_app
from app.models import Comic, Chapter, User, Rating, Comment, Follow, ComicFollow, ViewLog
from flask_login import login_required, current_user
from app import db
from datetime import datetime, timedelta
from sqlalchemy import func, desc
import os

bp = Blueprint('main', __name__)

@bp.route('/')
def index():
    """Home page with trending, new, and editor picks"""
    # Get trending comics (based on views in last 7 days)
    trending_comics = Comic.query.join(ViewLog).filter(
        ViewLog.viewed_at >= datetime.utcnow() - timedelta(days=7)
    ).group_by(Comic.id).order_by(
        func.count(ViewLog.id).desc()
    ).limit(10).all()
    
    # Get new comics (created in last 30 days)
    new_comics = Comic.query.filter(
        Comic.created_at >= datetime.utcnow() - timedelta(days=30)
    ).order_by(Comic.created_at.desc()).limit(10).all()
    
    # Get editor picks (manually curated)
    editor_picks = Comic.query.filter_by(is_editor_pick=True).limit(5).all()
    
    return render_template('index.html', 
                         trending_comics=trending_comics,
                         new_comics=new_comics,
                         editor_picks=editor_picks)

@bp.route('/about')
def about():
    return render_template('about.html')

@bp.route('/redirect-to-https')
def redirect_to_https():
    """Redirect HTTP requests to HTTPS"""
    if request.scheme == 'http':
        # Construct HTTPS URL
        https_url = request.url.replace('http://', 'https://', 1)
        return redirect(https_url, code=301)
    return redirect(url_for('main.index'))

@bp.route('/comic/<int:comic_id>')
def comic_detail(comic_id):
    comic = Comic.query.get_or_404(comic_id)
    chapters = Chapter.query.filter_by(comic_id=comic_id, is_published=True).order_by(Chapter.chapter_number).all()
    return render_template('comic/detail.html', comic=comic, chapters=chapters)

@bp.route('/search')
def search():
    query = request.args.get('q', '')
    genre = request.args.get('genre', '')
    
    comics_query = Comic.query.filter_by(is_published=True)
    
    if query:
        comics_query = comics_query.filter(Comic.title.ilike(f'%{query}%'))
    
    if genre:
        comics_query = comics_query.filter_by(genre=genre)
    
    comics = comics_query.order_by(Comic.created_at.desc()).all()
    return render_template('search.html', comics=comics, query=query, genre=genre) 