import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import FollowButton from '../components/FollowButton';
import { renderWithProviders } from '../test/render';

vi.mock('../api/auth', () => ({
  fetchMe: vi.fn(),
}));

vi.mock('../api/engagement', async () => {
  const actual = await vi.importActual<typeof import('../api/engagement')>('../api/engagement');
  return {
    ...actual,
    fetchFollowState: vi.fn(),
    fetchPendingFollow: vi.fn(),
    followSeries: vi.fn(),
    unfollowSeries: vi.fn(),
  };
});

import { fetchMe } from '../api/auth';
import {
  fetchFollowState,
  fetchPendingFollow,
  followSeries,
  unfollowSeries,
} from '../api/engagement';

describe('FollowButton', () => {
  beforeEach(() => {
    vi.mocked(fetchMe).mockReset();
    vi.mocked(fetchFollowState).mockReset();
    vi.mocked(fetchPendingFollow).mockReset();
    vi.mocked(followSeries).mockReset();
    vi.mocked(unfollowSeries).mockReset();
    vi.mocked(fetchPendingFollow).mockResolvedValue({ seriesSlug: null });
  });

  it('shows Follow for anonymous readers and routes to OAuth with intent', async () => {
    vi.mocked(fetchMe).mockResolvedValue({ authenticated: false });
    vi.mocked(fetchFollowState).mockResolvedValue({ seriesSlug: 'demo', followed: false });

    const locationStub = { href: '' };
    vi.stubGlobal('location', locationStub);

    renderWithProviders(<FollowButton seriesSlug="demo" />);

    const button = await screen.findByRole('button', { name: /sign in to follow/i });
    await userEvent.click(button);
    expect(locationStub.href).toContain('/login/reader');
    expect(locationStub.href).toContain('follow=demo');
  });

  it('toggles follow state when authenticated', async () => {
    vi.mocked(fetchMe).mockResolvedValue({
      authenticated: true,
      email: 'r@example.com',
      displayName: 'Reader',
      role: 'READER',
    });
    vi.mocked(fetchFollowState).mockResolvedValue({ seriesSlug: 'demo', followed: false });
    vi.mocked(followSeries).mockResolvedValue({ seriesSlug: 'demo', followed: true });

    renderWithProviders(<FollowButton seriesSlug="demo" />);

    const button = await screen.findByRole('button', { name: 'Follow this series' });
    await userEvent.click(button);
    await waitFor(() => {
      expect(followSeries).toHaveBeenCalledWith('demo');
    });
    expect(await screen.findByRole('button', { name: /unfollow this series/i })).toBeInTheDocument();
  });

  it('completes pending OAuth follow via authenticated POST when slug matches', async () => {
    vi.mocked(fetchMe).mockResolvedValue({
      authenticated: true,
      email: 'r@example.com',
      displayName: 'Reader',
      role: 'READER',
    });
    vi.mocked(fetchFollowState).mockResolvedValue({ seriesSlug: 'demo', followed: false });
    vi.mocked(fetchPendingFollow).mockResolvedValue({ seriesSlug: 'demo' });
    vi.mocked(followSeries).mockResolvedValue({ seriesSlug: 'demo', followed: true });

    renderWithProviders(<FollowButton seriesSlug="demo" />);

    expect(await screen.findByRole('button', { name: /unfollow this series/i })).toBeInTheDocument();
    expect(fetchPendingFollow).toHaveBeenCalled();
    expect(followSeries).toHaveBeenCalledWith('demo');
  });

  it('does not auto-follow when pending slug differs from the page series', async () => {
    vi.mocked(fetchMe).mockResolvedValue({
      authenticated: true,
      email: 'r@example.com',
      displayName: 'Reader',
      role: 'READER',
    });
    vi.mocked(fetchFollowState).mockResolvedValue({ seriesSlug: 'demo', followed: false });
    vi.mocked(fetchPendingFollow).mockResolvedValue({ seriesSlug: 'other-series' });

    renderWithProviders(<FollowButton seriesSlug="demo" />);

    expect(await screen.findByRole('button', { name: 'Follow this series' })).toBeInTheDocument();
    expect(followSeries).not.toHaveBeenCalled();
  });
});
