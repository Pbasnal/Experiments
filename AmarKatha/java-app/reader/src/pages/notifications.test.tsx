import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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
    markNotificationRead: vi.fn(),
    markNotificationReadBestEffort: vi.fn(),
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
  markNotificationRead,
} from '../api/engagement';

async function readyProfile() {
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
  vi.mocked(fetchNotificationPreferences).mockResolvedValue({
    emailNewChapter: false,
    inAppNewChapter: true,
    emailProductUpdates: false,
    updatedAt: new Date().toISOString(),
  });
  vi.mocked(fetchNotificationCapabilities).mockResolvedValue({
    inAppAvailable: true,
    emailAvailable: true,
    pushAvailable: false,
  });
}

describe('notification unread behavior', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('marks a notification unread and decrements unread count', async () => {
    await readyProfile();
    vi.mocked(fetchNotifications).mockResolvedValue({
      unreadCount: 1,
      items: [
        {
          id: 'n1',
          type: 'CHAPTER',
          seriesSlug: 'demo',
          seriesTitle: 'Demo',
          chapterSlug: 'c1',
          chapterTitle: 'One',
          title: 'New chapter',
          message: 'Chapter 1 is live',
          href: '/read/s/demo/c/c1',
          readAt: null,
          createdAt: new Date().toISOString(),
        },
      ],
    });
    vi.mocked(markNotificationRead).mockResolvedValue({
      id: 'n1',
      readAt: new Date().toISOString(),
    });

    renderWithProviders(<ProfilePage />);

    expect(await screen.findByLabelText(/unread: new chapter/i)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /mark .* as read/i }));
    await waitFor(() => {
      expect(markNotificationRead).toHaveBeenCalledWith('n1');
    });
    expect(await screen.findByText(/^Read$/)).toBeInTheDocument();
  });

  it('hides notification section when in-app flag is off', async () => {
    await readyProfile();
    renderWithProviders(<ProfilePage />, {
      features: { inAppNotifications: false },
    });
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /account/i })).toBeInTheDocument();
    });
    expect(screen.queryByRole('heading', { name: /^notifications$/i })).not.toBeInTheDocument();
    expect(fetchNotifications).not.toHaveBeenCalled();
  });
});
