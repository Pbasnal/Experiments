export interface SeriesCard {
  slug: string;
  title: string;
  creatorName: string;
  description: string;
  genres: string[];
  contentLanguage: string;
  coverGradient: string;
  scheduleLabel: string;
  status: 'ONGOING' | 'HIATUS' | 'COMPLETED';
  lastUpdatedAt: string;
  chapterCount: number;
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
