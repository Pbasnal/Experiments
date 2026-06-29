import { Link, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';

interface LayoutProps {
  children: ReactNode;
}

const navItems = [
  { label: 'Home', href: '/', external: false },
  { label: 'Creator', href: '/creator', external: true },
  { label: 'Admin', href: '/admin', external: true },
];

export default function Layout({ children }: LayoutProps) {
  const location = useLocation();

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
          <a href="/oauth2/authorization/google" className="btn-signin">
            Sign in
          </a>
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
