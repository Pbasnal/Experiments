import type { ChapterReader, HomeResponse, SeriesDetail } from '../types';

export async function fetchHome(): Promise<HomeResponse> {
  const res = await fetch('/api/reader/v1/home');
  if (!res.ok) {
    throw new Error(`Failed to load homepage (${res.status})`);
  }
  return res.json();
}

export async function fetchSeries(slug: string): Promise<SeriesDetail> {
  const res = await fetch(`/api/reader/v1/series/${encodeURIComponent(slug)}`);
  if (res.status === 404) {
    throw new Error('Series not found');
  }
  if (!res.ok) {
    throw new Error(`Failed to load series (${res.status})`);
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
  if (res.status === 404) {
    throw new Error('Chapter not found');
  }
  if (!res.ok) {
    throw new Error(`Failed to load chapter (${res.status})`);
  }
  return res.json();
}
