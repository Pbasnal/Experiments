import { type ReactElement, type ReactNode } from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { type ReaderFeatures } from '../api/features';
import { FeatureContext } from '../features/FeatureContext';

/** Typical post-fetch flags for component tests (not the fail-closed loading defaults). */
export const TEST_ENABLED_FEATURES: ReaderFeatures = {
  landingDiscovery: true,
  profileProgress: true,
  follows: true,
  inAppNotifications: true,
  emailNotifications: false,
};

export function renderWithProviders(
  ui: ReactElement,
  options?: {
    route?: string;
    features?: Partial<ReaderFeatures>;
    featuresLoading?: boolean;
  },
) {
  const features = { ...TEST_ENABLED_FEATURES, ...options?.features };
  const loading = options?.featuresLoading ?? false;
  const route = options?.route ?? '/';

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <MemoryRouter initialEntries={[route]}>
        <FeatureContext.Provider value={{ features, loading }}>
          {children}
        </FeatureContext.Provider>
      </MemoryRouter>
    );
  }

  return render(ui, { wrapper: Wrapper });
}
