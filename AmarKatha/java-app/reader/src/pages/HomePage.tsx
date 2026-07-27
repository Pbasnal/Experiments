import { useEffect, useState } from 'react';
import { fetchHome } from '../api/home';
import type { HomeResponse } from '../types';
import SeriesCardView from '../components/SeriesCard';

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

  const tagline =
    data?.tagline ?? 'Publish on your rhythm. Share a link. Readers know when you’re back.';

  return (
    <>
      <section className="landing-hero">
        <div className="landing-hero-media" aria-hidden="true">
          <img
            className="landing-hero-art"
            src="/hero-atmosphere.svg"
            alt=""
            width={1600}
            height={1000}
          />
          <div className="landing-hero-fade" />
        </div>
        <div className="landing-hero-inner">
          <p className="landing-brand">AmarKatha</p>
          <h1 className="landing-headline">Indian indie comics, on your schedule</h1>
          <p className="landing-tagline">{tagline}</p>
          <div className="landing-actions">
            <a href="#stories" className="btn btn-primary">
              Browse stories
            </a>
            <a href="/creator/signup" className="btn btn-secondary">
              Creators: get started
            </a>
          </div>
        </div>
      </section>

      <section className="how-it-works" aria-labelledby="how-heading">
        <div className="section-shell">
          <h2 id="how-heading">How it works</h2>
          <p className="section-lead">Three steps from page to share link — no marketplace cold start.</p>
          <ol className="how-steps">
            <li>
              <span className="how-num">1</span>
              <div>
                <h3>Upload chapters</h3>
                <p>Drop page images, publish when ready.</p>
              </div>
            </li>
            <li>
              <span className="how-num">2</span>
              <div>
                <h3>Set your rhythm</h3>
                <p>Weekly or custom days. Skip a slot or pause on hiatus.</p>
              </div>
            </li>
            <li>
              <span className="how-num">3</span>
              <div>
                <h3>Share the link</h3>
                <p>Readers see the next update — and come back after a skip.</p>
              </div>
            </li>
          </ol>
        </div>
      </section>

      <section className="catalog" id="stories">
        <div className="section-shell">
          <div className="section-header">
            <h2>Recently updated</h2>
            <p>Fresh chapters from invite creators. Open a series to read — no account needed.</p>
          </div>
          {loading && <p className="state-message">Loading stories…</p>}
          {error && <p className="state-message error">{error}</p>}
          {data && data.recentlyUpdated.length === 0 && !loading && (
            <div className="catalog-empty">
              <p>Stories are arriving soon.</p>
              <p className="muted">
                Got a share link from a creator? Open it to start reading.
              </p>
              <div className="landing-actions catalog-empty-actions">
                <a href="/creator/signup" className="btn btn-secondary">
                  Creators: sign up with invite
                </a>
                <a href="/creator/login" className="btn btn-ghost">
                  Already a creator? Sign in
                </a>
              </div>
            </div>
          )}
          {data && data.recentlyUpdated.length > 0 && (
            <div className="series-grid">
              {data.recentlyUpdated.map((series, i) => (
                <div
                  key={series.slug}
                  className="series-grid-item"
                  style={{ animationDelay: `${Math.min(i, 6) * 60}ms` }}
                >
                  <SeriesCardView series={series} />
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="creators-strip">
        <div className="section-shell creators-strip-inner">
          <div>
            <h2>For creators</h2>
            <p>
              Invite-only while we validate flexible schedules with Indian indie comics.
              Publish, skip when life happens, share one link with your readers.
            </p>
          </div>
          <div className="landing-actions">
            <a href="/creator/signup" className="btn btn-primary">
              Sign up with invite
            </a>
            <a href="/creator/login" className="btn btn-secondary">
              Sign in
            </a>
          </div>
        </div>
      </section>
    </>
  );
}
