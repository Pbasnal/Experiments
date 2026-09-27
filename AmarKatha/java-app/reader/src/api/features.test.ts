import { describe, expect, it, vi, afterEach } from 'vitest';
import { DEFAULT_FEATURES, fetchFeatures, normalizeFeatures } from '../api/features';

describe('fetchFeatures fail-closed', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('returns fail-closed defaults when the request fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockRejectedValue(new Error('network down')),
    );
    await expect(fetchFeatures()).resolves.toEqual(DEFAULT_FEATURES);
    expect(DEFAULT_FEATURES.follows).toBe(false);
    expect(DEFAULT_FEATURES.landingDiscovery).toBe(true);
  });

  it('returns fail-closed defaults on non-OK HTTP', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 503 }),
    );
    await expect(fetchFeatures()).resolves.toEqual(DEFAULT_FEATURES);
  });

  it('accepts server payload when fetch succeeds', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          landingDiscovery: true,
          profileProgress: true,
          follows: true,
          inAppNotifications: true,
          emailNotifications: true,
        }),
      }),
    );
    const features = await fetchFeatures();
    expect(features).toEqual(
      normalizeFeatures({
        landingDiscovery: true,
        profileProgress: true,
        follows: true,
        inAppNotifications: true,
        emailNotifications: true,
      }),
    );
    expect(features.follows).toBe(true);
  });
});
