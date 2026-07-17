import type { ChapterReader, HomeResponse, SeriesDetail } from '../types';

export async function fetchHome(): Promise<HomeResponse> {
  const res = await fetch('/api/reader/v1/home');
  if (!res.ok) {
    throw new Error(`Failed to load homepage (${res.status})`);
  }
  return res.json();
}

async function readApiError(res: Response, fallback: string): Promise<string> {
  try {
    const body = (await res.json()) as { message?: string; detail?: string };
    if (body.message && body.message.trim()) {
      return body.message;
    }
    if (body.detail && body.detail.trim()) {
      return body.detail;
    }
  } catch {
    // ignore non-JSON error bodies
  }
  return fallback;
}

export async function fetchSeries(slug: string): Promise<SeriesDetail> {
  const res = await fetch(`/api/reader/v1/series/${encodeURIComponent(slug)}`);
  if (res.status === 404 || res.status === 503) {
    throw new Error(await readApiError(res, 'Series not found'));
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to load series (${res.status})`));
  }
  return res.json();
}

export async function fetchChapter(
  seriesSlug: string,
  chapterSlug: string,
): Promise<ChapterReader> {
  const res = await fetch(
    `/api/reader/v1/series/${encodeURIComponent(seriesSlug)}/chapters/${encodeURIComponent(chapterSlug)}`,
  );
  if (res.status === 404 || res.status === 503) {
    throw new Error(await readApiError(res, 'Chapter not found'));
  }
  if (!res.ok) {
    throw new Error(await readApiError(res, `Failed to load chapter (${res.status})`));
  }
  return res.json();
}

export type AnalyticsReferrer = 'share' | 'homepage' | 'direct' | 'external';

export function classifyReferrer(): AnalyticsReferrer {
  const params = new URLSearchParams(window.location.search);
  const ref = params.get('ref');
  if (ref === 'share' || ref === 'ig' || ref === 'wa') {
    return 'share';
  }
  const docRef = document.referrer;
  if (!docRef) {
    return 'direct';
  }
  try {
    const url = new URL(docRef);
    if (url.origin === window.location.origin) {
      if (url.pathname === '/' || url.pathname === '') {
        return 'homepage';
      }
      return 'direct';
    }
    return 'external';
  } catch {
    return 'external';
  }
}

function onceKey(key: string): boolean {
  try {
    if (sessionStorage.getItem(key)) {
      return false;
    }
    sessionStorage.setItem(key, '1');
    return true;
  } catch {
    return true;
  }
}

export async function trackSeriesView(seriesSlug: string): Promise<void> {
  if (!onceKey(`ak:series-view:${seriesSlug}`)) {
    return;
  }
  try {
    await fetch('/api/reader/v1/events/series-view', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ seriesSlug, referrer: classifyReferrer() }),
    });
  } catch {
    // best-effort analytics
  }
}

export async function trackChapterView(seriesSlug: string, chapterSlug: string): Promise<void> {
  if (!onceKey(`ak:chapter-view:${seriesSlug}:${chapterSlug}`)) {
    return;
  }
  try {
    await fetch('/api/reader/v1/events/chapter-view', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ seriesSlug, chapterSlug, referrer: classifyReferrer() }),
    });
  } catch {
    // best-effort analytics
  }
}
