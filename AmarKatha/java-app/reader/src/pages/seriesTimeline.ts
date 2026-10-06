import type { ChapterSummary, Glimpse } from '../types';

export type TimelineItem =
  | { kind: 'chapter'; at: string | null; chapter: ChapterSummary }
  | { kind: 'glimpse'; at: string; glimpse: Glimpse };

/** Chapters by listed time and glimpses by posted time, earliest first. */
export function mergeSeriesTimeline(
  chapters: ChapterSummary[],
  glimpses: Glimpse[],
): TimelineItem[] {
  const items: TimelineItem[] = [
    ...chapters.map((chapter) => ({ kind: 'chapter' as const, at: chapter.listedAt, chapter })),
    ...glimpses.map((glimpse) => ({ kind: 'glimpse' as const, at: glimpse.postedAt, glimpse })),
  ];
  return items.sort((a, b) => timeOf(a.at) - timeOf(b.at));
}

export type TimelineGroup =
  | { kind: 'chapters'; chapters: ChapterSummary[] }
  | { kind: 'glimpse'; glimpse: Glimpse };

/** Runs of consecutive chapters share one card; each glimpse gets its own. */
export function groupTimeline(items: TimelineItem[]): TimelineGroup[] {
  const groups: TimelineGroup[] = [];
  for (const item of items) {
    if (item.kind === 'glimpse') {
      groups.push({ kind: 'glimpse', glimpse: item.glimpse });
      continue;
    }
    const last = groups[groups.length - 1];
    if (last && last.kind === 'chapters') {
      last.chapters.push(item.chapter);
    } else {
      groups.push({ kind: 'chapters', chapters: [item.chapter] });
    }
  }
  return groups;
}

function timeOf(iso: string | null): number {
  if (!iso) {
    return Number.POSITIVE_INFINITY;
  }
  const parsed = Date.parse(iso);
  return Number.isNaN(parsed) ? Number.POSITIVE_INFINITY : parsed;
}
