import { describe, expect, it } from 'vitest';
import { normalizeFeatures, DEFAULT_FEATURES } from '../api/features';
import {
  oncePerSession,
  sanitizeExceptionMeta,
  stripPii,
} from '../api/analytics';
import { getFocusableElements } from '../utils/focusTrap';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CoverImage from '../components/CoverImage';
import HeroCarousel from '../components/HeroCarousel';
import type { SeriesCard } from '../types';

describe('feature normalize + defaults', () => {
  it('fails closed for personalized features on invalid payloads', () => {
    expect(normalizeFeatures(null)).toEqual(DEFAULT_FEATURES);
    expect(DEFAULT_FEATURES.landingDiscovery).toBe(true);
    expect(DEFAULT_FEATURES.profileProgress).toBe(false);
    expect(DEFAULT_FEATURES.follows).toBe(false);
    expect(DEFAULT_FEATURES.inAppNotifications).toBe(false);
    expect(normalizeFeatures({ landingDiscovery: false }).landingDiscovery).toBe(false);
    expect(normalizeFeatures({ emailNotifications: true }).emailNotifications).toBe(true);
    expect(normalizeFeatures({ profileProgress: true }).profileProgress).toBe(true);
  });
});

describe('analytics sanitization helpers', () => {
  it('redacts email-like text and truncates exception meta', () => {
    expect(stripPii('failed for user@example.com')).toContain('[redacted]');
    const meta = sanitizeExceptionMeta('TypeError', 'boom user@x.co and more');
    expect(meta?.name).toBe('TypeError');
    expect(meta?.message).toContain('[redacted]');
    expect(meta?.message).not.toContain('user@x.co');
  });

  it('redacts urls, bearer tokens, phones, and long identifiers', () => {
    const longId = 'b'.repeat(36);
    const cleaned = stripPii(
      `open https://evil.test/cb?code=1 Bearer abc.def phone +1 (555) 987-6543 id=${longId}`,
    );
    expect(cleaned).toContain('[redacted-url]');
    expect(cleaned).toContain('[redacted-secret]');
    expect(cleaned).toContain('[redacted-phone]');
    expect(cleaned).toContain('[redacted-id]');
    expect(cleaned).not.toContain('evil.test');
    expect(cleaned).not.toContain('abc.def');
    expect(cleaned).not.toContain(longId);
  });

  it('dedupes oncePerSession keys', () => {
    sessionStorage.clear();
    expect(oncePerSession('test-key')).toBe(true);
    expect(oncePerSession('test-key')).toBe(false);
  });
});

describe('focusTrap helpers', () => {
  it('lists focusable elements inside a container', () => {
    document.body.innerHTML = `
      <div id="root">
        <button>One</button>
        <a href="/x">Two</a>
        <button disabled>Skip</button>
        <div tabindex="-1">No</div>
      </div>
    `;
    const root = document.getElementById('root')!;
    const focusable = getFocusableElements(root);
    expect(focusable.map((el) => el.textContent)).toEqual(['One', 'Two']);
  });
});

describe('CoverImage', () => {
  it('renders a semantic img when url exists', () => {
    const { container } = render(
      <CoverImage url="/covers/demo.jpg" gradient="red" className="cover" />,
    );
    const img = container.querySelector('img.cover-image');
    expect(img).not.toBeNull();
    expect(img).toHaveAttribute('src', '/covers/demo.jpg');
    expect(img).toHaveAttribute('alt', '');
  });

  it('uses gradient fallback without an img when url is missing', () => {
    const { container } = render(
      <CoverImage url={null} gradient="linear-gradient(#000,#111)" className="cover" />,
    );
    expect(container.querySelector('img')).toBeNull();
    expect(container.firstChild).toHaveStyle({ background: 'linear-gradient(#000,#111)' });
  });
});

describe('HeroCarousel a11y', () => {
  it('uses carousel semantics without an incomplete tablist pattern', () => {
    const slides: SeriesCard[] = [
      {
        slug: 'one',
        title: 'One',
        description: '',
        coverGradient: 'red',
        coverUrl: null,
        creatorName: 'A',
        chapterCount: 1,
        contentLanguage: 'en',
        scheduleLabel: '',
        status: 'ONGOING',
        genres: [],
        lastUpdatedAt: new Date().toISOString(),
      },
      {
        slug: 'two',
        title: 'Two',
        description: '',
        coverGradient: 'blue',
        coverUrl: null,
        creatorName: 'B',
        chapterCount: 2,
        contentLanguage: 'hi',
        scheduleLabel: '',
        status: 'ONGOING',
        genres: [],
        lastUpdatedAt: new Date().toISOString(),
      },
    ];
    const { container } = render(
      <MemoryRouter>
        <HeroCarousel slides={slides} />
      </MemoryRouter>,
    );
    expect(container.querySelector('[role="tablist"]')).toBeNull();
    expect(container.querySelector('[role="tab"]')).toBeNull();
    expect(container.querySelector('[aria-roledescription="carousel"]')).not.toBeNull();
    expect(container.querySelector('[role="group"][aria-label="Carousel slides"]')).not.toBeNull();
  });
});
