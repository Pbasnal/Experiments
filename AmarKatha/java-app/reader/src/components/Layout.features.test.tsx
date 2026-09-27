import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Layout from '../components/Layout';
import { renderWithProviders } from '../test/render';

vi.mock('../api/auth', () => ({
  fetchMe: vi.fn().mockResolvedValue({
    authenticated: true,
    email: 'r@example.com',
    displayName: 'Reader',
    role: 'READER',
  }),
  logout: vi.fn(),
}));

vi.mock('../api/engagement', async () => {
  const actual = await vi.importActual<typeof import('../api/engagement')>('../api/engagement');
  return {
    ...actual,
    fetchUnreadCount: vi.fn().mockResolvedValue({ unreadCount: 3 }),
  };
});

vi.mock('../api/analytics', () => ({
  trackSignupClick: vi.fn(),
}));

describe('feature flag UI gating', () => {
  it('hides profile and notification bell when flags are off', async () => {
    renderWithProviders(<Layout><div>child</div></Layout>, {
      features: {
        profileProgress: false,
        inAppNotifications: false,
        follows: false,
      },
    });

    expect(await screen.findByText('Reader')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /^profile$/i })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/notification/i)).not.toBeInTheDocument();
  });

  it('hides personalized chrome while features are loading', async () => {
    renderWithProviders(<Layout><div>child</div></Layout>, {
      features: {
        profileProgress: true,
        inAppNotifications: true,
      },
      featuresLoading: true,
    });

    expect(await screen.findByText('Reader')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /^profile$/i })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/notification/i)).not.toBeInTheDocument();
  });

  it('shows notification bell when authenticated and flags allow', async () => {
    renderWithProviders(<Layout><div>child</div></Layout>);
    const bells = await screen.findAllByLabelText(/3 unread notifications/i);
    expect(bells.length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole('link', { name: /^profile$/i })).toBeInTheDocument();
  });

  it('closes mobile nav on Escape and returns focus to the toggle', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Layout><div>child</div></Layout>);
    const toggle = await screen.findByRole('button', { name: /open menu/i });
    await user.click(toggle);
    expect(screen.getByRole('navigation', { name: /mobile/i })).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('navigation', { name: /mobile/i })).not.toBeInTheDocument();
    expect(toggle).toHaveFocus();
  });
});
