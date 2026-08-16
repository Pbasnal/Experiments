import { Link, useParams } from 'react-router-dom';
import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchChapter, fetchSeries, trackChapterView } from '../api/home';
import type { ChapterReader, ChapterSummary } from '../types';
import ChapterBlock from '../components/reader/ChapterBlock';
import ChapterJumpSheet from '../components/reader/ChapterJumpSheet';
import {
  formatChapterNumber,
  useChapterWindow,
} from '../components/reader/useChapterWindow';

export default function ChapterReaderPage() {
  const { seriesSlug, chapterSlug } = useParams<{
    seriesSlug: string;
    chapterSlug: string;
  }>();
  const [playlist, setPlaylist] = useState<ChapterSummary[] | null>(null);
  const [seriesTitle, setSeriesTitle] = useState('');
  const [entryChapter, setEntryChapter] = useState<ChapterReader | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const bootstrappedSeriesRef = useRef<string | null>(null);

  useEffect(() => {
    if (!seriesSlug || !chapterSlug) {
      return;
    }

    // URL replace from infinite-scroll must not re-bootstrap the session.
    if (bootstrappedSeriesRef.current === seriesSlug && playlist) {
      return;
    }

    let cancelled = false;
    setLoading(true);
    setEntryChapter(null);
    setPlaylist(null);
    bootstrappedSeriesRef.current = null;

    Promise.all([fetchSeries(seriesSlug), fetchChapter(seriesSlug, chapterSlug)])
      .then(([series, chapter]) => {
        if (cancelled) {
          return;
        }
        const index = series.chapters.findIndex((c) => c.slug === chapterSlug);
        if (index < 0) {
          setError('Chapter not found');
          setEntryChapter(null);
          setPlaylist(null);
          return;
        }
        setSeriesTitle(series.title);
        setPlaylist(series.chapters);
        setEntryChapter(chapter);
        setError(null);
        bootstrappedSeriesRef.current = seriesSlug;
        void trackChapterView(seriesSlug, chapterSlug);
      })
      .catch((e: Error) => {
        if (!cancelled) {
          setEntryChapter(null);
          setPlaylist(null);
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
    // playlist intentionally omitted — used only as a gate via ref + closure
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seriesSlug, chapterSlug]);

  if (loading) {
    return (
      <div className="chapter-reader empty">
        <p>Loading chapter…</p>
      </div>
    );
  }

  if (error || !entryChapter || !playlist || !seriesSlug) {
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
    <ChapterReaderSession
      key={seriesSlug}
      seriesSlug={seriesSlug}
      seriesTitle={seriesTitle}
      playlist={playlist}
      entryChapter={entryChapter}
    />
  );
}

function ChapterReaderSession({
  seriesSlug,
  seriesTitle,
  playlist,
  entryChapter,
}: {
  seriesSlug: string;
  seriesTitle: string;
  playlist: ChapterSummary[];
  entryChapter: ChapterReader;
}) {
  const [sheetOpen, setSheetOpen] = useState(false);
  const topSentinelRef = useRef<HTMLDivElement>(null);
  const bottomSentinelRef = useRef<HTMLDivElement>(null);

  const {
    orderedChapters,
    activeSlug,
    loadingPrev,
    loadingNext,
    atStart,
    atEnd,
    jumping,
    loadPrev,
    loadNext,
    retryPrev,
    retryNext,
    jumpTo,
  } = useChapterWindow({ seriesSlug, playlist, entryChapter });

  const activeMeta = playlist.find((c) => c.slug === activeSlug);

  useEffect(() => {
    const top = topSentinelRef.current;
    const bottom = bottomSentinelRef.current;
    if (!top || !bottom) {
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) {
            continue;
          }
          if (entry.target === top) {
            void loadPrev();
          } else if (entry.target === bottom) {
            void loadNext();
          }
        }
      },
      { root: null, rootMargin: '400px 0px', threshold: 0 },
    );

    observer.observe(top);
    observer.observe(bottom);
    return () => observer.disconnect();
  }, [loadPrev, loadNext]);

  const handleJump = useCallback(
    async (slug: string) => {
      await jumpTo(slug);
      setSheetOpen(false);
    },
    [jumpTo],
  );

  return (
    <div className="chapter-reader">
      <header className="chapter-reader-header">
        <Link to={`/read/s/${seriesSlug}`} className="back-link">
          ← {seriesTitle}
        </Link>
        {activeMeta && (
          <p className="chapter-reader-active">
            Reading Ch. {formatChapterNumber(activeMeta.chapterNumber)}
          </p>
        )}
      </header>

      <button
        type="button"
        className="chapter-jump-fab"
        onClick={() => setSheetOpen(true)}
        aria-haspopup="dialog"
        aria-expanded={sheetOpen}
      >
        Chapters
      </button>

      <div className="chapter-feed">
        <div ref={topSentinelRef} className="chapter-sentinel" aria-hidden="true" />

        {loadingPrev && (
          <div
            className={`chapter-load-banner${loadingPrev.error ? ' is-error' : ''}`}
            role="status"
          >
            {loadingPrev.error ? (
              <>
                <p>
                  Couldn’t open Chapter {formatChapterNumber(loadingPrev.chapterNumber)}.
                </p>
                <button type="button" className="btn btn-secondary" onClick={retryPrev}>
                  Retry
                </button>
              </>
            ) : (
              <p>Opening Chapter {formatChapterNumber(loadingPrev.chapterNumber)}…</p>
            )}
          </div>
        )}

        {atStart && !loadingPrev && (
          <p className="chapter-edge-note">This is the first chapter.</p>
        )}

        {orderedChapters.map((chapter) => (
          <ChapterBlock key={chapter.chapterSlug} chapter={chapter} />
        ))}

        {loadingNext && (
          <div
            className={`chapter-load-banner${loadingNext.error ? ' is-error' : ''}`}
            role="status"
          >
            {loadingNext.error ? (
              <>
                <p>
                  Couldn’t open Chapter {formatChapterNumber(loadingNext.chapterNumber)}.
                </p>
                <button type="button" className="btn btn-secondary" onClick={retryNext}>
                  Retry
                </button>
              </>
            ) : (
              <p>Opening Chapter {formatChapterNumber(loadingNext.chapterNumber)}…</p>
            )}
          </div>
        )}

        {atEnd && !loadingNext && (
          <p className="chapter-edge-note">You’ve reached the latest chapter.</p>
        )}

        <div ref={bottomSentinelRef} className="chapter-sentinel" aria-hidden="true" />
      </div>

      <footer className="chapter-reader-footer">
        <Link to={`/read/s/${seriesSlug}`} className="btn btn-secondary">
          Back to series
        </Link>
      </footer>

      <ChapterJumpSheet
        open={sheetOpen}
        chapters={playlist}
        activeSlug={activeSlug}
        onClose={() => setSheetOpen(false)}
        onSelect={(slug) => void handleJump(slug)}
      />

      {jumping && (
        <div className="chapter-jump-busy" role="status">
          Opening chapter…
        </div>
      )}
    </div>
  );
}
