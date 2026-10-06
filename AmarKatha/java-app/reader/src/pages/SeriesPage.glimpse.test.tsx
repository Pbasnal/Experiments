import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { FeatureContext } from '../features/FeatureContext';
import { TEST_ENABLED_FEATURES } from '../test/render';
import SeriesPage from './SeriesPage';

vi.mock('../api/home', () => ({
  fetchSeries: vi.fn(),
  trackSeriesView: vi.fn(),
}));

vi.mock('../components/FollowButton', () => ({
  default: () => null,
}));

vi.mock('../api/engagement', async () => {
  const actual = await vi.importActual<typeof import('../api/engagement')>('../api/engagement');
  return {
    ...actual,
    setGlimpseReaction: vi.fn(),
  };
});

import { fetchSeries } from '../api/home';
import { EngagementAuthError, setGlimpseReaction } from '../api/engagement';

describe('SeriesPage glimpses', () => {
  it('renders a glimpse strip between the surrounding chapters', async () => {
    vi.mocked(fetchSeries).mockResolvedValue({
      slug: 'monsoon-market',
      title: 'Monsoon Market',
      creatorName: 'Meera Iyer',
      description: 'A night market.',
      genres: [],
      contentLanguage: 'en',
      coverGradient: 'linear-gradient(#111,#222)',
      scheduleLabel: 'Every Friday',
      status: 'ONGOING',
      lastUpdatedAt: '2026-03-01T00:00:00Z',
      chapterCount: 2,
      chapters: [
        {
          slug: 'chapter-1',
          title: 'Chapter 1',
          chapterNumber: 1,
          listedAt: '2026-01-01T00:00:00Z',
        },
        {
          slug: 'chapter-2',
          title: 'Chapter 2',
          chapterNumber: 2,
          listedAt: '2026-03-01T00:00:00Z',
        },
      ],
      glimpses: [
        {
          id: 'g1',
          tag: 'CHARACTER',
          postedAt: '2026-02-01T00:00:00Z',
          images: [
            {
              id: 'img-1',
              url: '/demo/glimpses/monsoon-character-1.svg',
              sortOrder: 1,
              reactionCount: 18,
              reacted: false,
            },
          ],
        },
      ],
    });

    render(
      <MemoryRouter initialEntries={['/read/s/monsoon-market']}>
        <FeatureContext.Provider value={{ features: TEST_ENABLED_FEATURES, loading: false }}>
          <Routes>
            <Route path="/read/s/:seriesSlug" element={<SeriesPage />} />
          </Routes>
        </FeatureContext.Provider>
      </MemoryRouter>,
    );

    const chapterOne = await screen.findByRole('link', { name: /Chapter 1/ });
    const strip = screen.getByRole('button', { name: 'Open Character image 1 of 1' });
    const chapterTwo = screen.getByRole('link', { name: /Chapter 2/ });

    expect(screen.getAllByRole('list')).toHaveLength(2);
    expect(strip.closest('ul')).toBeNull();
    expect(chapterOne.compareDocumentPosition(strip) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(strip.compareDocumentPosition(chapterTwo) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(strip.querySelector('img')).toHaveAttribute('src', '/demo/glimpses/monsoon-character-1.svg');
    expect(strip.closest('.glimpse-strip')).not.toBeNull();

    await userEvent.click(strip);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Character glimpse 1 of 1' })).toHaveAttribute(
      'src',
      '/demo/glimpses/monsoon-character-1.svg',
    );
    await waitFor(() => expect(fetchSeries).toHaveBeenCalledWith('monsoon-market'));
  });

  it('asks an anonymous reader to log in before reacting', async () => {
    vi.mocked(setGlimpseReaction).mockRejectedValue(new EngagementAuthError());
    vi.mocked(fetchSeries).mockResolvedValue({
      slug: 'monsoon-market',
      title: 'Monsoon Market',
      creatorName: 'Meera Iyer',
      description: 'A night market.',
      genres: [],
      contentLanguage: 'en',
      coverGradient: 'linear-gradient(#111,#222)',
      scheduleLabel: 'Every Friday',
      status: 'ONGOING',
      lastUpdatedAt: '2026-03-01T00:00:00Z',
      chapterCount: 1,
      chapters: [
        {
          slug: 'chapter-1',
          title: 'Chapter 1',
          chapterNumber: 1,
          listedAt: '2026-01-01T00:00:00Z',
        },
      ],
      glimpses: [
        {
          id: 'g1',
          tag: 'CHARACTER',
          postedAt: '2026-02-01T00:00:00Z',
          images: [
            {
              id: 'img-1',
              url: '/demo/glimpses/monsoon-character-1.svg',
              sortOrder: 1,
              reactionCount: 18,
              reacted: false,
            },
          ],
        },
      ],
    });

    render(
      <MemoryRouter initialEntries={['/read/s/monsoon-market']}>
        <FeatureContext.Provider value={{ features: TEST_ENABLED_FEATURES, loading: false }}>
          <Routes>
            <Route path="/read/s/:seriesSlug" element={<SeriesPage />} />
          </Routes>
        </FeatureContext.Provider>
      </MemoryRouter>,
    );

    await userEvent.click(await screen.findByRole('button', { name: 'React' }));

    expect(screen.getByRole('dialog', { name: 'Log in to react' })).toBeInTheDocument();
    expect(screen.getByText('You need to log in to react to a glimpse.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Log in' })).toHaveAttribute(
      'href',
      expect.stringContaining('/login/reader?returnTo='),
    );
  });
});
