import { Link } from 'react-router-dom';
import type { SeriesCard } from '../types';

interface SeriesCardProps {
  series: SeriesCard;
}

function formatRelative(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  if (days === 0) return 'Updated today';
  if (days === 1) return 'Updated yesterday';
  return `Updated ${days} days ago`;
}

export default function SeriesCardView({ series }: SeriesCardProps) {
  return (
    <article className="series-card">
      <Link to={`/read/s/${series.slug}`} className="series-card-link">
        <div className="series-cover" style={{ background: series.coverGradient }}>
          <span className="series-lang">{series.contentLanguage.toUpperCase()}</span>
          {series.status === 'HIATUS' && <span className="series-badge hiatus">Hiatus</span>}
          {series.status === 'COMPLETED' && <span className="series-badge completed">Complete</span>}
        </div>
        <div className="series-body">
          <h3 className="series-title">{series.title}</h3>
          <p className="series-creator">by {series.creatorName}</p>
          <p className="series-desc">{series.description}</p>
          <div className="series-meta">
            <span className="schedule-pill">{series.scheduleLabel}</span>
            <span className="meta-dot">·</span>
            <span>{series.chapterCount} chapters</span>
          </div>
          <div className="series-tags">
            {series.genres.map((g) => (
              <span key={g} className="genre-tag">
                {g}
              </span>
            ))}
          </div>
          <p className="series-updated">{formatRelative(series.lastUpdatedAt)}</p>
        </div>
      </Link>
    </article>
  );
}
