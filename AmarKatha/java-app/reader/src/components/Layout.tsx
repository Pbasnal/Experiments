import { Link, useLocation } from 'react-router-dom';
import { useEffect, useState, type ReactNode } from 'react';
import { fetchMe, logout, type AuthMeResponse } from '../api/auth';

interface LayoutProps {
  children: ReactNode;
}

const navItems = [
  { label: 'Home', href: '/', external: false },
  { label: 'Creator', href: '/creator', external: true },
];

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

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="header-inner">
          <Link to="/" className="brand">
            <span className="brand-mark">अ</span>
            <span className="brand-text">AmarKatha</span>
          </Link>
          <nav className="site-nav" aria-label="Main">
            {navItems.map((item) =>
              item.external ? (
                <a key={item.href} href={item.href} className="nav-link">
                  {item.label}
                </a>
              ) : (
                <Link
                  key={item.href}
                  to={item.href}
                  className={`nav-link${location.pathname === item.href ? ' active' : ''}`}
                >
                  {item.label}
                </Link>
              ),
            )}
          </nav>
          <div className="auth-actions">
            {auth?.authenticated ? (
              <>
                {auth.role === 'ADMIN' && (
                  <a href="/admin" className="nav-link">
                    Admin
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
              <a href="/creator/login" className="btn-signin">
                Sign in
              </a>
            )}
          </div>
        </div>
      </header>
      <main>{children}</main>
      <footer className="site-footer">
        <p>V0 UI preview · Scheduling-first Indian comics platform</p>
        <div className="footer-links">
          <a href="/legal/terms">Terms</a>
          <a href="/legal/privacy">Privacy</a>
          <a href="/legal/grievance">Grievance</a>
        </div>
      </footer>
    </div>
  );
}
