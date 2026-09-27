import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import HomePage from '../pages/HomePage';
import { renderWithProviders } from '../test/render';

vi.mock('../api/auth', () => ({
  fetchMe: vi.fn().mockResolvedValue({ authenticated: false }),
}));

vi.mock('../api/home', () => ({
  fetchHome: vi.fn(),
}));

vi.mock('../api/analytics', () => ({
  trackLandingView: vi.fn(),
  trackSignupClick: vi.fn(),
}));

import { fetchHome } from '../api/home';

const homePayload = {
  tagline: 'Tagline',
  recentlyUpdated: [
    {
      slug: 'demo',
      title: 'Demo Series',
      creatorName: 'Creator',
      description: 'Desc',
      genres: [],
      contentLanguage: 'hi',
      coverGradient: 'linear-gradient(#000,#111)',
      coverUrl: '/covers/demo.jpg',
      scheduleLabel: 'Weekly',
      status: 'ONGOING' as const,
      lastUpdatedAt: new Date().toISOString(),
      chapterCount: 3,
    },
  ],
  filters: ['LANGUAGE'],
  languageOptions: [
    { code: 'hi', label: 'Hindi', nativeLabel: 'हिन्दी', seriesCount: 1 },
  ],
};

describe('HomePage language visibility + discovery rollback', () => {
  beforeEach(() => {
    vi.mocked(fetchHome).mockReset();
    vi.mocked(fetchHome).mockResolvedValue(homePayload);
  });

  it('shows language strip when API includes LANGUAGE filter', async () => {
    renderWithProviders(<HomePage />);
    expect(await screen.findByLabelText(/filter by language/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /hindi/i })).toBeInTheDocument();
  });

  it('hides language strip and discovery hero when landingDiscovery is off', async () => {
    renderWithProviders(<HomePage />, {
      features: { landingDiscovery: false },
    });
    expect(await screen.findByRole('heading', { name: /start reading/i })).toBeInTheDocument();
    expect(screen.queryByLabelText(/filter by language/i)).not.toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText('Demo Series')).toBeInTheDocument();
    });
  });
});
