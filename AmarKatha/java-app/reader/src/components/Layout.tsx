import { Link, useLocation } from 'react-router-dom';
import { useEffect, useId, useRef, useState, type ReactNode } from 'react';
import { trackSignupClick } from '../api/analytics';
import { fetchMe, logout, type AuthMeResponse } from '../api/auth';
import {
  EngagementAuthError,
  fetchUnreadCount,
  NOTIFICATIONS_CHANGED_EVENT,
} from '../api/engagement';
import { getFocusableElements } from '../utils/focusTrap';
import { useFeatures } from '../features/FeatureContext';
import FeedbackFab from './FeedbackFab';

interface LayoutProps {
  children: ReactNode;
}

const UNREAD_POLL_MS = 90_000;

export default function Layout({ children }: LayoutProps) {
  const location = useLocation();
  const { features, loading: featuresLoading } = useFeatures();
  const [auth, setAuth] = useState<AuthMeResponse | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const menuId = useId();
  const menuToggleRef = useRef<HTMLButtonElement>(null);
  const mobileNavRef = useRef<HTMLDivElement>(null);

  // Fail-closed while features load: personalized chrome stays hidden.
  const showProfile = !featuresLoading && features.profileProgress;
  const showNotifications = !featuresLoading && features.inAppNotifications && showProfile;

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

  useEffect(() => {
    setMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!menuOpen) {
      return;
    }
    const nav = mobileNavRef.current;
    const first = nav ? getFocusableElements(nav)[0] : null;
    first?.focus();

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        event.preventDefault();
        setMenuOpen(false);
      }
    }
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      menuToggleRef.current?.focus();
    };
  }, [menuOpen]);

  useEffect(() => {
    if (!auth?.authenticated || !showNotifications) {
      setUnreadCount(0);
      return;
    }

    let cancelled = false;

    async function refreshUnread() {
      try {
        const result = await fetchUnreadCount();
        if (!cancelled) {
          setUnreadCount(result.unreadCount);
        }
      } catch (e) {
        if (cancelled) {
          return;
        }
        if (e instanceof EngagementAuthError) {
          setUnreadCount(0);
        }
        // keep last known count on transient errors
      }
    }

    void refreshUnread();
    const intervalId = window.setInterval(() => {
      void refreshUnread();
    }, UNREAD_POLL_MS);

    function onNotificationsChanged() {
      void refreshUnread();
    }
    window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, onNotificationsChanged);

    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
      window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, onNotificationsChanged);
    };
  }, [auth?.authenticated, showNotifications]);

  async function handleLogout() {
    await logout();
    setAuth({ authenticated: false });
    window.location.href = '/';
  }

  const isHome = location.pathname === '/';
  const isProfile = location.pathname === '/read/profile';
  const badgeLabel =
    unreadCount > 0
      ? unreadCount > 99
        ? '99+ unread notifications'
        : `${unreadCount} unread notification${unreadCount === 1 ? '' : 's'}`
      : 'Notifications';

  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <header className="site-header">
        <div className="header-inner">
          <Link to="/" className="brand" aria-label="AmarKatha home">
            <span className="brand-mark" aria-hidden="true">
              अ
            </span>
            <span className="brand-text">AmarKatha</span>
          </Link>
          {auth?.authenticated && auth.demoMode ? (
            <a href="/admin/profile" className="demo-badge">
              Demo data
            </a>
          ) : null}

          <nav className="site-nav" aria-label="Main">
            <Link
              to="/"
              className={`nav-link${isHome ? ' active' : ''}`}
              aria-current={isHome ? 'page' : undefined}
            >
              Stories
            </Link>
            {auth?.authenticated && showProfile && (
              <Link
                to="/read/profile"
                className={`nav-link${isProfile ? ' active' : ''}`}
                aria-current={isProfile ? 'page' : undefined}
              >
                Profile
              </Link>
            )}
            <a href="/creator/signup" className="nav-link">
              For creators
            </a>
          </nav>

          <div className="auth-actions">
            {auth?.authenticated ? (
              <>
                {showNotifications ? (
                  <NotificationBell
                    unreadCount={unreadCount}
                    label={badgeLabel}
                    className="notif-bell"
                  />
                ) : null}
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
                {showProfile ? (
                  <Link
                    to="/read/profile"
                    className="auth-email"
                    title={auth.email}
                    aria-current={isProfile ? 'page' : undefined}
                  >
                    {auth.displayName || auth.email}
                  </Link>
                ) : (
                  <span className="auth-email" title={auth.email}>
                    {auth.displayName || auth.email}
                  </span>
                )}
                <button type="button" className="btn-signin btn-signout" onClick={handleLogout}>
                  Sign out
                </button>
              </>
            ) : (
              <a
                href="/login/reader"
                className="btn-signin btn-signin-quiet"
                onClick={() => trackSignupClick()}
              >
                Sign in
              </a>
            )}
          </div>

          <div className="header-mobile-actions">
            {auth?.authenticated && showNotifications ? (
              <NotificationBell
                unreadCount={unreadCount}
                label={badgeLabel}
                className="notif-bell notif-bell--mobile"
              />
            ) : null}
            <button
              ref={menuToggleRef}
              type="button"
              className="nav-menu-toggle"
              aria-expanded={menuOpen}
              aria-controls={menuId}
              onClick={() => setMenuOpen((open) => !open)}
            >
              <span className="visually-hidden">{menuOpen ? 'Close menu' : 'Open menu'}</span>
              <span aria-hidden="true">{menuOpen ? '✕' : '☰'}</span>
            </button>
          </div>
        </div>

        {menuOpen ? (
          <div
            ref={mobileNavRef}
            id={menuId}
            className="site-nav-mobile"
            role="navigation"
            aria-label="Mobile"
          >            <Link
              to="/"
              className={`nav-link${isHome ? ' active' : ''}`}
              aria-current={isHome ? 'page' : undefined}
            >
              Stories
            </Link>
            {auth?.authenticated && showProfile ? (
              <Link
                to="/read/profile"
                className={`nav-link${isProfile ? ' active' : ''}`}
                aria-current={isProfile ? 'page' : undefined}
              >
                Profile
              </Link>
            ) : null}
            {auth?.authenticated && showNotifications ? (
              <Link to="/read/profile#notifications" className="nav-link">
                Notifications
                {unreadCount > 0 ? (
                  <span className="notif-mobile-count">
                    {unreadCount > 99 ? '99+' : unreadCount}
                  </span>
                ) : null}
              </Link>
            ) : null}
            <a href="/creator/signup" className="nav-link">
              For creators
            </a>
            {auth?.authenticated ? (
              <>
                {showProfile ? (
                  <Link to="/read/profile" className="nav-link auth-email-mobile">
                    {auth.displayName || auth.email}
                  </Link>
                ) : (
                  <span className="nav-link auth-email-mobile">
                    {auth.displayName || auth.email}
                  </span>
                )}
                <button type="button" className="btn-signin btn-signout" onClick={handleLogout}>
                  Sign out
                </button>
              </>
            ) : (
              <a
                href="/login/reader"
                className="btn-signin btn-signin-quiet"
                onClick={() => trackSignupClick()}
              >
                Sign in
              </a>
            )}
          </div>
        ) : null}
      </header>

      <main id="main-content">{children}</main>

      <footer className="site-footer">
        <p className="footer-tagline">Indian indie comics · flexible schedules · share a link</p>
        <nav className="footer-links" aria-label="Legal">
          <a href="/legal/terms">Terms</a>
          <a href="/legal/privacy">Privacy</a>
          <a href="/legal/grievance">Grievance</a>
        </nav>
      </footer>
      <FeedbackFab />
    </div>
  );
}

function NotificationBell({
  unreadCount,
  label,
  className,
}: {
  unreadCount: number;
  label: string;
  className?: string;
}) {
  const badgeText = unreadCount > 99 ? '99+' : String(unreadCount);

  return (
    <Link
      to="/read/profile#notifications"
      className={className}
      aria-label={label}
      title={label}
    >
      <span className="notif-bell-icon" aria-hidden="true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M12 3a5.5 5.5 0 0 0-5.5 5.5v2.2c0 .7-.2 1.4-.55 2L4.7 15.1c-.55.85.05 2 1.05 2h12.5c1 0 1.6-1.15 1.05-2l-1.25-2.4c-.35-.6-.55-1.3-.55-2V8.5A5.5 5.5 0 0 0 12 3Z"
            stroke="currentColor"
            strokeWidth="1.75"
            strokeLinejoin="round"
          />
          <path
            d="M9.5 19a2.5 2.5 0 0 0 5 0"
            stroke="currentColor"
            strokeWidth="1.75"
            strokeLinecap="round"
          />
        </svg>
      </span>
      {unreadCount > 0 ? (
        <span className="notif-bell-badge" aria-hidden="true">
          {badgeText}
        </span>
      ) : null}
    </Link>
  );
}
