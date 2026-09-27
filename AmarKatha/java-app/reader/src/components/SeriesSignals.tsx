import type { SeriesCard } from '../types';

type Props = {
  series: Pick<SeriesCard, 'rating' | 'readerCount' | 'editorsPick'>;
  className?: string;
};

export function hasSeriesSignals(series: Props['series']): boolean {
  return (series.rating ?? 0) > 0 || (series.readerCount ?? 0) > 0 || Boolean(series.editorsPick);
}

export function formatReaderCount(count: number): string {
  if (count >= 1_000_000) {
    return `${trimOneDecimal(count / 1_000_000)}M`;
  }
  if (count >= 1_000) {
    return `${trimOneDecimal(count / 1_000)}K`;
  }
  return String(count);
}

function trimOneDecimal(value: number): string {
  const rounded = Math.round(value * 10) / 10;
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1);
}

export default function SeriesSignals({ series, className }: Props) {
  const rating = series.rating ?? 0;
  const readers = series.readerCount ?? 0;
  if (!hasSeriesSignals(series)) {
    return null;
  }

  return (
    <p className={className ? `series-signals ${className}` : 'series-signals'}>
      {series.editorsPick ? <span className="editors-pick">Editor’s pick</span> : null}
      {rating > 0 ? (
        <span className="series-rating">
          <span aria-hidden="true">★</span>
          <span className="visually-hidden">Rated </span>
          {rating.toFixed(1)}
        </span>
      ) : null}
      {readers > 0 ? (
        <span className="series-readers">
          {formatReaderCount(readers)} readers
        </span>
      ) : null}
    </p>
  );
}
