import { Link, useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { fetchSeries } from '../api/home';
import type { SeriesDetail } from '../types';

export default function SeriesPage() {
  const { seriesSlug } = useParams<{ seriesSlug: string }>();
  const [series, setSeries] = useState<SeriesDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!seriesSlug) {
      return;
    }
    let cancelled = false;
    setLoading(true);
    fetchSeries(seriesSlug)
      .then((data) => {
        if (!cancelled) {
          setSeries(data);
          setError(null);
        }
      })
      .catch((e: Error) => {
        if (!cancelled) {
          setSeries(null);
          setError(e.message);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [seriesSlug]);

  if (loading) {
    return (
      <div className="series-page empty">
        <p>Loading series…</p>
        <Link to="/">← Back to home</Link>
      </div>
    );
  }

  if (error || !series) {
    return (
      <div className="series-page empty">
        <p>{error ?? 'Series not found'}</p>
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
        {series.description && <p className="series-synopsis">{series.description}</p>}
        {series.genres.length > 0 && (
          <div className="series-tags">
            {series.genres.map((g) => (
              <span key={g} className="genre-tag">
                {g}
              </span>
            ))}
          </div>
        )}
        <section className="chapter-list-preview">
          <h2>Chapters</h2>
          <p className="muted">{series.chapterCount} listed chapter{series.chapterCount === 1 ? '' : 's'}</p>
          <ul>
            {series.chapters.map((chapter) => (
              <li key={chapter.slug}>
                <Link to={`/read/s/${series.slug}/c/${chapter.slug}`} className="chapter-link">
                  <span className="chapter-num">Ch. {formatChapterNumber(chapter.chapterNumber)}</span>
                  <span className="chapter-title">{chapter.title}</span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
        <div className="share-box">
          <label>Share link</label>
          <code>
            {window.location.origin}/read/s/{series.slug}
          </code>
        </div>
      </div>
    </div>
  );
}

function formatChapterNumber(n: number): string {
  return Number.isInteger(n) ? String(n) : String(n);
}
