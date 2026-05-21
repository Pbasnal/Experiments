from flask import Blueprint, render_template, redirect, url_for, flash, request, current_app
from flask_login import login_user, logout_user, login_required, current_user
from app import db
from app.models import User
from urllib.parse import urlparse
import re
import secrets

bp = Blueprint('auth', __name__)


def _google_oauth_ready():
    return 'google' in current_app.blueprints


def _safe_next_url():
    next_page = request.args.get('next')
    if next_page and urlparse(next_page).netloc == '':
        return next_page
    return url_for('main.index')


def _unique_username(base: str) -> str:
    """Derive a unique username from Google profile data."""
    slug = re.sub(r'[^a-zA-Z0-9_]', '', (base or 'user').lower())[:50] or 'user'
    username = slug
    counter = 1
    while User.query.filter_by(username=username).first():
        username = f"{slug}{counter}"
        counter += 1
    return username


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
        return redirect(_safe_next_url())

    return render_template('auth/login.html')


@bp.route('/logout')
@login_required
def logout():
    logout_user()
    return redirect(url_for('main.index'))


@bp.route('/auth/google')
def google_login():
    """Start Google OAuth (redirects to Flask-Dance /google)."""
    if not _google_oauth_ready():
        flash(
            'Google sign-in is not configured. Add credentials to .env or google_credentials.json '
            'and restart the app. See docs/oauth-google.md.',
            'error',
        )
        return redirect(url_for('auth.login'))

    from flask_dance.contrib.google import google

    if google.authorized:
        return redirect(url_for('auth.handle_google_user'))

    return redirect(url_for('google.login', _external=False))


@bp.route('/auth/handle-google-user')
def handle_google_user():
    """Complete login after Google redirects to /google/authorized."""
    if not _google_oauth_ready():
        flash('Google sign-in is not configured.', 'error')
        return redirect(url_for('auth.login'))

    try:
        from flask_dance.contrib.google import google

        if not google.authorized:
            flash('Google sign-in was cancelled or failed. Please try again.', 'error')
            return redirect(url_for('auth.login'))

        resp = google.get('/oauth2/v2/userinfo')
        if not resp.ok:
            flash('Could not load your Google profile. Please try again.', 'error')
            current_app.logger.warning('Google userinfo failed: %s', resp.text)
            return redirect(url_for('auth.login'))

        user_info = resp.json()
        email = user_info.get('email')
        if not email:
            flash('Your Google account did not provide an email address.', 'error')
            return redirect(url_for('auth.login'))

        user = User.query.filter_by(email=email).first()

        if not user:
            base_username = (
                user_info.get('given_name')
                or user_info.get('name')
                or email.split('@')[0]
            )
            user = User(
                username=_unique_username(base_username),
                email=email,
                is_artist=False,
            )
            # OAuth-only account; random password blocks empty-hash edge cases
            user.set_password(secrets.token_urlsafe(32))
            db.session.add(user)
            db.session.commit()
            flash('Account created with Google!', 'success')
        else:
            flash('Welcome back!', 'success')

        login_user(user, remember=True)

        if not user.is_artist:
            flash('Want to publish comics? You can become a creator anytime.')
            return redirect(url_for('auth.become_creator'))

        return redirect(_safe_next_url())

    except Exception as exc:
        current_app.logger.exception('Google OAuth error')
        flash(f'Sign-in error: {exc}', 'error')
        return redirect(url_for('auth.login'))
