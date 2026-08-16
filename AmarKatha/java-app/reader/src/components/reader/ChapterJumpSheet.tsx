import { useEffect, useId, useRef } from 'react';
import type { ChapterSummary } from '../../types';

interface ChapterJumpSheetProps {
  open: boolean;
  chapters: ChapterSummary[];
  activeSlug: string;
  onClose: () => void;
  onSelect: (slug: string) => void;
}

export default function ChapterJumpSheet({
  open,
  chapters,
  activeSlug,
  onClose,
  onSelect,
}: ChapterJumpSheetProps) {
  const titleId = useId();
  const closeRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    closeRef.current?.focus();
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        onClose();
      }
    }
    document.addEventListener('keydown', onKey);
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = prevOverflow;
    };
  }, [open, onClose]);

  if (!open) {
    return null;
  }

  return (
    <div className="chapter-jump-overlay" role="presentation" onClick={onClose}>
      <div
        className="chapter-jump-sheet"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="chapter-jump-sheet-header">
          <h2 id={titleId}>Chapters</h2>
          <button
            ref={closeRef}
            type="button"
            className="chapter-jump-close"
            onClick={onClose}
            aria-label="Close chapter list"
          >
            ×
          </button>
        </div>
        <ul className="chapter-jump-list">
          {chapters.map((chapter) => {
            const title =
              chapter.title?.trim() ||
              `Chapter ${formatChapterNumber(chapter.chapterNumber)}`;
            const isActive = chapter.slug === activeSlug;
            return (
              <li key={chapter.slug}>
                <button
                  type="button"
                  className={`chapter-jump-item${isActive ? ' is-active' : ''}`}
                  onClick={() => onSelect(chapter.slug)}
                  aria-current={isActive ? 'true' : undefined}
                >
                  <span className="chapter-jump-item-title">{title}</span>
                  <span className="chapter-jump-item-num">
                    Ch. {formatChapterNumber(chapter.chapterNumber)}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}

function formatChapterNumber(n: number): string {
  return Number.isInteger(n) ? String(n) : String(n);
}
