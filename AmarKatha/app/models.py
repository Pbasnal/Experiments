from datetime import datetime
from werkzeug.security import generate_password_hash, check_password_hash
from flask_login import UserMixin
from app import db, login_manager
import json

class User(UserMixin, db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(64), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(512))
    is_artist = db.Column(db.Boolean, default=False)
    bio = db.Column(db.Text)
    avatar = db.Column(db.String(200))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    comics = db.relationship('Comic', backref='author', lazy='dynamic')
    series = db.relationship('Series', backref='author', lazy='dynamic')
    
    # Follow relationships
    following = db.relationship('Follow', foreign_keys='Follow.follower_id', backref='follower', lazy='dynamic')
    followers = db.relationship('Follow', foreign_keys='Follow.followed_id', backref='followed', lazy='dynamic')

    def set_password(self, password):
        self.password_hash = generate_password_hash(password)

    def check_password(self, password):
        return check_password_hash(self.password_hash, password)

    def follow(self, user):
        if not self.is_following(user):
            f = Follow(follower=self, followed=user)
            db.session.add(f)

    def unfollow(self, user):
        f = self.following.filter_by(followed_id=user.id).first()
        if f:
            db.session.delete(f)

    def is_following(self, user):
        return self.following.filter_by(followed_id=user.id).count() > 0

    def __repr__(self):
        return f'<User {self.username}>'

class Series(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False)
    description = db.Column(db.Text)
    cover_image = db.Column(db.String(200))
    genre = db.Column(db.String(50))
    status = db.Column(db.String(20), default='ongoing')  # ongoing, completed, hiatus, cancelled
    schedule = db.Column(db.String(20))  # weekly, biweekly, monthly, irregular
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    author_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    
    # Relationships
    comics = db.relationship('Comic', backref='series', lazy='dynamic')

    def __repr__(self):
        return f'<Series {self.name}>'

class Comic(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(100), nullable=False)
    description = db.Column(db.Text)
    cover_image = db.Column(db.String(200))
    genre = db.Column(db.String(50))  # e.g., "Shonen", "Romance", "Drama"
    status = db.Column(db.String(20), default='ongoing')  # ongoing, completed, hiatus
    schedule = db.Column(db.String(20))  # weekly, biweekly, monthly
    content_type = db.Column(db.String(20), default='comic')  # comic, story, mixed, standalone
    series_id = db.Column(db.Integer, db.ForeignKey('series.id'), nullable=True)
    tags = db.Column(db.Text)  # JSON string of tags
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    author_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    is_published = db.Column(db.Boolean, default=False)
    is_editor_pick = db.Column(db.Boolean, default=False)
    total_views = db.Column(db.Integer, default=0)
    total_rating = db.Column(db.Float, default=0.0)
    rating_count = db.Column(db.Integer, default=0)
    
    # Relationships
    chapters = db.relationship('Chapter', backref='comic', lazy='dynamic', cascade='all, delete-orphan')
    ratings = db.relationship('Rating', backref='comic', lazy='dynamic', cascade='all, delete-orphan')
    follows = db.relationship('ComicFollow', backref='comic', lazy='dynamic', cascade='all, delete-orphan')

    def average_rating(self):
        if self.rating_count > 0:
            return round(self.total_rating / self.rating_count, 1)
        return 0.0

    def get_tags(self):
        """Get tags as a list"""
        if self.tags:
            try:
                return json.loads(self.tags)
            except json.JSONDecodeError:
                return []
        return []

    def set_tags(self, tags_list):
        """Set tags from a list"""
        if isinstance(tags_list, list):
            self.tags = json.dumps(tags_list)
        else:
            self.tags = None

    def __repr__(self):
        return f'<Comic {self.title}>'

