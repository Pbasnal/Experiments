import { Link, useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { fetchChapter } from '../api/home';
import type { ChapterReader } from '../types';

export default function ChapterReaderPage() {
  const { seriesSlug, chapterSlug } = useParams<{
    seriesSlug: string;
    chapterSlug: string;
  }>();
  const [chapter, setChapter] = useState<ChapterReader | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!seriesSlug || !chapterSlug) {
      return;
    }
    let cancelled = false;
    setLoading(true);
    fetchChapter(seriesSlug, chapterSlug)
      .then((data) => {
        if (!cancelled) {
          setChapter(data);
          setError(null);
        }
      })
      .catch((e: Error) => {
        if (!cancelled) {
          setChapter(null);
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
  }, [seriesSlug, chapterSlug]);

  if (loading) {
    return (
      <div className="chapter-reader empty">
        <p>Loading chapter…</p>
      </div>
    );
  }

  if (error || !chapter) {
    return (
      <div className="chapter-reader empty">
        <p>{error ?? 'Chapter not found'}</p>
        {seriesSlug && (
          <Link to={`/read/s/${seriesSlug}`}>← Back to series</Link>
        )}
      </div>
    );
  }

  return (
    <div className="chapter-reader">
      <header className="chapter-reader-header">
        <Link to={`/read/s/${chapter.seriesSlug}`} className="back-link">
          ← {chapter.seriesTitle}
        </Link>
        <h1>
          Ch. {formatChapterNumber(chapter.chapterNumber)} · {chapter.title}
        </h1>
      </header>
      <div className="page-stack">
        {chapter.pages.map((page) => (
          <img
            key={page.sortOrder}
            className="page-image"
            src={page.imageUrl}
            alt={`Page ${page.sortOrder}`}
            loading="lazy"
            width={page.width ?? undefined}
            height={page.height ?? undefined}
          />
        ))}
      </div>
      <footer className="chapter-reader-footer">
        <Link to={`/read/s/${chapter.seriesSlug}`} className="btn btn-secondary">
          Back to series
        </Link>
      </footer>
    </div>
  );
}

function formatChapterNumber(n: number): string {
  return Number.isInteger(n) ? String(n) : String(n);
}
