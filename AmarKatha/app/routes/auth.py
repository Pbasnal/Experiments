from flask import Blueprint, render_template, redirect, url_for, flash, request, current_app
from flask_login import login_user, logout_user, login_required, current_user
from app import db
from app.models import User
from urllib.parse import urlparse
import uuid

bp = Blueprint('auth', __name__)

@bp.route('/register', methods=['GET', 'POST'])
def register():
    if current_user.is_authenticated:
        return redirect(url_for('main.index'))
    
    if request.method == 'POST':
        username = request.form['username']
        email = request.form['email']
        password = request.form['password']
        is_artist = 'is_artist' in request.form

        if User.query.filter_by(username=username).first():
            flash('Username already exists')
            return redirect(url_for('auth.register'))
        
        if User.query.filter_by(email=email).first():
            flash('Email already registered')
            return redirect(url_for('auth.register'))

        user = User(username=username, email=email, is_artist=is_artist)
        user.set_password(password)
        db.session.add(user)
        db.session.commit()
        
        flash('Registration successful!')
        return redirect(url_for('auth.login'))
    
    return render_template('auth/register.html')

@bp.route('/become-creator', methods=['GET', 'POST'])
@login_required
def become_creator():
    """Allow users to become creators"""
    if current_user.is_artist:
        flash('You are already a creator!')
        return redirect(url_for('creator.dashboard'))
    
    if request.method == 'POST':
        current_user.is_artist = True
        db.session.commit()
        flash('Congratulations! You are now a creator!')
        return redirect(url_for('creator.dashboard'))
    
    return render_template('auth/become_creator.html')

@bp.route('/login', methods=['GET', 'POST'])
def login():
    if current_user.is_authenticated:
        return redirect(url_for('main.index'))
    
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']
        remember = 'remember' in request.form
        
        user = User.query.filter_by(username=username).first()
        if user is None or not user.check_password(password):
            flash('Invalid username or password')
            return redirect(url_for('auth.login'))
        
        login_user(user, remember=remember)
        next_page = request.args.get('next')
        if not next_page or urlparse(next_page).netloc != '':
            next_page = url_for('main.index')
        return redirect(next_page)
    
    return render_template('auth/login.html')

@bp.route('/logout')
@login_required
def logout():
    logout_user()
    return redirect(url_for('main.index'))

@bp.route('/auth/google')
def google_login():
    """Initiate Google OAuth login"""
    try:
        from flask_dance.contrib.google import google
        # Check if google blueprint is properly registered
        if 'google' not in current_app.blueprints:
            flash('Google OAuth is not configured. Please contact the administrator.')
            return redirect(url_for('auth.login'))
        
        if not google.authorized:
            return redirect(url_for('google.login'))
        
        # If already authorized, get user info and log them in
        return handle_google_user()
    except ImportError:
        flash('Google OAuth is not available. Flask-Dance is not installed.')
        flash('To enable OAuth, install: pip install Flask-Dance==7.0.0 oauthlib==3.2.2')
        return redirect(url_for('auth.login'))

@bp.route('/auth/handle-google-user')
def handle_google_user():
    """Handle Google user authentication after OAuth"""
    try:
        from flask_dance.contrib.google import google
        
        if not google.authorized:
            flash('Google OAuth authorization failed')
            return redirect(url_for('auth.login'))
        
        resp = google.get('/oauth2/v2/userinfo')
        
        if resp.ok:
            user_info = resp.json()
            email = user_info['email']
            
            # First try to find user by email
            user = User.query.filter_by(email=email).first()
            
            if not user:
                # If no user exists with this email, create new user
                base_username = user_info.get('given_name', email.split('@')[0])
                username = base_username
                counter = 1
                
                # Keep trying usernames until we find a unique one
                while User.query.filter_by(username=username).first():
                    username = f"{base_username}{counter}"
                    counter += 1
                
                user = User(
                    username=username,
                    email=email,
                    is_artist=False  # Default to reader, can upgrade later
                )
                db.session.add(user)
                db.session.commit()
                flash('Account created successfully with Google! To start creating comics, become a creator from your profile.')
            else:
                flash('Welcome back!')
            
            login_user(user)
            
            # If they're not a creator, suggest becoming one
            if not user.is_artist:
                flash('Want to create comics? Become a creator!')
                return redirect(url_for('auth.become_creator'))
            
            next_page = request.args.get('next')
            if not next_page or urlparse(next_page).netloc != '':
                next_page = url_for('main.index')
            return redirect(next_page)
        else:
            flash('Failed to get user info from Google')
            return redirect(url_for('auth.login'))
    except Exception as e:
        print(f"Error handling Google user: {str(e)}")
        flash(f'OAuth error: {str(e)}')
        return redirect(url_for('auth.login'))