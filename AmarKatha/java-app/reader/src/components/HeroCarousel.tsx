import { useCallback, useEffect, useId, useRef, useState, type TouchEvent } from 'react';
import { Link } from 'react-router-dom';
import type { SeriesCard } from '../types';
import CoverImage from './CoverImage';
import SeriesSignals from './SeriesSignals';

const SWIPE_THRESHOLD_PX = 48;

interface HeroCarouselProps {
  slides: SeriesCard[];
}

function langLabel(code: string): string {
  return code.trim().toUpperCase();
}

/**
 * Simple carousel semantics (not a tabs pattern): region + live region + prev/next +
 * slide chooser buttons with aria-current.
 */
export default function HeroCarousel({ slides }: HeroCarouselProps) {
  const [index, setIndex] = useState(0);
  const touchStartX = useRef<number | null>(null);
  const labelId = useId();
  const multi = slides.length > 1;
  const slide = slides[index];

  const go = useCallback(
    (next: number) => {
      if (slides.length === 0) return;
      const len = slides.length;
      setIndex(((next % len) + len) % len);
    },
    [slides.length],
  );

  useEffect(() => {
    if (index >= slides.length) {
      setIndex(0);
    }
  }, [slides.length, index]);

  if (!slide) return null;

  function onTouchStart(e: TouchEvent) {
    touchStartX.current = e.changedTouches[0]?.clientX ?? null;
  }

  function onTouchEnd(e: TouchEvent) {
    if (touchStartX.current == null || !multi) return;
    const endX = e.changedTouches[0]?.clientX ?? touchStartX.current;
    const delta = endX - touchStartX.current;
    touchStartX.current = null;
    if (Math.abs(delta) < SWIPE_THRESHOLD_PX) return;
    go(delta < 0 ? index + 1 : index - 1);
  }

  return (
    <section
      className="hero-carousel"
      aria-roledescription="carousel"
      aria-labelledby={labelId}
      onTouchStart={onTouchStart}
      onTouchEnd={onTouchEnd}
    >
      <div className="halftone hero-carousel-texture" aria-hidden="true" />
      <div className="section-shell hero-carousel-inner">
        <p id={labelId} className="section-kicker">
          Recently updated
        </p>
        <div
          className="hero-slide"
          aria-live="polite"
          aria-atomic="true"
          aria-roledescription="slide"
          aria-label={`${index + 1} of ${slides.length}: ${slide.title}`}
        >
          <div className="hero-slide-copy">
            <h1 className="hero-slide-title">
              <Link to={`/read/s/${slide.slug}`} className="hero-slide-title-link">
                {slide.title}
              </Link>
            </h1>
            <p className="hero-slide-meta">
              <span className="free-tag">Free</span>
              <span>{langLabel(slide.contentLanguage)}</span>
              <span className="meta-dot" aria-hidden="true">
                ·
              </span>
              <span>
                {slide.chapterCount} chapter{slide.chapterCount === 1 ? '' : 's'}
              </span>
            </p>
            {slide.description ? (
              <p className="hero-slide-desc">{slide.description}</p>
            ) : null}
            <SeriesSignals series={slide} className="hero-signals" />
            <p className="hero-slide-byline">
              by {slide.creatorName}
              {slide.scheduleLabel ? (
                <>
                  <span className="meta-dot" aria-hidden="true">
                    ·
                  </span>
                  <span className="hero-slide-schedule">{slide.scheduleLabel}</span>
                </>
              ) : null}
            </p>
            <div className="landing-actions">
              <Link to={`/read/s/${slide.slug}`} className="btn btn-primary">
                Read
              </Link>
              <a href="#stories" className="btn btn-secondary">
                Browse stories
              </a>
            </div>
          </div>
          <div className="hero-slide-cover-wrap" aria-hidden="true">
            <CoverImage
              className="hero-slide-cover panel-border"
              url={slide.coverUrl}
              gradient={slide.coverGradient}
              alt=""
            />
          </div>
        </div>

        {multi ? (
          <div className="hero-carousel-controls">
            <button
              type="button"
              className="hero-carousel-nav"
              onClick={() => go(index - 1)}
              aria-label="Previous featured series"
            >
              ‹
            </button>
            <div className="hero-carousel-dots" role="group" aria-label="Carousel slides">
              {slides.map((s, i) => (
                <button
                  key={s.slug}
                  type="button"
                  aria-label={`Show ${s.title}`}
                  aria-current={i === index ? 'true' : undefined}
                  className={`hero-carousel-dot${i === index ? ' is-active' : ''}`}
                  onClick={() => setIndex(i)}
                />
              ))}
            </div>
            <button
              type="button"
              className="hero-carousel-nav"
              onClick={() => go(index + 1)}
              aria-label="Next featured series"
            >
              ›
            </button>
          </div>
        ) : null}
      </div>
    </section>
  );
}
