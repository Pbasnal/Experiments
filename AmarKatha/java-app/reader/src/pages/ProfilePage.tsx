import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchMe, logout, type AuthMeResponse } from '../api/auth';

export default function ProfilePage() {
  const [auth, setAuth] = useState<AuthMeResponse | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchMe().then((me) => {
      if (!cancelled) {
        setAuth(me);
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleLogout() {
    await logout();
    window.location.href = '/';
  }

  if (auth === null) {
    return (
      <section className="profile-page">
        <p className="muted">Loading…</p>
      </section>
    );
  }

  if (!auth.authenticated) {
    return (
      <section className="profile-page">
        <h1>Profile</h1>
        <p className="subtitle">Sign in to view your reader profile.</p>
        <a href="/creator/login" className="btn btn-primary">
          Sign in
        </a>
      </section>
    );
  }

  return (
    <section className="profile-page">
      <h1>Profile</h1>
      <p className="subtitle">Your reader account.</p>
      <dl className="profile-dl">
        <div>
          <dt>Name</dt>
          <dd>{auth.displayName || '—'}</dd>
        </div>
        <div>
          <dt>Email</dt>
          <dd>{auth.email}</dd>
        </div>
        <div>
          <dt>Role</dt>
          <dd>{auth.role}</dd>
        </div>
      </dl>
      <p className="muted">More profile settings coming soon.</p>
      <div className="profile-actions">
        <Link to="/" className="btn btn-secondary">
          Home
        </Link>
        {auth.role === 'CREATOR' && (
          <a href="/creator" className="btn btn-secondary">
            Creator home
          </a>
        )}
        {auth.role === 'ADMIN' && (
          <a href="/admin" className="btn btn-secondary">
            Admin
          </a>
        )}
        <button type="button" className="btn btn-signin btn-signout" onClick={handleLogout}>
          Sign out
        </button>
      </div>
    </section>
  );
}
