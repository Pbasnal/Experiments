import { Link } from 'react-router-dom';
import type { SeriesCard } from '../types';
import CoverImage from './CoverImage';
import SeriesSignals from './SeriesSignals';

interface SeriesCardProps {
  series: SeriesCard;
}

function formatRelative(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  if (days <= 0) return 'Updated today';
  if (days === 1) return 'Updated yesterday';
  return `Updated ${days} days ago`;
}

export default function SeriesCardView({ series }: SeriesCardProps) {
  return (
    <article className="series-card">
      <Link
        to={`/read/s/${series.slug}`}
        className="series-card-link"
        aria-label={`${series.title} by ${series.creatorName}. Free to read.`}
      >
        <CoverImage
          className="series-cover"
          url={series.coverUrl}
          gradient={series.coverGradient}
        >
          <span className="series-lang">{series.contentLanguage.toUpperCase()}</span>
          <span className="free-tag series-free-tag" aria-hidden="true">
            Free
          </span>
          {series.status === 'HIATUS' && <span className="series-badge hiatus">Hiatus</span>}
          {series.status === 'COMPLETED' && (
            <span className="series-badge completed">Complete</span>
          )}
          <div className="series-cover-fade" aria-hidden="true" />
          <div className="series-cover-meta">
            <span>
              {series.chapterCount} ch.
              {series.scheduleLabel ? ` · ${series.scheduleLabel}` : ''}
            </span>
          </div>
        </CoverImage>
        <div className="series-body">
          <h3 className="series-title">{series.title}</h3>
          <SeriesSignals series={series} />
          <p className="series-creator">{series.creatorName}</p>
          <p className="series-updated">{formatRelative(series.lastUpdatedAt)}</p>
        </div>
      </Link>
    </article>
  );
}
