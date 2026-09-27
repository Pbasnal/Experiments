import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { trackNotificationClick } from '../api/analytics';
import { fetchMe, logout, type AuthMeResponse } from '../api/auth';
import {
  EngagementAuthError,
  fetchMyFollowing,
  fetchMyProgress,
  fetchNotificationCapabilities,
  fetchNotificationPreferences,
  fetchNotifications,
  fetchPortalSummary,
  isInternalAppHref,
  markAllNotificationsRead,
  markNotificationRead,
  markNotificationReadBestEffort,
  readerLoginWithReturn,
  updateNotificationPreferences,
  type FollowedSeries,
  type NotificationCapabilities,
  type NotificationPreferences,
  type ReaderNotification,
  type ReaderPortalSummary,
  type ReaderProgress,
} from '../api/engagement';
import CoverImage from '../components/CoverImage';
import { useFeatures } from '../features/FeatureContext';

type PortalState =
  | { status: 'loading' }
  | { status: 'disabled' }
  | { status: 'anonymous' }
  | { status: 'error'; message: string }
  | {
      status: 'ready';
      auth: AuthMeResponse & { authenticated: true };
      portal: ReaderPortalSummary;
      progress: ReaderProgress[];
      following: FollowedSeries[];
    };

type NotificationsState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | {
      status: 'ready';
      items: ReaderNotification[];
      unreadCount: number;
      preferences: NotificationPreferences;
      capabilities: NotificationCapabilities;
    };

