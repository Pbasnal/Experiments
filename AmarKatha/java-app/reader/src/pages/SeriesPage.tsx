import { Link, useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { fetchHome } from '../api/home';
import type { SeriesCard } from '../types';

export default function SeriesPage() {
  const { seriesSlug } = useParams<{ seriesSlug: string }>();
  const [series, setSeries] = useState<SeriesCard | null>(null);

  useEffect(() => {
    fetchHome().then((home) => {
      const match = home.recentlyUpdated.find((s) => s.slug === seriesSlug);
      setSeries(match ?? null);
    });
  }, [seriesSlug]);

  if (!series) {
    return (
      <div className="series-page empty">
        <p>Loading series…</p>
        <Link to="/">← Back to home</Link>
      </div>
    );
  }

  return (
    <div className="series-page">
      <Link to="/" className="back-link">
        ← All series
      </Link>
      <div className="series-hero" style={{ background: series.coverGradient }}>
        <div className="series-hero-content">
          <span className="preview-badge">Series hub · Preview</span>
          <h1>{series.title}</h1>
          <p className="series-creator">by {series.creatorName}</p>
        </div>
      </div>
      <div className="series-content">
        <aside className="schedule-strip">
          <strong>Schedule</strong>
          <p>{series.scheduleLabel}</p>
          {series.status === 'HIATUS' && (
            <p className="hiatus-note">This series is on hiatus. Check back when the creator resumes.</p>
          )}
        </aside>
        <p className="series-synopsis">{series.description}</p>
        <div className="series-tags">
          {series.genres.map((g) => (
            <span key={g} className="genre-tag">
              {g}
            </span>
          ))}
        </div>
        <section className="chapter-list-preview">
          <h2>Chapters</h2>
          <p className="muted">
            Chapter reader coming in Week 3. {series.chapterCount} chapters will appear here when listed.
          </p>
          <ul>
            {Array.from({ length: Math.min(series.chapterCount, 5) }, (_, i) => (
              <li key={i}>
                <span className="chapter-num">Ch. {i + 1}</span>
                <span className="chapter-placeholder">Vertical reader — planned</span>
              </li>
            ))}
          </ul>
        </section>
        <div className="share-box">
          <label>Share link (V0 primary distribution)</label>
          <code>{window.location.origin}/read/s/{series.slug}</code>
        </div>
      </div>
    </div>
  );
}
