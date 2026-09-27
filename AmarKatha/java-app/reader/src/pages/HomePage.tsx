import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { trackLandingView, trackSignupClick } from '../api/analytics';
import { fetchHome } from '../api/home';
import { fetchMe, type AuthMeResponse } from '../api/auth';
import type { HomeResponse, SeriesCard } from '../types';
import HeroCarousel from '../components/HeroCarousel';
import SeriesCardView from '../components/SeriesCard';
import HowStepIcon from '../components/HowStepIcon';
import CoverImage from '../components/CoverImage';
import { useFeatures } from '../features/FeatureContext';
import {
  matchesLanguage,
  resolveLanguageFilter,
  shouldShowLanguageStrip,
} from '../utils/languageFilter';

const HERO_SLIDE_LIMIT = 5;
const STATS_MIN_SERIES = 3;
const WEEK_MS = 7 * 24 * 60 * 60 * 1000;

const HOW_STEPS = [
  {
    name: 'upload',
    title: 'Upload chapters',
    body: 'Drop page images, publish when ready.',
  },
  {
    name: 'schedule',
    title: 'Set your rhythm',
    body: 'Weekly or custom days. Skip a slot or pause on hiatus.',
  },
  {
    name: 'share',
    title: 'Share the link',
    body: 'Readers see the next update and come back after a skip.',
  },
  {
    name: 'pause',
    title: 'Skip and hiatus',
    body: 'Skip a slot or pause. Readers see the next expected update.',
  },
  {
    name: 'owned',
    title: 'Creator-owned',
    body: 'You made it, you keep it. A platform, not a publisher.',
  },
] as const;

const BHARAT_POINTS = [
  {
    icon: 'thunder',
    title: 'Story first',
    body: 'Rough pages and regional voices are welcome. The story matters more than polish.',
  },
  {
    icon: 'language',
    title: 'Every language',
    body: 'Hindi, Tamil, Bengali, and more. Readers filter when the catalog is ready.',
  },
  {
    icon: 'free',
    title: 'Free to read',
    body: 'Open a series and start. No account, no paywall on what is published today.',
  },
] as const;

export type CreatorSpotlight = {
  name: string;
  count: number;
  firstSlug: string;
  genres: string[];
};

export function deriveCreators(series: SeriesCard[]): CreatorSpotlight[] {
  const map = new Map<string, { count: number; firstSlug: string; genres: Set<string> }>();
  for (const s of series) {
    const existing = map.get(s.creatorName);
    if (existing) {
      existing.count += 1;
    } else {
      map.set(s.creatorName, { count: 1, firstSlug: s.slug, genres: new Set() });
    }
    for (const genre of s.genres ?? []) {
      const trimmed = genre.trim();
      if (trimmed) map.get(s.creatorName)?.genres.add(trimmed);
    }
  }
  return [...map.entries()]
    .map(([name, v]) => ({
      name,
      count: v.count,
      firstSlug: v.firstSlug,
      genres: [...v.genres].slice(0, 2),
    }))
    .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name));
}