export default function ProfilePage() {
  const location = useLocation();
  const { features, loading: featuresLoading } = useFeatures();
  const [state, setState] = useState<PortalState>({ status: 'loading' });
  const [notifications, setNotifications] = useState<NotificationsState>({ status: 'loading' });
  const [prefsSaving, setPrefsSaving] = useState(false);
  const [markingAll, setMarkingAll] = useState(false);
  const [markingId, setMarkingId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const profileEnabled = features.profileProgress;
  const followsEnabled = features.follows;
  const notificationsEnabled = features.inAppNotifications;
  const emailEnabled = features.emailNotifications;

  useEffect(() => {
    if (featuresLoading) {
      return;
    }
    if (!profileEnabled) {
      setState({ status: 'disabled' });
      return;
    }

    let cancelled = false;

    async function load() {
      setState({ status: 'loading' });
      const me = await fetchMe();
      if (cancelled) {
        return;
      }
      if (!me.authenticated) {
        setState({ status: 'anonymous' });
        return;
      }
      try {
        const portalPromise = fetchPortalSummary();
        const progressPromise = fetchMyProgress().catch(() => [] as ReaderProgress[]);
        const followingPromise = followsEnabled
          ? fetchMyFollowing().catch(() => [] as FollowedSeries[])
          : Promise.resolve([] as FollowedSeries[]);
        const [portal, progress, following] = await Promise.all([
          portalPromise,
          progressPromise,
          followingPromise,
        ]);
        if (!cancelled) {
          setState({ status: 'ready', auth: me, portal, progress, following });
        }
      } catch (e) {
        if (cancelled) {
          return;
        }
        if (e instanceof EngagementAuthError) {
          setState({ status: 'anonymous' });
          return;
        }
        setState({
          status: 'error',
          message: e instanceof Error ? e.message : 'Could not load your profile',
        });
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [featuresLoading, profileEnabled, followsEnabled]);

  useEffect(() => {
    if (state.status !== 'ready' || !notificationsEnabled) {
      return;
    }
    let cancelled = false;

    async function loadNotifications() {
      setNotifications({ status: 'loading' });
      try {
        const [list, preferences, capabilities] = await Promise.all([
          fetchNotifications(),
          fetchNotificationPreferences(),
          fetchNotificationCapabilities(),
        ]);
        if (!cancelled) {
          setNotifications({
            status: 'ready',
            items: list.items,
            unreadCount: list.unreadCount,
            preferences,
            capabilities: {
              ...capabilities,
              emailAvailable: capabilities.emailAvailable && emailEnabled,
              inAppAvailable: capabilities.inAppAvailable && notificationsEnabled,
            },
          });
        }
      } catch (e) {
        if (cancelled) {
          return;
        }
        if (e instanceof EngagementAuthError) {
          setState({ status: 'anonymous' });
          return;
        }
        setNotifications({
          status: 'error',
          message: e instanceof Error ? e.message : 'Could not load notifications',
        });
      }
    }

    void loadNotifications();
    return () => {
      cancelled = true;
    };
  }, [state.status, notificationsEnabled, emailEnabled]);

  useEffect(() => {
    if (
      state.status !== 'ready' ||
      !notificationsEnabled ||
      location.hash !== '#notifications'
    ) {
      return;
    }
    const el = document.getElementById('notifications');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [state.status, location.hash, notifications.status, notificationsEnabled]);

  async function handleLogout() {
    await logout();
    window.location.href = '/';
  }

  async function handleMarkAllRead() {
    if (notifications.status !== 'ready' || notifications.unreadCount === 0) {
      return;
    }
    setActionError(null);
    setMarkingAll(true);
    try {
      await markAllNotificationsRead();
      setNotifications((prev) =>
        prev.status === 'ready'
          ? {
              ...prev,
              unreadCount: 0,
              items: prev.items.map((n) =>
                n.readAt ? n : { ...n, readAt: new Date().toISOString() },
              ),
            }
          : prev,
      );
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Could not mark all as read');
    } finally {
      setMarkingAll(false);
    }
  }

  async function handleMarkOneRead(id: string) {
    setActionError(null);
    setMarkingId(id);
    try {
      const result = await markNotificationRead(id);
      setNotifications((prev) => {
        if (prev.status !== 'ready') {
          return prev;
        }
        const wasUnread = prev.items.some((n) => n.id === id && !n.readAt);
        return {
          ...prev,
          unreadCount: wasUnread ? Math.max(0, prev.unreadCount - 1) : prev.unreadCount,
          items: prev.items.map((n) =>
            n.id === id ? { ...n, readAt: result.readAt } : n,
          ),
        };
      });
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Could not mark as read');
    } finally {
      setMarkingId(null);
    }
  }

  async function handlePreferenceToggle(
    key: 'emailNewChapter' | 'inAppNewChapter',
    value: boolean,
  ) {
    if (notifications.status !== 'ready') {
      return;
    }
    setActionError(null);
    setPrefsSaving(true);
    const previous = notifications.preferences;
    setNotifications({
      ...notifications,
      preferences: { ...previous, [key]: value },
    });
    try {
      const updated = await updateNotificationPreferences({ [key]: value });
      setNotifications((prev) =>
        prev.status === 'ready' ? { ...prev, preferences: updated } : prev,
      );
    } catch (e) {
      setNotifications((prev) =>
        prev.status === 'ready' ? { ...prev, preferences: previous } : prev,
      );
      setActionError(e instanceof Error ? e.message : 'Could not save preferences');
    } finally {
      setPrefsSaving(false);
    }
  }

  if (featuresLoading || state.status === 'loading') {
    return (
      <section className="profile-page" aria-busy="true" aria-live="polite">
        <p className="section-kicker">Your library</p>
        <h1>Profile</h1>
        <p className="subtitle muted">Loading your reading activity…</p>
      </section>
    );
  }

  if (state.status === 'disabled') {
    return (
      <section className="profile-page" aria-live="polite">
        <p className="section-kicker">Your library</p>
        <h1>Profile</h1>
        <p className="subtitle">
          Reader profiles are temporarily unavailable. You can still browse and read stories.
        </p>
        <div className="profile-actions">
          <Link to="/" className="btn btn-primary">
            Browse stories
          </Link>
        </div>
      </section>
    );
  }

  if (state.status === 'anonymous') {
    const returnTo = '/read/profile';
    return (
      <section className="profile-page profile-page--anon">
        <p className="section-kicker">Your library</p>
        <h1>Profile</h1>
        <p className="subtitle">
          Sign in to continue reading, manage follows, and keep progress across devices.
        </p>
        <div className="profile-actions">
          <a href={readerLoginWithReturn(returnTo)} className="btn btn-primary">
            Sign in with Google
          </a>
          <Link to="/" className="btn btn-secondary">
            Keep browsing
          </Link>
        </div>
      </section>
    );
  }

  if (state.status === 'error') {
    return (
      <section className="profile-page" role="alert">
        <p className="section-kicker">Your library</p>
        <h1>Profile</h1>
        <p className="subtitle">{state.message}</p>
        <div className="profile-actions">
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => window.location.reload()}
          >
            Try again
          </button>
          <Link to="/" className="btn btn-secondary">
            Home
          </Link>
        </div>
      </section>
    );
  }

  const { auth, portal, progress, following } = state;
  const showEmailPrefs = emailEnabled;

  return (
    <div className="profile-page profile-page--portal">
      <header className="profile-hero">
        <p className="section-kicker">Your library</p>
        <h1>{portal.displayName?.trim() || 'Reader'}</h1>
        <p className="subtitle">
          {followsEnabled
            ? portal.followingCount === 1
              ? 'Following 1 series'
              : `Following ${portal.followingCount} series`
            : 'Your reading progress'}
        </p>
      </header>

      <section className="profile-section" aria-labelledby="continue-heading">
        <div className="profile-section-head">
          <h2 id="continue-heading">Continue reading</h2>
          <p>Pick up where you left off.</p>
        </div>
        {progress.length === 0 ? (
          <p className="profile-empty muted">
            No chapters yet.{' '}
            <Link to="/">Browse stories</Link> and your progress will show up here.
          </p>
        ) : (
          <ul className="profile-rail">
            {progress.map((item) => (
              <li key={`${item.seriesSlug}:${item.chapterSlug}`}>
                <ProgressCard item={item} />
              </li>
            ))}
          </ul>
        )}
      </section>

      {followsEnabled ? (
        <section className="profile-section" aria-labelledby="following-heading">
          <div className="profile-section-head">
            <h2 id="following-heading">Following</h2>
            <p>Series you follow for new chapters.</p>
          </div>
          {following.length === 0 ? (
            <p className="profile-empty muted">
              You’re not following anything yet. Open a series and tap Follow.
            </p>
          ) : (
            <ul className="profile-rail">
              {following.map((item) => (
                <li key={item.slug}>
                  <FollowingCard item={item} />
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : null}

      {notificationsEnabled ? (
        <section
          id="notifications"
          className="profile-section"
          aria-labelledby="notifications-heading"
          tabIndex={-1}
        >
          <div className="profile-section-head profile-section-head--row">
            <div>
              <h2 id="notifications-heading">Notifications</h2>
              <p>Chapter alerts when you’re following a series.</p>
            </div>
            {notifications.status === 'ready' && notifications.unreadCount > 0 ? (
              <button
                type="button"
                className="btn btn-secondary profile-notif-mark-all"
                onClick={() => void handleMarkAllRead()}
                disabled={markingAll}
                aria-busy={markingAll}
              >
                {markingAll ? 'Marking…' : 'Mark all read'}
              </button>
            ) : null}
          </div>

          {actionError ? (
            <p className="profile-notif-error" role="alert">
              {actionError}
            </p>
          ) : null}

          {notifications.status === 'loading' ? (
            <p className="profile-empty muted" aria-live="polite">
              Loading notifications…
            </p>
          ) : null}

          {notifications.status === 'error' ? (
            <p className="profile-empty" role="alert">
              {notifications.message}
            </p>
          ) : null}

          {notifications.status === 'ready' ? (
            <>
              {notifications.items.length === 0 ? (
                <p className="profile-empty muted">
                  No notifications yet. Follow a series to get alerts when new chapters publish.
                </p>
              ) : (
                <ul className="profile-notif-list">
                  {notifications.items.map((item) => (
                    <li key={item.id}>
                      <NotificationRow
                        item={item}
                        marking={markingId === item.id}
                        onMarkRead={() => void handleMarkOneRead(item.id)}
                      />
                    </li>
                  ))}
                </ul>
              )}

              <fieldset className="profile-notif-prefs" disabled={prefsSaving}>
                <legend>Alert preferences</legend>
                <PreferenceToggle
                  id="pref-in-app"
                  label="In-app alerts for new chapters"
                  checked={notifications.preferences.inAppNewChapter}
                  disabled={!notifications.capabilities.inAppAvailable}
                  onChange={(v) => void handlePreferenceToggle('inAppNewChapter', v)}
                />
                {showEmailPrefs ? (
                  <>
                    <PreferenceToggle
                      id="pref-email-chapter"
                      label="Email when a followed series publishes"
                      checked={notifications.preferences.emailNewChapter}
                      disabled={!notifications.capabilities.emailAvailable}
                      disabledReason={
                        notifications.capabilities.emailAvailable
                          ? undefined
                          : 'Email delivery isn’t available on this server yet.'
                      }
                      onChange={(v) => void handlePreferenceToggle('emailNewChapter', v)}
                    />
                  </>
                ) : null}
              </fieldset>
            </>
          ) : null}
        </section>
      ) : null}

      <section className="profile-section" aria-labelledby="account-heading">
        <div className="profile-section-head">
          <h2 id="account-heading">Account</h2>
          <p>Signed-in identity and shortcuts.</p>
        </div>
        <dl className="profile-dl">
          <div>
            <dt>Name</dt>
            <dd>{portal.displayName?.trim() || '—'}</dd>
          </div>
          <div>
            <dt>Email</dt>
            <dd>{portal.email}</dd>
          </div>
          <div>
            <dt>Role</dt>
            <dd>{portal.role}</dd>
          </div>
        </dl>
        <p className="muted profile-prefs-note">
          Public reading stays available without signing in.
        </p>
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
    </div>
  );
}

function NotificationRow({
  item,
  marking,
  onMarkRead,
}: {
  item: ReaderNotification;
  marking: boolean;
  onMarkRead: () => void;
}) {
  const unread = !item.readAt;
  const to = isInternalAppHref(item.href) ? item.href : '/';

  return (
    <article
      className={`profile-notif-item${unread ? ' profile-notif-item--unread' : ''}`}
      aria-label={unread ? `Unread: ${item.title}` : item.title}
    >
      <div className="profile-notif-item-main">
        <Link
          to={to}
          className="profile-notif-link"
          onClick={() => {
            trackNotificationClick(item.seriesSlug);
            if (unread) {
              markNotificationReadBestEffort(item.id);
            }
          }}
        >
          <span className="profile-notif-title">{item.title}</span>
          {item.message ? <span className="profile-notif-message">{item.message}</span> : null}
          <time className="muted" dateTime={item.createdAt}>
            {formatRelativeNotification(item.createdAt)}
          </time>
        </Link>
      </div>
      {unread ? (
        <button
          type="button"
          className="profile-notif-mark"
          onClick={onMarkRead}
          disabled={marking}
          aria-busy={marking}
          aria-label={`Mark “${item.title}” as read`}
        >
          {marking ? '…' : 'Mark read'}
        </button>
      ) : (
        <span className="profile-notif-read-label muted">Read</span>
      )}
    </article>
  );
}

function PreferenceToggle({
  id,
  label,
  checked,
  disabled,
  disabledReason,
  onChange,
}: {
  id: string;
  label: string;
  checked: boolean;
  disabled?: boolean;
  disabledReason?: string;
  onChange: (value: boolean) => void;
}) {
  const hintId = `${id}-hint`;
  return (
    <div className="profile-pref-row">
      <label
        htmlFor={id}
        className={
          disabled ? 'profile-pref-label profile-pref-label--disabled' : 'profile-pref-label'
        }
      >
        <input
          id={id}
          type="checkbox"
          className="profile-pref-checkbox"
          checked={checked}
          disabled={disabled}
          aria-describedby={disabled && disabledReason ? hintId : undefined}
          onChange={(e) => onChange(e.target.checked)}
        />
        <span>{label}</span>
      </label>
      {disabled && disabledReason ? (
        <p className="profile-pref-hint muted" id={hintId}>
          {disabledReason}
        </p>
      ) : null}
    </div>
  );
}

function ProgressCard({ item }: { item: ReaderProgress }) {
  const chapterLabel =
    item.chapterTitle?.trim() ||
    (item.chapterNumber != null
      ? `Chapter ${formatChapterNumber(item.chapterNumber)}`
      : 'Continue');

  return (
    <Link
      to={`/read/s/${item.seriesSlug}/c/${item.chapterSlug}`}
      className="profile-card"
      aria-label={`Continue ${item.seriesTitle}: ${chapterLabel}`}
    >
      <CoverImage
        className="profile-card-cover"
        url={item.coverUrl}
        gradient={item.coverGradient || 'var(--ash)'}
      />
      <div className="profile-card-body">
        <h3>{item.seriesTitle}</h3>
        <p>{chapterLabel}</p>
        <time className="muted" dateTime={item.lastReadAt}>
          {formatRelative(item.lastReadAt)}
        </time>
      </div>
    </Link>
  );
}

function FollowingCard({ item }: { item: FollowedSeries }) {
  const href = item.latestChapterSlug
    ? `/read/s/${item.slug}/c/${item.latestChapterSlug}`
    : `/read/s/${item.slug}`;
  const chapterHint =
    item.latestChapterTitle?.trim() ||
    (item.latestChapterNumber != null
      ? `Ch. ${formatChapterNumber(item.latestChapterNumber)}`
      : item.scheduleLabel || 'Open series');

  return (
    <Link
      to={href}
      className="profile-card"
      aria-label={`${item.title}${item.hasUnread ? ', unread chapters' : ''}`}
    >
      <CoverImage
        className="profile-card-cover"
        url={item.coverUrl}
        gradient={item.coverGradient || 'var(--ash)'}
      >
        {item.hasUnread && <span className="profile-unread">New</span>}
      </CoverImage>
      <div className="profile-card-body">
        <h3>{item.title}</h3>
        <p>{chapterHint}</p>
        {item.scheduleLabel && <p className="muted">{item.scheduleLabel}</p>}
      </div>
    </Link>
  );
}

function formatChapterNumber(n: number): string {
  return Number.isInteger(n) ? String(n) : String(n);
}

function formatRelative(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const diff = Date.now() - date.getTime();
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  if (days <= 0) return 'Read today';
  if (days === 1) return 'Read yesterday';
  if (days < 14) return `Read ${days} days ago`;
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(date);
}

export function formatRelativeNotification(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const diff = Date.now() - date.getTime();
  const minutes = Math.floor(diff / (1000 * 60));
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 14) return `${days}d ago`;
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
  }).format(date);
}
