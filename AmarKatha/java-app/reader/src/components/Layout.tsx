import { Link, useLocation } from 'react-router-dom';
import { useEffect, useState, type ReactNode } from 'react';
import { fetchMe, logout, type AuthMeResponse } from '../api/auth';

interface LayoutProps {
  children: ReactNode;
}

export default function Layout({ children }: LayoutProps) {
  const location = useLocation();
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
    setAuth({ authenticated: false });
    window.location.href = '/';
  }

  const isHome = location.pathname === '/';

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="header-inner">
          <Link to="/" className="brand">
            <span className="brand-mark">अ</span>
            <span className="brand-text">AmarKatha</span>
          </Link>
          <nav className="site-nav" aria-label="Main">
            <Link to="/" className={`nav-link${isHome ? ' active' : ''}`}>
              Stories
            </Link>
            <a href="/creator/signup" className="nav-link">
              For creators
            </a>
          </nav>
          <div className="auth-actions">
            {auth?.authenticated ? (
              <>
                {auth.role === 'ADMIN' && (
                  <a href="/admin" className="nav-link">
                    Admin
                  </a>
                )}
                {(auth.role === 'CREATOR' || auth.role === 'ADMIN') && (
                  <a href="/creator" className="nav-link">
                    Dashboard
                  </a>
                )}
                <a href="/profile" className="auth-email" title={auth.email}>
                  {auth.displayName || auth.email}
                </a>
                <button type="button" className="btn-signin btn-signout" onClick={handleLogout}>
                  Sign out
                </button>
              </>
            ) : (
              <a href="/login" className="btn-signin btn-signin-quiet">
                Sign in
              </a>
            )}
          </div>
        </div>
      </header>
      <main>{children}</main>
      <footer className="site-footer">
        <p className="footer-tagline">Indian indie comics · flexible schedules · share a link</p>
        <div className="footer-links">
          <a href="/legal/terms">Terms</a>
          <a href="/legal/privacy">Privacy</a>
          <a href="/legal/grievance">Grievance</a>
        </div>
      </footer>
    </div>
  );
}
