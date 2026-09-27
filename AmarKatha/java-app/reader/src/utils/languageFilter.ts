import type { LanguageOption, SeriesCard } from '../types';

export function matchesLanguage(series: SeriesCard, code: string): boolean {
  return series.contentLanguage.trim().toLowerCase() === code.trim().toLowerCase();
}

/**
 * Resolve `?language=` against backend-gated options.
 * Returns null when the strip should not show or the code is invalid.
 */
export function resolveLanguageFilter(
  raw: string | null,
  options: LanguageOption[] | undefined,
  filters: string[] | undefined,
): string | null {
  if (!raw) return null;
  const show = filters?.includes('LANGUAGE') && (options?.length ?? 0) > 0;
  if (!show || !options) return null;
  const hit = options.find((o) => o.code.toLowerCase() === raw.toLowerCase());
  return hit ? hit.code : null;
}

export function shouldShowLanguageStrip(
  filters: string[] | undefined,
  options: LanguageOption[] | undefined,
  landingDiscovery = true,
): boolean {
  if (!landingDiscovery) return false;
  return Boolean(filters?.includes('LANGUAGE') && (options?.length ?? 0) > 0);
}
