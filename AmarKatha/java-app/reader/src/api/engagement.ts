/** Typed client for EngagementApiController (`/api/reader/v1`). */

export type ReaderProgress = {
  seriesSlug: string;
  seriesTitle: string;
  coverGradient: string;
  coverUrl: string | null;
  chapterSlug: string;
  chapterTitle: string;
  chapterNumber: number | null;
  /** ISO-8601 Instant from Jackson */
  lastReadAt: string;
};

export type FollowedSeries = {
  slug: string;
  title: string;
  coverGradient: string;
  coverUrl: string | null;
  scheduleLabel: string;
  status: string;
  latestChapterSlug: string | null;
  latestChapterTitle: string | null;
  latestChapterNumber: number | null;
  lastReadChapterSlug: string | null;
  hasUnread: boolean;
  /** ISO-8601 Instant from Jackson */
  followedAt: string;
};

export type FollowState = {
  seriesSlug: string;
  followed: boolean;
};

export type ReaderPortalSummary = {
  displayName: string;
  email: string;
  role: string;
  followingCount: number;
  continueReading: ReaderProgress[];
  following: FollowedSeries[];
};

export type ProgressUpdateRequest = {
  seriesSlug: string;
  chapterSlug: string;
};

export type ReaderNotification = {
  id: string;
  type: string;
  seriesSlug: string | null;
  seriesTitle: string | null;
  chapterSlug: string | null;
  chapterTitle: string | null;
  title: string;
  message: string;
  href: string;
  /** ISO-8601 Instant, null when unread */
  readAt: string | null;
  /** ISO-8601 Instant */
  createdAt: string;
};

export type NotificationListResponse = {
  items: ReaderNotification[];
  unreadCount: number;
};

export type UnreadCount = {
  unreadCount: number;
};

export type MarkReadResponse = {
  id: string;
  readAt: string;
};

export type ReadAllResponse = {
  markedRead: number;
};

export type NotificationPreferences = {
  emailNewChapter: boolean;
  inAppNewChapter: boolean;
  emailProductUpdates: boolean;
  /** ISO-8601 Instant */
  updatedAt: string;
};

export type NotificationPreferenceUpdate = {
  emailNewChapter?: boolean;
  inAppNewChapter?: boolean;
  emailProductUpdates?: boolean;
};

export type NotificationCapabilities = {
  inAppAvailable: boolean;
  emailAvailable: boolean;
  pushAvailable: boolean;
};

/** Dispatched after mark-read so Layout can refresh the badge. */
export const NOTIFICATIONS_CHANGED_EVENT = 'amarkatha:notifications-changed';

export function notifyNotificationsChanged(): void {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED_EVENT));
  }
}

async function readApiError(res: Response, fallback: string): Promise<string> {
  try {
    const body = (await res.json()) as { message?: string; detail?: string };
    if (body.message?.trim()) {
      return body.message;
    }
    if (body.detail?.trim()) {
      return body.detail;
    }
  } catch {
    // ignore non-JSON error bodies
  }
  return fallback;
}

export async function fetchPortalSummary(): Promise<ReaderPortalSummary> {
  const res = await fetch('/api/reader/v1/me', { credentials: 'same-origin' });
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to load profile (${res.status})`));
  }
  return res.json();
}

export async function fetchMyProgress(): Promise<ReaderProgress[]> {
  const res = await fetch('/api/reader/v1/me/progress', { credentials: 'same-origin' });
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to load progress (${res.status})`));
  }
  return res.json();
}

export async function fetchReadTarget(seriesSlug: string): Promise<string> {
  const fallback = `/read/s/${seriesSlug}`;
  try {
    const res = await fetch(
      `/api/reader/v1/series/${encodeURIComponent(seriesSlug)}/read-target`,
      { credentials: 'same-origin' },
    );
    if (!res.ok) {
      return fallback;
    }
    const body = (await res.json()) as { href?: unknown };
    return typeof body.href === 'string' && body.href.startsWith('/read/s/')
      ? body.href
      : fallback;
  } catch {
    return fallback;
  }
}

export async function fetchMyFollowing(): Promise<FollowedSeries[]> {
  const res = await fetch('/api/reader/v1/me/following', { credentials: 'same-origin' });
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to load following (${res.status})`));
  }
  return res.json();
}

export async function fetchFollowState(seriesSlug: string): Promise<FollowState> {
  const res = await fetch(
    `/api/reader/v1/series/${encodeURIComponent(seriesSlug)}/follow`,
    { credentials: 'same-origin' },
  );
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to load follow state (${res.status})`));
  }
  return res.json();
}

export async function followSeries(seriesSlug: string): Promise<FollowState> {
  const res = await fetch(
    `/api/reader/v1/series/${encodeURIComponent(seriesSlug)}/follow`,
    { method: 'POST', credentials: 'same-origin' },
  );
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to follow (${res.status})`));
  }
  return res.json();
}

export async function unfollowSeries(seriesSlug: string): Promise<FollowState> {
  const res = await fetch(
    `/api/reader/v1/series/${encodeURIComponent(seriesSlug)}/follow`,
    { method: 'DELETE', credentials: 'same-origin' },
  );
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to unfollow (${res.status})`));
  }
  return res.json();
}

