import { describe, expect, it } from 'vitest';
import { isInternalAppHref, readerLoginWithFollowIntent, readerLoginWithReturn } from '../api/engagement';

describe('OAuth return intent helpers', () => {
  it('builds reader login with returnTo only', () => {
    expect(readerLoginWithReturn('/read/profile')).toBe(
      '/login/reader?returnTo=%2Fread%2Fprofile',
    );
  });

  it('builds follow intent with series slug and return path', () => {
    const url = readerLoginWithFollowIntent('demo-series', '/read/s/demo-series');
    const parsed = new URL(url, 'http://localhost');
    expect(parsed.pathname).toBe('/login/reader');
    expect(parsed.searchParams.get('returnTo')).toBe('/read/s/demo-series');
    expect(parsed.searchParams.get('follow')).toBe('demo-series');
  });

  it('defaults returnTo to the series hub when omitted', () => {
    const url = readerLoginWithFollowIntent('alpha');
    const parsed = new URL(url, 'http://localhost');
    expect(parsed.searchParams.get('returnTo')).toBe('/read/s/alpha');
    expect(parsed.searchParams.get('follow')).toBe('alpha');
  });
});

describe('isInternalAppHref', () => {
  it('accepts same-app paths and rejects protocol-relative URLs', () => {
    expect(isInternalAppHref('/read/s/demo')).toBe(true);
    expect(isInternalAppHref('/read/profile#notifications')).toBe(true);
    expect(isInternalAppHref('//evil.example/phish')).toBe(false);
    expect(isInternalAppHref('https://evil.example')).toBe(false);
    expect(isInternalAppHref(null)).toBe(false);
  });
});
