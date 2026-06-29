import { useEffect, useState } from 'react';
import { fetchHome } from '../api/home';
import type { HomeResponse } from '../types';
import SeriesCardView from '../components/SeriesCard';
import PlatformMap from '../components/PlatformMap';

export default function HomePage() {
  const [data, setData] = useState<HomeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchHome()
      .then(setData)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <>
      <section className="hero">
        <div className="hero-inner">
          <span className="hero-eyebrow">V0 validation launch · UI preview</span>
          <h1>Where Indian stories come alive</h1>
          <p className="hero-tagline">
            {data?.tagline ?? 'Publish on your rhythm, share a link, readers know when you\'re back.'}
          </p>
          <div className="hero-actions">
            <a href="#catalog" className="btn btn-primary">
              Browse catalog
            </a>
            <a href="/creator" className="btn btn-secondary">
              Creator portal
            </a>
            <a href="#platform-map" className="btn btn-ghost">
              See all routes
            </a>
          </div>
        </div>
        <div className="hero-visual" aria-hidden="true">
          <div className="hero-panel p1" />
          <div className="hero-panel p2" />
          <div className="hero-panel p3" />
        </div>
      </section>

      <section className="value-props">
        <div className="value-card">
          <h3>For creators</h3>
          <p>Upload chapters, set weekly or biweekly cadence, skip a cycle or go on hiatus — then copy a share link.</p>
          <a href="/creator">Open creator home →</a>
        </div>
        <div className="value-card">
          <h3>For readers</h3>
          <p>Mobile-first vertical scroll, schedule strip on every series page, no account required in V0.</p>
          <a href="#catalog">Start reading →</a>
        </div>
        <div className="value-card">
          <h3>V0 scope</h3>
          <p>No trending, search, or comments yet. Chronological catalog + creator share links validate the wedge first.</p>
          <a href="#platform-map">Full route map →</a>
        </div>
      </section>

      <section className="catalog" id="catalog">
        <div className="section-header">
          <h2>Recently updated</h2>
          <p>Chronological feed — mock catalog for UI preview. Editor&apos;s Picks and trending deferred to V1.</p>
        </div>
        {loading && <p className="state-message">Loading catalog…</p>}
        {error && <p className="state-message error">{error}</p>}
        {data && (
          <div className="series-grid">
            {data.recentlyUpdated.map((series) => (
              <SeriesCardView key={series.slug} series={series} />
            ))}
          </div>
        )}
      </section>

      {data && <PlatformMap routes={data.platformRoutes} />}
    </>
  );
}
