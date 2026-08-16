import type { ChapterReader } from '../../types';
import PageImage from './PageImage';

interface ChapterBlockProps {
  chapter: ChapterReader;
}

export default function ChapterBlock({ chapter }: ChapterBlockProps) {
  const title =
    chapter.title?.trim() || `Chapter ${formatChapterNumber(chapter.chapterNumber)}`;

  return (
    <section
      id={`chapter-${chapter.chapterSlug}`}
      className="chapter-block"
      data-chapter-slug={chapter.chapterSlug}
    >
      <header className="chapter-boundary">
        <div className="chapter-boundary-rule" aria-hidden="true" />
        <h2 className="chapter-boundary-title">{title}</h2>
        <p className="chapter-boundary-num">Ch. {formatChapterNumber(chapter.chapterNumber)}</p>
      </header>
      <div className="page-stack">
        {chapter.pages.map((page) => (
          <PageImage key={page.sortOrder} page={page} />
        ))}
      </div>
    </section>
  );
}

function formatChapterNumber(n: number): string {
  return Number.isInteger(n) ? String(n) : String(n);
}
