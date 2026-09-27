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
  /** 0 means unset — hide the stars. */
  rating?: number;
  /** 0 means unset — hide the reader count. */
  readerCount?: number;
  editorsPick?: boolean;
}

export interface ChapterSummary {
  slug: string;
  title: string;
  chapterNumber: number;
  listedAt: string | null;
}

export interface SeriesDetail extends SeriesCard {
  chapters: ChapterSummary[];
  /** True when the signed-in viewer created this series. */
  viewerOwnsSeries?: boolean;
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

/** Anticipated home discovery option when LANGUAGE is in filters. */
export interface LanguageOption {
  code: string;
  label: string;
  nativeLabel: string;
  seriesCount: number;
}

export interface HomeResponse {
  tagline: string;
  recentlyUpdated: SeriesCard[];
  /** @deprecated Not shown on public landing; may be omitted. */
  platformRoutes?: unknown[];
  /**
   * Discovery filters the backend elected to show (e.g. `['LANGUAGE']`).
   * Absent or empty → no language strip (backward-safe).
   */
  filters?: string[];
  /**
   * Language chips when `filters` includes `LANGUAGE`.
   * Absent → treat as no options (backward-safe).
   */
  languageOptions?: LanguageOption[];
}
