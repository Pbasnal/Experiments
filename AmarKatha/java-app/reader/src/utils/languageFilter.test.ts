import { describe, expect, it } from 'vitest';
import {
  matchesLanguage,
  resolveLanguageFilter,
  shouldShowLanguageStrip,
} from '../utils/languageFilter';
import type { LanguageOption, SeriesCard } from '../types';

const series = (lang: string): SeriesCard => ({
  slug: 'demo',
  title: 'Demo',
  creatorName: 'Creator',
  description: '',
  genres: [],
  contentLanguage: lang,
  coverGradient: 'linear-gradient(#000,#111)',
  scheduleLabel: 'Weekly',
  status: 'ONGOING',
  lastUpdatedAt: new Date().toISOString(),
  chapterCount: 2,
});

const options: LanguageOption[] = [
  { code: 'hi', label: 'Hindi', nativeLabel: 'हिन्दी', seriesCount: 2 },
  { code: 'ta', label: 'Tamil', nativeLabel: 'தமிழ்', seriesCount: 1 },
];

describe('languageFilter', () => {
  it('matches language case-insensitively', () => {
    expect(matchesLanguage(series('HI'), 'hi')).toBe(true);
    expect(matchesLanguage(series('ta'), 'hi')).toBe(false);
  });

  it('shows language strip only when LANGUAGE filter and options exist', () => {
    expect(shouldShowLanguageStrip(['LANGUAGE'], options)).toBe(true);
    expect(shouldShowLanguageStrip([], options)).toBe(false);
    expect(shouldShowLanguageStrip(['LANGUAGE'], [])).toBe(false);
    expect(shouldShowLanguageStrip(['LANGUAGE'], options, false)).toBe(false);
  });

  it('resolves valid language query params and rejects unknown codes', () => {
    expect(resolveLanguageFilter('hi', options, ['LANGUAGE'])).toBe('hi');
    expect(resolveLanguageFilter('HI', options, ['LANGUAGE'])).toBe('hi');
    expect(resolveLanguageFilter('bn', options, ['LANGUAGE'])).toBeNull();
    expect(resolveLanguageFilter('hi', options, [])).toBeNull();
  });
});
