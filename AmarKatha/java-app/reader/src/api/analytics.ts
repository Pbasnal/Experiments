import { classifyReferrer, type AnalyticsReferrer } from './home';

/** Client-ingestible types from AnalyticsEventType.clientIngestible(). */
export type ClientEventType =
  | 'LANDING_VIEW'
  | 'SIGNUP_CLICK'
  | 'NOTIFICATION_CLICK'
  | 'WEB_VITAL_LCP'
  | 'WEB_VITAL_INP'
  | 'WEB_VITAL_CLS'
  | 'FRONTEND_EXCEPTION';

export type AnalyticsMeta = {
  value?: number;
  rating?: 'good' | 'needs-improvement' | 'poor';
  name?: string;
  message?: string;
  bulk?: boolean;
  count?: number;
};

export type TrackEventInput = {
  type: ClientEventType;
  seriesSlug?: string;
  chapterSlug?: string;
  referrer?: AnalyticsReferrer | 'app';
  meta?: AnalyticsMeta;
};

const EMAIL_LIKE = /[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}/g;
const URL_LIKE = /(?:https?:\/\/|www\.)\S+/gi;
const BEARER_OR_TOKEN =
  /(?:bearer\s+[A-Za-z0-9._\-+/=]+|(?:access_token|refresh_token|id_token|api[_-]?key|token|secret|password|authorization)\s*[:=]\s*[^\s"'&,;]+)/gi;
const PHONE_LIKE = /(?<!\w)(?:\+?\d[\d\s().-]{7,}\d)/g;
const LONG_IDENTIFIER = /\b[A-Za-z0-9_-]{32,}\b/g;

/** True in production browser builds; false in Vite DEV and Vitest. */
export function analyticsEnabled(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }
  if (import.meta.env.MODE === 'test' || import.meta.env.DEV) {
    return false;
  }
  return true;
}

export function oncePerSession(key: string): boolean {
  try {
    const storageKey = `ak:evt:${key}`;
    if (sessionStorage.getItem(storageKey)) {
      return false;
    }
    sessionStorage.setItem(storageKey, '1');
    return true;
  } catch {
    return true;
  }
}

export function stripPii(raw: string): string {
  return raw
    .replace(EMAIL_LIKE, '[redacted]')
    .replace(URL_LIKE, '[redacted-url]')
    .replace(BEARER_OR_TOKEN, '[redacted-secret]')
    .replace(PHONE_LIKE, '[redacted-phone]')
    .replace(LONG_IDENTIFIER, '[redacted-id]');
}

export function sanitizeExceptionMeta(
  name: unknown,
  message: unknown,
): AnalyticsMeta | null {
  const safeName = truncate(
    stripPii(String(name ?? 'Error')).replace(/[\r\n]+/g, ' ').trim(),
    64,
  );
  if (!safeName) {
    return null;
  }
  const meta: AnalyticsMeta = { name: safeName };
  if (message != null && String(message).trim()) {
    const safeMessage = truncate(
      stripPii(String(message)).replace(/[\r\n]+/g, ' ').trim(),
      200,
    );
    if (safeMessage) {
      meta.message = safeMessage;
    }
  }
  return meta;
}

function truncate(value: string, max: number): string {
  return value.length <= max ? value : value.slice(0, max);
}

/**
 * Best-effort, non-blocking product/RUM event.
 * Never throws; skipped in DEV/test to avoid noise.
 */
export function trackEvent(input: TrackEventInput): void {
  if (!analyticsEnabled()) {
    return;
  }
  const body = {
    type: input.type,
    seriesSlug: input.seriesSlug,
    chapterSlug: input.chapterSlug,
    referrer: input.referrer ?? classifyReferrer(),
    meta: input.meta,
  };
  try {
    void fetch('/api/reader/v1/events', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify(body),
      keepalive: true,
    }).catch(() => {
      // best-effort
    });
  } catch {
    // best-effort
  }
}

export function trackLandingView(): void {
  if (!oncePerSession('landing-view')) {
    return;
  }
  trackEvent({ type: 'LANDING_VIEW', referrer: 'homepage' });
}

export function trackSignupClick(referrer?: AnalyticsReferrer | 'app'): void {
  trackEvent({ type: 'SIGNUP_CLICK', referrer: referrer ?? classifyReferrer() });
}

export function trackNotificationClick(seriesSlug?: string | null): void {
  trackEvent({
    type: 'NOTIFICATION_CLICK',
    seriesSlug: seriesSlug ?? undefined,
    referrer: 'app',
  });
}

export function trackWebVital(
  name: 'LCP' | 'INP' | 'CLS',
  value: number,
  rating: 'good' | 'needs-improvement' | 'poor',
): void {
  const type: ClientEventType =
    name === 'LCP' ? 'WEB_VITAL_LCP' : name === 'INP' ? 'WEB_VITAL_INP' : 'WEB_VITAL_CLS';
  trackEvent({
    type,
    referrer: 'direct',
    meta: { value, rating },
  });
}

export function trackFrontendException(name: unknown, message: unknown): void {
  const meta = sanitizeExceptionMeta(name, message);
  if (!meta) {
    return;
  }
  if (!oncePerSession(`fe-ex:${meta.name}:${meta.message ?? ''}`)) {
    return;
  }
  trackEvent({ type: 'FRONTEND_EXCEPTION', referrer: 'direct', meta });
}
