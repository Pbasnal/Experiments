import { useEffect, useRef, useState } from 'react';
import {
  EngagementAuthError,
  readerLoginWithReturn,
  setGlimpseReaction,
} from '../api/engagement';
import type { Glimpse, GlimpseImage, GlimpseTag } from '../types';

const TAG_LABEL: Record<GlimpseTag, string> = {
  CHARACTER: 'Character',
  BACKGROUND: 'Background',
  LORE: 'Lore',
  ITEMS: 'Items',
  TEASER: 'Teaser',
};

export default function GlimpseStrip({
  seriesSlug,
  glimpse,
}: {
  seriesSlug: string;
  glimpse: Glimpse;
}) {
  const [images, setImages] = useState(glimpse.images);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [active, setActive] = useState(0);
  const [openIndex, setOpenIndex] = useState<number | null>(null);
  const [loginPrompt, setLoginPrompt] = useState(false);
  const stripRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (openIndex == null) {
      return;
    }
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpenIndex(null);
      } else if (event.key === 'ArrowRight') {
        setOpenIndex((current) =>
          current == null ? current : Math.min(images.length - 1, current + 1),
        );
      } else if (event.key === 'ArrowLeft') {
        setOpenIndex((current) => (current == null ? current : Math.max(0, current - 1)));
      }
    }
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [openIndex, images.length]);

  async function toggle(image: GlimpseImage) {
    if (busyId) {
      return;
    }
    setBusyId(image.id);
    try {
      const next = await setGlimpseReaction(glimpse.id, image.id, !image.reacted);
      setImages((current) =>
        current.map((item) =>
          item.id === image.id
            ? { ...item, reactionCount: next.count, reacted: next.reacted }
            : item,
        ),
      );
    } catch (e) {
      if (e instanceof EngagementAuthError) {
        setLoginPrompt(true);
      }
    } finally {
      setBusyId(null);
    }
  }

  function onScroll() {
    const strip = stripRef.current;
    if (!strip) {
      return;
    }
    const frames = Array.from(strip.children) as HTMLElement[];
    const left = strip.scrollLeft;
    let nearest = 0;
    let best = Number.POSITIVE_INFINITY;
    frames.forEach((frame, index) => {
      const distance = Math.abs(frame.offsetLeft - strip.offsetLeft - left);
      if (distance < best) {
        best = distance;
        nearest = index;
      }
    });
    if (strip.scrollLeft + strip.clientWidth >= strip.scrollWidth - 2) {
      nearest = frames.length - 1;
    }
    setActive(nearest);
  }

  function goTo(index: number) {
    const strip = stripRef.current;
    const frame = strip?.children[index] as HTMLElement | undefined;
    if (strip && frame) {
      strip.scrollTo({ left: frame.offsetLeft - strip.offsetLeft, behavior: 'smooth' });
    }
  }

  const label = TAG_LABEL[glimpse.tag] ?? glimpse.tag;

  return (
    <section className="glimpse-card" id={`glimpse-${glimpse.id}`}>
      <p className="glimpse-kicker">Glimpse · {label}</p>
      <div className="glimpse-strip" ref={stripRef} onScroll={onScroll}>
        {images.map((image, index) => (
          <figure className="glimpse-frame" key={image.id}>
            <button
              type="button"
              className="glimpse-open"
              onClick={() => setOpenIndex(index)}
              aria-label={`Open ${label} image ${index + 1} of ${images.length}`}
            >
              <img src={image.url} alt="" />
            </button>
            <button
              type="button"
              className={image.reacted ? 'glimpse-react is-on' : 'glimpse-react'}
              aria-pressed={image.reacted}
              aria-label={image.reacted ? 'Remove reaction' : 'React'}
              disabled={busyId === image.id}
              onClick={() => void toggle(image)}
            >
              <span aria-hidden="true">{image.reacted ? '♥' : '♡'}</span>
              {image.reactionCount > 0 ? <span>{image.reactionCount}</span> : null}
            </button>
          </figure>
        ))}
      </div>
      {openIndex != null ? (
        <div
          className="glimpse-lightbox"
          role="dialog"
          aria-modal="true"
          aria-label={`${label} image ${openIndex + 1} of ${images.length}`}
          onClick={() => setOpenIndex(null)}
        >
          <button type="button" className="glimpse-lightbox-close" onClick={() => setOpenIndex(null)}>
            Close
          </button>
          <img
            src={images[openIndex].url}
            alt={`${label} glimpse ${openIndex + 1} of ${images.length}`}
            onClick={(event) => event.stopPropagation()}
          />
        </div>
      ) : null}
      {loginPrompt ? (
        <div
          className="glimpse-login-overlay"
          role="presentation"
          onClick={() => setLoginPrompt(false)}
        >
          <div
            className="glimpse-login-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby={`glimpse-login-${glimpse.id}`}
            onClick={(event) => event.stopPropagation()}
          >
            <h2 id={`glimpse-login-${glimpse.id}`}>Log in to react</h2>
            <p>You need to log in to react to a glimpse.</p>
            <div className="glimpse-login-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setLoginPrompt(false)}>
                Not now
              </button>
              <a className="btn btn-primary" href={readerLoginWithReturn(`/read/s/${seriesSlug}#glimpse-${glimpse.id}`)}>
                Log in
              </a>
            </div>
          </div>
        </div>
      ) : null}
      {images.length > 1 ? (
        <div className="glimpse-dots" role="tablist" aria-label="Glimpse images">
          {images.map((image, index) => (
            <button
              key={image.id}
              type="button"
              role="tab"
              aria-selected={index === active}
              aria-label={`Image ${index + 1}`}
              className={index === active ? 'glimpse-dot is-active' : 'glimpse-dot'}
              onClick={() => goTo(index)}
            />
          ))}
        </div>
      ) : null}
    </section>
  );
}
