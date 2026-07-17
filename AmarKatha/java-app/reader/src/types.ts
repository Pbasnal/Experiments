export interface ScheduleStrip {
  headline: string;
  scheduleLabel: string;
  nextExpectedAt: string | null;
  skipMessage: string | null;
  status: 'ONGOING' | 'HIATUS' | 'COMPLETED';
  cadence: string | null;
  periodDays: number | null;
  releaseHourIst?: number | null;
}

export interface SeriesCard {
  slug: string;
  title: string;
  creatorName: string;
  description: string;
  genres: string[];
  contentLanguage: string;
  coverGradient: string;
  coverUrl?: string | null;
  scheduleLabel: string;
  schedule?: ScheduleStrip;
  status: 'ONGOING' | 'HIATUS' | 'COMPLETED';
  lastUpdatedAt: string;
  chapterCount: number;
}

export interface ChapterSummary {
  slug: string;
  title: string;
  chapterNumber: number;
  listedAt: string | null;
}

export interface SeriesDetail extends SeriesCard {
  chapters: ChapterSummary[];
}

export interface ChapterPage {
  sortOrder: number;
  imageUrl: string;
  width: number | null;
  height: number | null;
}

export interface ChapterReader {
  seriesSlug: string;
  seriesTitle: string;
  chapterSlug: string;
  title: string;
  chapterNumber: number;
  pages: ChapterPage[];
}

export interface PlatformRoute {
  label: string;
  path: string;
  area: string;
  status: 'preview' | 'planned' | 'live';
  description: string;
}

export interface HomeResponse {
  tagline: string;
  recentlyUpdated: SeriesCard[];
  platformRoutes: PlatformRoute[];
}