export default function HomePage() {
  const { features } = useFeatures();
  const [data, setData] = useState<HomeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [auth, setAuth] = useState<AuthMeResponse | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();

  useEffect(() => {
    trackLandingView();
  }, []);

  useEffect(() => {
    fetchHome()
      .then(setData)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    let cancelled = false;
    fetchMe().then((me) => {
      if (!cancelled) setAuth(me);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  const languageOptions = data?.languageOptions;
  const showLanguageStrip = shouldShowLanguageStrip(
    data?.filters,
    languageOptions,
    features.landingDiscovery,
  );

  const selectedLanguage = resolveLanguageFilter(
    searchParams.get('language'),
    languageOptions,
    data?.filters,
  );

  const catalog = data?.recentlyUpdated ?? [];

  const filteredCatalog = useMemo(() => {
    if (!selectedLanguage) return catalog;
    return catalog.filter((s) => matchesLanguage(s, selectedLanguage));
  }, [catalog, selectedLanguage]);

  const heroSlides = useMemo(() => {
    const picks = catalog.filter((series) => series.editorsPick);
    return (picks.length > 0 ? picks : catalog).slice(0, HERO_SLIDE_LIMIT);
  }, [catalog]);

  const creators = useMemo(() => deriveCreators(catalog), [catalog]);

  const updatedThisWeek = useMemo(() => {
    const cutoff = Date.now() - WEEK_MS;
    return catalog.filter((s) => {
      const t = new Date(s.lastUpdatedAt).getTime();
      return Number.isFinite(t) && t >= cutoff;
    });
  }, [catalog]);

  const languagesInCatalog = useMemo(() => {
    const set = new Set(catalog.map((s) => s.contentLanguage.trim().toLowerCase()).filter(Boolean));
    return set.size;
  }, [catalog]);

  const showStats = catalog.length >= STATS_MIN_SERIES;
  const showDiscoveryHero = features.landingDiscovery && heroSlides.length > 0;

  function setLanguage(code: string | null) {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (code) next.set('language', code);
        else next.delete('language');
        return next;
      },
      { replace: true },
    );
  }

  const signedIn = Boolean(auth?.authenticated);
  const tagline =
    data?.tagline ?? 'Publish on your rhythm. Share a link. Readers know when you’re back.';

  const firstSeriesHref = catalog[0] ? `/read/s/${catalog[0].slug}` : '#stories';

  return (
    <div className="home-page">
      {showDiscoveryHero ? (
        <HeroCarousel slides={heroSlides} />
      ) : (
        !loading && (
          <section className="hero-empty" aria-labelledby="hero-empty-heading">
            <div className="halftone hero-carousel-texture" aria-hidden="true" />
            <div className="section-shell hero-empty-inner">
              <p className="section-kicker">AmarKatha</p>
              <h1 id="hero-empty-heading" className="hero-slide-title">
                {features.landingDiscovery
                  ? 'Indian indie comics, on the creator’s clock'
                  : 'Start reading'}
              </h1>
              <p className="hero-slide-desc">
                {features.landingDiscovery
                  ? tagline
                  : 'Browse listed series below — no account needed to read.'}
              </p>
              <div className="landing-actions">
                {!signedIn ? (
                  <a
                    href="/login/reader"
                    className="btn btn-primary"
                    onClick={() => trackSignupClick('homepage')}
                  >
                    Sign up to read
                  </a>
                ) : null}
                <a
                  href={features.landingDiscovery ? '#stories' : firstSeriesHref}
                  className="btn btn-secondary"
                >
                  {features.landingDiscovery || !catalog[0] ? 'Browse stories' : 'Open a series'}
                </a>
              </div>
            </div>
          </section>
        )
      )}

      {features.landingDiscovery ? (
        <section className="creator-pitch" aria-labelledby="how-heading">
          <div className="section-shell creator-pitch-panel">
            <div className="creator-pitch-top">
              <div>
                <p className="section-kicker">How it works</p>
                <h2 id="how-heading">Your story deserves to be read</h2>
                <p className="creator-pitch-message">
                  Amar Katha is built for writers, artists, and storytellers from every corner of
                  India. Publish in your language, find your audience, keep your rights.
                </p>
              </div>
              <a href="/creator/signup" className="btn btn-ink">
                Sign up with invite
              </a>
            </div>
            <ol className="how-steps">
              {HOW_STEPS.map((step, index) => (
                <li key={step.name} className="how-step">
                  <HowStepIcon name={step.name} step={index + 1} />
                  <div>
                    <h3>{step.title}</h3>
                    <p>{step.body}</p>
                  </div>
                </li>
              ))}
            </ol>
          </div>
        </section>
      ) : null}

      {showLanguageStrip && languageOptions ? (
        <section className="language-strip" aria-label="Filter by language">
          <div className="language-strip-inner">
            <p className="language-strip-label" id="language-strip-label">
              Read in
            </p>
            <div
              className="language-strip-scroll"
              role="group"
              aria-labelledby="language-strip-label"
            >
              <button
                type="button"
                className={`language-chip${!selectedLanguage ? ' is-active' : ''}`}
                aria-pressed={!selectedLanguage}
                onClick={() => setLanguage(null)}
              >
                <span className="language-chip-native">All</span>
                <span className="language-chip-count">{catalog.length}</span>
              </button>
              {languageOptions.map((opt) => {
                const active = selectedLanguage?.toLowerCase() === opt.code.toLowerCase();
                return (
                  <button
                    key={opt.code}
                    type="button"
                    className={`language-chip${active ? ' is-active' : ''}`}
                    aria-pressed={active}
                    onClick={() => setLanguage(opt.code)}
                  >
                    <span className="language-chip-native font-script">
                      {opt.nativeLabel || opt.label}
                    </span>
                    <span className="language-chip-en">{opt.label}</span>
                    <span className="language-chip-count">{opt.seriesCount}</span>
                  </button>
                );
              })}
            </div>
          </div>
        </section>
      ) : null}

      <section className="catalog" id="stories" aria-labelledby="stories-heading">
        <div className="section-shell">
          <header className="section-header">
            <p className="section-kicker">Recently updated</p>
            <h2 id="stories-heading">What India is Reading</h2>
            <p>
              Fresh chapters from invite creators. Open a series to read — no account needed.
              {selectedLanguage ? ' Showing your language filter.' : ''}
            </p>
          </header>

          {loading && (
            <p className="state-message" role="status">
              Loading stories…
            </p>
          )}
          {error && (
            <p className="state-message error" role="alert">
              {error}
            </p>
          )}

          {!loading && !error && catalog.length === 0 && (
            <div className="catalog-empty">
              <p>Stories are arriving soon.</p>
              <p className="muted">Got a share link from a creator? Open it to start reading.</p>
            </div>
          )}

          {!loading && !error && catalog.length > 0 && filteredCatalog.length === 0 && (
            <div className="catalog-empty">
              <p>No stories in that language yet.</p>
              <button type="button" className="btn btn-secondary" onClick={() => setLanguage(null)}>
                Show all languages
              </button>
            </div>
          )}

          {filteredCatalog.length > 0 && (
            <div className="series-grid">
              {filteredCatalog.map((series, i) => (
                <div
                  key={series.slug}
                  className="series-grid-item"
                  style={{ animationDelay: `${Math.min(i, 8) * 50}ms` }}
                >
                  <SeriesCardView series={series} />
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {features.landingDiscovery ? (
        <section className="welcome-band" aria-labelledby="welcome-heading">
          <div className="halftone welcome-band-texture" aria-hidden="true" />
          <div className="section-shell welcome-band-inner">
            <h2 id="welcome-heading">Comics for Every Bharat</h2>
            <p className="welcome-band-lead">{tagline}</p>
            <ul className="welcome-points">
              {BHARAT_POINTS.map((point) => (
                <li key={point.title}>
                  <BharatIcon name={point.icon} />
                  <div>
                    <h3>{point.title}</h3>
                    <p>{point.body}</p>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </section>
      ) : null}

      {features.landingDiscovery && creators.length > 0 ? (
        <section className="creators-row" aria-labelledby="creators-heading">
          <div className="section-shell">
            <header className="section-header creators-header">
              <div>
                <p className="section-kicker kicker-orange">Creators</p>
                <h2 id="creators-heading">Invite-only voices</h2>
              </div>
              <Link to="/creators" className="creators-view-all">
                View all creators
              </Link>
            </header>
            <ul className="creators-list">
              {creators.slice(0, 3).map((creator) => (
                <li key={creator.name}>
                  <CreatorCard creator={creator} />
                </li>
              ))}
            </ul>
          </div>
        </section>
      ) : null}

      {features.landingDiscovery && showStats ? (
        <section className="stats-band" aria-label="Catalog at a glance">
          <div className="section-shell stats-band-inner">
            <div>
              <p className="stats-value">{catalog.length}</p>
              <p className="stats-label">Series with listed chapters</p>
            </div>
            <div>
              <p className="stats-value">{languagesInCatalog}</p>
              <p className="stats-label">Languages in catalog</p>
            </div>
            <div>
              <p className="stats-value">₹0</p>
              <p className="stats-label">To start publishing</p>
            </div>
          </div>
        </section>
      ) : null}

      {features.landingDiscovery && updatedThisWeek.length > 0 ? (
        <section className="updated-week" aria-labelledby="updated-week-heading">
          <div className="section-shell">
            <header className="section-header">
              <p className="section-kicker kicker-orange">Fresh off the press</p>
              <h2 id="updated-week-heading">This Week</h2>
            </header>
            <ul className="updated-week-list">
              {updatedThisWeek.map((series) => (
                <li key={series.slug}>
                  <Link to={`/read/s/${series.slug}`} className="updated-week-row">
                    <CoverImage
                      className="updated-week-thumb"
                      url={series.coverUrl}
                      gradient={series.coverGradient}
                    />
                    <span className="updated-week-copy">
                      <span className="updated-week-badge">Updated</span>
                      <span className="updated-week-title">{series.title}</span>
                      <span className="updated-week-meta">
                        {series.creatorName} · {series.contentLanguage.toUpperCase()}
                      </span>
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </section>
      ) : null}

    </div>
  );
}

function CreatorCard({ creator }: { creator: CreatorSpotlight }) {
  const initial = creator.name.trim().charAt(0).toUpperCase() || '?';
  return (
    <Link to={`/read/s/${creator.firstSlug}`} className="creator-spotlight-card">
      <span className="creator-avatar" aria-hidden="true">
        {initial}
      </span>
      <span className="creator-spotlight-copy">
        <span className="creator-chip-name">{creator.name}</span>
        <span className="creator-chip-count">
          {creator.count} series
        </span>
        {creator.genres.length > 0 ? (
          <span className="creator-genres">
            {creator.genres.map((genre) => (
              <span key={genre} className="creator-genre">
                {genre}
              </span>
            ))}
          </span>
        ) : null}
      </span>
    </Link>
  );
}

function BharatIcon({ name }: { name: (typeof BHARAT_POINTS)[number]['icon'] }) {
  const common = {
    viewBox: '0 0 32 32',
    fill: 'none',
    'aria-hidden': true as const,
    className: 'bharat-icon',
  };
  if (name === 'thunder') {
    return (
      <svg {...common}>
        <path d="M18 3 7 18h8l-2 11 12-16h-8l1-10Z" fill="currentColor" />
      </svg>
    );
  }
  if (name === 'language') {
    return (
      <svg {...common}>
        <circle cx="16" cy="16" r="11" stroke="currentColor" strokeWidth="2" />
        <path d="M5 16h22M16 5c3 3.2 4.5 7 4.5 11S19 23.8 16 27c-3-3.2-4.5-7-4.5-11S13 8.2 16 5Z" stroke="currentColor" strokeWidth="2" />
      </svg>
    );
  }
  return (
    <svg {...common}>
      <rect x="5" y="8" width="22" height="16" rx="2" stroke="currentColor" strokeWidth="2" />
      <path d="M5 13h22M12 21h4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}
