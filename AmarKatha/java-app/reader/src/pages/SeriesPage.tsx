import { Link, useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { fetchSeries, trackSeriesView } from '../api/home';
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
          void trackSeriesView(seriesSlug);
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

  const scheduleLine = series.schedule?.nextExpectedAt
    ? `${formatLocalDate(series.schedule.nextExpectedAt)}${
        series.schedule.periodDays != null ? ` · every ${series.schedule.periodDays} days` : ''
      }`
    : (series.schedule?.scheduleLabel ?? series.scheduleLabel);

  return (
    <div className="series-page">
      <Link to="/" className="back-link">
        ← All series
      </Link>
      <header
        className={`series-hero${series.coverUrl ? ' has-cover' : ''}`}
        style={series.coverUrl ? undefined : { background: series.coverGradient }}
      >
        {series.coverUrl && (
          <img
            className="series-hero-image"
            src={series.coverUrl}
            alt=""
            decoding="async"
          />
        )}
        <div className="series-hero-fade" aria-hidden="true" />
        <div className="series-hero-content">
          <h1 className="series-hero-title">{series.title}</h1>
          <p className="series-creator">by {series.creatorName}</p>
          <div className="series-hero-schedule">
            <strong>{series.schedule?.headline ?? 'Schedule'}</strong>
            <p>{scheduleLine}</p>
            {series.schedule?.skipMessage && (
              <p className="hiatus-note">
                <em>{series.schedule.skipMessage}</em>
              </p>
            )}
            {(series.schedule?.status ?? series.status) === 'HIATUS' && (
              <p className="hiatus-note">This series is on hiatus. Check back when the creator resumes.</p>
            )}
          </div>
          {series.description && (
            <p className="series-hero-desc">{series.description}</p>
          )}
        </div>
      </header>
      <div className="series-content">
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
            {window.location.origin}/read/s/{series.slug}?ref=share
          </code>
        </div>
      </div>
    </div>
  );
}

function formatChapterNumber(n: number): string {
  return Number.isInteger(n) ? String(n) : String(n);
}

/** Format UTC Instant in the viewer's local timezone. */
function formatLocalDate(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  return new Intl.DateTimeFormat(undefined, {
    weekday: 'long',
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    hourCycle: 'h12',
  }).format(date);
}
