export type ReaderFeatures = {
  landingDiscovery: boolean;
  profileProgress: boolean;
  follows: boolean;
  inAppNotifications: boolean;
  emailNotifications: boolean;
};

/**
 * Fail-closed defaults for personalized/staged features when fetch fails or is loading.
 * Anonymous reading ({@code landingDiscovery}) stays available; home degrades safely.
 */
export const DEFAULT_FEATURES: ReaderFeatures = {
  landingDiscovery: true,
  profileProgress: false,
  follows: false,
  inAppNotifications: false,
  emailNotifications: false,
};

function asBool(value: unknown, fallback: boolean): boolean {
  return typeof value === 'boolean' ? value : fallback;
}

export function normalizeFeatures(raw: unknown): ReaderFeatures {
  if (!raw || typeof raw !== 'object') {
    return { ...DEFAULT_FEATURES };
  }
  const body = raw as Record<string, unknown>;
  return {
    landingDiscovery: asBool(body.landingDiscovery, DEFAULT_FEATURES.landingDiscovery),
    profileProgress: asBool(body.profileProgress, DEFAULT_FEATURES.profileProgress),
    follows: asBool(body.follows, DEFAULT_FEATURES.follows),
    inAppNotifications: asBool(
      body.inAppNotifications,
      DEFAULT_FEATURES.inAppNotifications,
    ),
    emailNotifications: asBool(
      body.emailNotifications,
      DEFAULT_FEATURES.emailNotifications,
    ),
  };
}

/**
 * Fetch staged rollout flags. On network/HTTP failure returns fail-closed defaults
 * so personalized controls stay hidden while anonymous reading remains available.
 */
export async function fetchFeatures(): Promise<ReaderFeatures> {
  try {
    const res = await fetch('/api/reader/v1/features', { credentials: 'same-origin' });
    if (!res.ok) {
      return { ...DEFAULT_FEATURES };
    }
    return normalizeFeatures(await res.json());
  } catch {
    return { ...DEFAULT_FEATURES };
  }
}
