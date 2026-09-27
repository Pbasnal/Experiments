import type { ReactNode } from 'react';

interface CoverImageProps {
  url?: string | null;
  gradient?: string | null;
  alt?: string;
  className?: string;
  children?: ReactNode;
}

/**
 * Semantic cover: real `<img>` when a URL exists; gradient fallback otherwise.
 * Decorative covers use empty alt (parent provides the accessible name).
 */
export default function CoverImage({
  url,
  gradient,
  alt = '',
  className,
  children,
}: CoverImageProps) {
  const style = !url && gradient ? { background: gradient } : undefined;

  return (
    <div className={className} style={style}>
      {url ? (
        <img
          className="cover-image"
          src={url}
          alt={alt}
          decoding="async"
          loading="lazy"
        />
      ) : null}
      {children}
    </div>
  );
}
