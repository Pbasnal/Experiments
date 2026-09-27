import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import {
  DEFAULT_FEATURES,
  fetchFeatures,
  type ReaderFeatures,
} from '../api/features';

export type FeatureContextValue = {
  features: ReaderFeatures;
  /** True until the first features fetch settles (success or fallback). */
  loading: boolean;
};

export const FeatureContext = createContext<FeatureContextValue>({
  features: DEFAULT_FEATURES,
  loading: true,
});

export function FeatureProvider({ children }: { children: ReactNode }) {
  const [features, setFeatures] = useState<ReaderFeatures>(DEFAULT_FEATURES);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    fetchFeatures().then((next) => {
      if (!cancelled) {
        setFeatures(next);
        setLoading(false);
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <FeatureContext.Provider value={{ features, loading }}>
      {children}
    </FeatureContext.Provider>
  );
}

export function useFeatures(): FeatureContextValue {
  return useContext(FeatureContext);
}