class Chapter(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(100), nullable=False)
    chapter_number = db.Column(db.Float, nullable=False)  # Float to support 1.5, 2.1, etc.
    comic_id = db.Column(db.Integer, db.ForeignKey('comic.id'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    published_at = db.Column(db.DateTime)
    is_published = db.Column(db.Boolean, default=False)
    scheduled_publish = db.Column(db.DateTime)
    total_views = db.Column(db.Integer, default=0)
    total_rating = db.Column(db.Float, default=0.0)
    rating_count = db.Column(db.Integer, default=0)
    
    # Relationships
    pages = db.relationship('ChapterPage', backref='chapter', lazy='dynamic', cascade='all, delete-orphan')
    comments = db.relationship('Comment', backref='chapter', lazy='dynamic', cascade='all, delete-orphan')
    ratings = db.relationship('ChapterRating', backref='chapter', lazy='dynamic', cascade='all, delete-orphan')

    def average_rating(self):
        if self.rating_count > 0:
            return round(self.total_rating / self.rating_count, 1)
        return 0.0

    def __repr__(self):
        return f'<Chapter {self.title} of Comic {self.comic_id}>'

class ChapterPage(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    page_number = db.Column(db.Integer, nullable=False)
    image_path = db.Column(db.String(200), nullable=False)
    chapter_id = db.Column(db.Integer, db.ForeignKey('chapter.id'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def __repr__(self):
        return f'<ChapterPage {self.page_number} of Chapter {self.chapter_id}>'

class Comment(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    content = db.Column(db.Text, nullable=False)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    chapter_id = db.Column(db.Integer, db.ForeignKey('chapter.id'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    is_edited = db.Column(db.Boolean, default=False)
    
    # Relationships
    user = db.relationship('User', backref='comments')

    def __repr__(self):
        return f'<Comment {self.id} by User {self.user_id}>'

class Rating(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    comic_id = db.Column(db.Integer, db.ForeignKey('comic.id'), nullable=False)
    rating = db.Column(db.Integer, nullable=False)  # 1-5 stars
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relationships
    user = db.relationship('User', backref='comic_ratings')

    __table_args__ = (db.UniqueConstraint('user_id', 'comic_id', name='_user_comic_rating_uc'),)

    def __repr__(self):
        return f'<Rating {self.rating} by User {self.user_id} for Comic {self.comic_id}>'

class ChapterRating(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    chapter_id = db.Column(db.Integer, db.ForeignKey('chapter.id'), nullable=False)
    rating = db.Column(db.Integer, nullable=False)  # 1-5 stars
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relationships
    user = db.relationship('User', backref='chapter_ratings')

    __table_args__ = (db.UniqueConstraint('user_id', 'chapter_id', name='_user_chapter_rating_uc'),)

    def __repr__(self):
        return f'<ChapterRating {self.rating} by User {self.user_id} for Chapter {self.chapter_id}>'

class Follow(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    follower_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    followed_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    __table_args__ = (db.UniqueConstraint('follower_id', 'followed_id', name='_follower_followed_uc'),)

    def __repr__(self):
        return f'<Follow {self.follower_id} -> {self.followed_id}>'

class ComicFollow(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    comic_id = db.Column(db.Integer, db.ForeignKey('comic.id'), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    user = db.relationship('User', backref='comic_follows')

    __table_args__ = (db.UniqueConstraint('user_id', 'comic_id', name='_user_comic_follow_uc'),)

    def __repr__(self):
        return f'<ComicFollow User {self.user_id} -> Comic {self.comic_id}>'

class ViewLog(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=True)  # Null for anonymous users
    comic_id = db.Column(db.Integer, db.ForeignKey('comic.id'), nullable=False)
    chapter_id = db.Column(db.Integer, db.ForeignKey('chapter.id'), nullable=True)
    ip_address = db.Column(db.String(45))
    user_agent = db.Column(db.Text)
    viewed_at = db.Column(db.DateTime, default=datetime.utcnow)
    dwell_time = db.Column(db.Integer, default=0)  # Time spent in seconds
    
    # Relationships
    user = db.relationship('User', backref='view_logs')
    comic = db.relationship('Comic', backref='view_logs')
    chapter = db.relationship('Chapter', backref='view_logs')

    def __repr__(self):
        return f'<ViewLog User {self.user_id} -> Comic {self.comic_id}>'

@login_manager.user_loader
def load_user(id):
    return User.query.get(int(id)) 