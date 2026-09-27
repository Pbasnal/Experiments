import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import ProfilePage from '../pages/ProfilePage';
import { renderWithProviders } from '../test/render';

vi.mock('../api/auth', () => ({
  fetchMe: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../api/engagement', async () => {
  const actual = await vi.importActual<typeof import('../api/engagement')>('../api/engagement');
  return {
    ...actual,
    fetchPortalSummary: vi.fn(),
    fetchMyProgress: vi.fn(),
    fetchMyFollowing: vi.fn(),
    fetchNotifications: vi.fn(),
    fetchNotificationPreferences: vi.fn(),
    fetchNotificationCapabilities: vi.fn(),
  };
});

vi.mock('../api/analytics', () => ({
  trackNotificationClick: vi.fn(),
}));

import { fetchMe } from '../api/auth';
import {
  fetchMyFollowing,
  fetchMyProgress,
  fetchNotificationCapabilities,
  fetchNotificationPreferences,
  fetchNotifications,
  fetchPortalSummary,
} from '../api/engagement';

describe('ProfilePage states', () => {
  beforeEach(() => {
    vi.mocked(fetchMe).mockReset();
    vi.mocked(fetchPortalSummary).mockReset();
    vi.mocked(fetchMyProgress).mockReset();
    vi.mocked(fetchMyFollowing).mockReset();
    vi.mocked(fetchNotifications).mockReset();
    vi.mocked(fetchNotificationPreferences).mockReset();
    vi.mocked(fetchNotificationCapabilities).mockReset();
  });

  it('shows loading while features or portal load', () => {
    renderWithProviders(<ProfilePage />, { featuresLoading: true });
    expect(screen.getByText(/loading your reading activity/i)).toBeInTheDocument();
  });

  it('shows anonymous empty sign-in when not authenticated', async () => {
    vi.mocked(fetchMe).mockResolvedValue({ authenticated: false });
    renderWithProviders(<ProfilePage />);
    expect(await screen.findByRole('link', { name: /sign in with google/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /keep browsing/i })).toBeInTheDocument();
  });

  it('shows error state when portal fetch fails', async () => {
    vi.mocked(fetchMe).mockResolvedValue({
      authenticated: true,
      email: 'r@example.com',
      displayName: 'Reader',
      role: 'READER',
    });
    vi.mocked(fetchPortalSummary).mockRejectedValue(new Error('boom'));
    vi.mocked(fetchMyProgress).mockResolvedValue([]);
    vi.mocked(fetchMyFollowing).mockResolvedValue([]);

    renderWithProviders(<ProfilePage />);
    expect(await screen.findByRole('alert')).toHaveTextContent('boom');
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
  });

  it('shows empty progress and following when ready with no items', async () => {
    vi.mocked(fetchMe).mockResolvedValue({
      authenticated: true,
      email: 'r@example.com',
      displayName: 'Reader',
      role: 'READER',
    });
    vi.mocked(fetchPortalSummary).mockResolvedValue({
      displayName: 'Reader',
      email: 'r@example.com',
      role: 'READER',
      followingCount: 0,
      continueReading: [],
      following: [],
    });
    vi.mocked(fetchMyProgress).mockResolvedValue([]);
    vi.mocked(fetchMyFollowing).mockResolvedValue([]);
    vi.mocked(fetchNotifications).mockResolvedValue({ items: [], unreadCount: 0 });
    vi.mocked(fetchNotificationPreferences).mockResolvedValue({
      emailNewChapter: false,
      inAppNewChapter: true,
      emailProductUpdates: false,
      updatedAt: new Date().toISOString(),
    });
    vi.mocked(fetchNotificationCapabilities).mockResolvedValue({
      inAppAvailable: true,
      emailAvailable: false,
      pushAvailable: false,
    });

    renderWithProviders(<ProfilePage />);
    await waitFor(() => {
      expect(screen.getByText(/no chapters yet/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/not following anything yet/i)).toBeInTheDocument();
  });

  it('hides profile when profileProgress flag is off', async () => {
    renderWithProviders(<ProfilePage />, {
      features: { profileProgress: false },
    });
    expect(
      await screen.findByText(/reader profiles are temporarily unavailable/i),
    ).toBeInTheDocument();
    expect(fetchMe).not.toHaveBeenCalled();
  });
});
