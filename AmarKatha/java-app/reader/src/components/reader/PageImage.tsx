import { useState } from 'react';
import type { ChapterPage } from '../../types';

interface PageImageProps {
  page: ChapterPage;
}

export default function PageImage({ page }: PageImageProps) {
  const [loaded, setLoaded] = useState(false);
  const [failed, setFailed] = useState(false);
  const hasSize = page.width != null && page.height != null && page.width > 0 && page.height > 0;
  const aspectRatio = hasSize ? `${page.width} / ${page.height}` : '2 / 3';

  return (
    <div className="page-slot" style={{ aspectRatio }}>
      {!loaded && !failed && <div className="page-skeleton" aria-hidden="true" />}
      {failed ? (
        <div className="page-unavailable" role="img" aria-label={`Page ${page.sortOrder} unavailable`}>
          Image unavailable
        </div>
      ) : (
        <img
          className={`page-image${loaded ? ' is-loaded' : ' is-loading'}`}
          src={page.imageUrl}
          alt={`Page ${page.sortOrder}`}
          loading="lazy"
          decoding="async"
          width={page.width ?? undefined}
          height={page.height ?? undefined}
          onLoad={() => setLoaded(true)}
          onError={() => setFailed(true)}
        />
      )}
    </div>
  );
}