/** Latched false after 401/403 so anonymous public reads don't keep posting. */
let progressAuthOk: boolean | null = null;

/**
 * Best-effort progress upsert when authenticated.
 * Silent on network/auth failures so public reading stays uninterrupted.
 */
export async function postProgress(seriesSlug: string, chapterSlug: string): Promise<void> {
  if (progressAuthOk === false) {
    return;
  }
  try {
    const body: ProgressUpdateRequest = { seriesSlug, chapterSlug };
    const res = await fetch('/api/reader/v1/me/progress', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify(body),
    });
    if (res.status === 401 || res.status === 403) {
      progressAuthOk = false;
      return;
    }
    if (res.ok) {
      progressAuthOk = true;
    }
  } catch {
    // best-effort
  }
}

export async function fetchNotifications(limit?: number): Promise<NotificationListResponse> {
  const params = new URLSearchParams();
  if (limit != null && limit > 0) {
    params.set('limit', String(limit));
  }
  const qs = params.toString();
  const res = await fetch(`/api/reader/v1/me/notifications${qs ? `?${qs}` : ''}`, {
    credentials: 'same-origin',
  });
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to load notifications (${res.status})`));
  }
  return res.json();
}

export async function fetchUnreadCount(): Promise<UnreadCount> {
  const res = await fetch('/api/reader/v1/me/notifications/unread-count', {
    credentials: 'same-origin',
  });
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to load unread count (${res.status})`));
  }
  return res.json();
}

export async function markNotificationRead(id: string): Promise<MarkReadResponse> {
  const res = await fetch(`/api/reader/v1/me/notifications/${encodeURIComponent(id)}/read`, {
    method: 'POST',
    credentials: 'same-origin',
  });
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to mark notification read (${res.status})`));
  }
  const body = (await res.json()) as MarkReadResponse;
  notifyNotificationsChanged();
  return body;
}

/**
 * Best-effort mark-read for notification href clicks.
 * Never throws; does not block navigation.
 */
export function markNotificationReadBestEffort(id: string): void {
  void (async () => {
    try {
      await markNotificationRead(id);
    } catch {
      // best-effort
    }
  })();
}

export async function markAllNotificationsRead(): Promise<ReadAllResponse> {
  const res = await fetch('/api/reader/v1/me/notifications/read-all', {
    method: 'POST',
    credentials: 'same-origin',
  });
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to mark all read (${res.status})`));
  }
  const body = (await res.json()) as ReadAllResponse;
  notifyNotificationsChanged();
  return body;
}

export async function fetchNotificationPreferences(): Promise<NotificationPreferences> {
  const res = await fetch('/api/reader/v1/me/notification-preferences', {
    credentials: 'same-origin',
  });
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to load notification preferences (${res.status})`));
  }
  return res.json();
}

export async function updateNotificationPreferences(
  body: NotificationPreferenceUpdate,
): Promise<NotificationPreferences> {
  const res = await fetch('/api/reader/v1/me/notification-preferences', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    body: JSON.stringify(body),
  });
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to update notification preferences (${res.status})`));
  }
  return res.json();
}

export async function fetchNotificationCapabilities(): Promise<NotificationCapabilities> {
  const res = await fetch('/api/reader/v1/me/notification-capabilities', {
    credentials: 'same-origin',
  });
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to load notification capabilities (${res.status})`));
  }
  return res.json();
}

/** Build reader OAuth URL that completes a pending follow after login. */
export function readerLoginWithFollowIntent(seriesSlug: string, returnTo?: string): string {
  const params = new URLSearchParams();
  params.set('returnTo', returnTo ?? `/read/s/${seriesSlug}`);
  params.set('follow', seriesSlug);
  return `/login/reader?${params.toString()}`;
}

export function readerLoginWithReturn(returnTo: string): string {
  const params = new URLSearchParams();
  params.set('returnTo', returnTo);
  return `/login/reader?${params.toString()}`;
}

export type PendingFollow = {
  seriesSlug: string | null;
};

/** Peek session pending-follow slug after OAuth (does not clear). */
export async function fetchPendingFollow(): Promise<PendingFollow> {
  const res = await fetch('/api/reader/v1/me/pending-follow', {
    credentials: 'same-origin',
  });
  if (res.status === 401 || res.status === 403) {
    throw new EngagementAuthError();
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to load pending follow (${res.status})`));
  }
  return res.json();
}

/**
 * True for same-app relative paths only. Rejects protocol-relative `//` URLs.
 */
export function isInternalAppHref(href: string | null | undefined): boolean {
  if (!href) {
    return false;
  }
  const trimmed = href.trim();
  return trimmed.startsWith('/') && !trimmed.startsWith('//');
}

export class EngagementAuthError extends Error {
  constructor(message = 'Authentication required') {
    super(message);
    this.name = 'EngagementAuthError';
  }
}
