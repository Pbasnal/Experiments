import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchHome } from '../api/home';
import { deriveCreators, type CreatorSpotlight } from './HomePage';

export default function CreatorsPage() {
  const [creators, setCreators] = useState<CreatorSpotlight[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchHome()
      .then((home) => {
        if (!cancelled) setCreators(deriveCreators(home.recentlyUpdated ?? []));
      })
      .catch((e: Error) => {
        if (!cancelled) setError(e.message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section className="creators-page" aria-labelledby="all-creators-heading">
      <div className="section-shell">
        <p className="section-kicker kicker-orange">Creators</p>
        <h1 id="all-creators-heading">Invite-only voices</h1>
        <p className="creators-page-lead">
          Everyone publishing on Amar Katha right now. Open a name to read their series.
        </p>
        {error ? (
          <p role="alert">{error}</p>
        ) : creators === null ? (
          <p role="status">Loading creators…</p>
        ) : creators.length === 0 ? (
          <p>Creators will show up here once a series is listed.</p>
        ) : (
          <ul className="creators-list">
            {creators.map((creator) => (
              <li key={creator.name}>
                <Link to={`/read/s/${creator.firstSlug}`} className="creator-spotlight-card">
                  <span className="creator-avatar" aria-hidden="true">
                    {creator.name.trim().charAt(0).toUpperCase() || '?'}
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
              </li>
            ))}
          </ul>
        )}
        <p className="creators-page-back">
          <Link to="/">Back to home</Link>
        </p>
      </div>
    </section>
  );
}
