import { describe, expect, it } from 'vitest';
import { mergeSeriesTimeline } from './seriesTimeline';
import type { ChapterSummary, Glimpse } from '../types';

const chapter = (slug: string, listedAt: string): ChapterSummary => ({
  slug,
  title: slug,
  chapterNumber: slug.endsWith('1') ? 1 : 2,
  listedAt,
});

const glimpse = (id: string, postedAt: string): Glimpse => ({
  id,
  tag: 'CHARACTER',
  postedAt,
  images: [],
});

describe('mergeSeriesTimeline', () => {
  it('places a glimpse between the chapters around its posted time', () => {
    const items = mergeSeriesTimeline(
      [chapter('chapter-1', '2026-01-01T00:00:00Z'), chapter('chapter-2', '2026-03-01T00:00:00Z')],
      [glimpse('g1', '2026-02-01T00:00:00Z')],
    );

    expect(items.map((item) => item.kind)).toEqual(['chapter', 'glimpse', 'chapter']);
    expect(items[0].kind === 'chapter' && items[0].chapter.slug).toBe('chapter-1');
    expect(items[2].kind === 'chapter' && items[2].chapter.slug).toBe('chapter-2');
  });
});
