import { useEffect, useState } from 'react';
import { fetchMe } from '../api/auth';
import {
  EngagementAuthError,
  fetchFollowState,
  fetchPendingFollow,
  followSeries,
  readerLoginWithFollowIntent,
  unfollowSeries,
} from '../api/engagement';

type Props = {
  seriesSlug: string;
};

export default function FollowButton({ seriesSlug }: Props) {
  const [followed, setFollowed] = useState(false);
  const [authenticated, setAuthenticated] = useState<boolean | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoaded(false);
    setError(null);

    Promise.all([fetchMe(), fetchFollowState(seriesSlug)])
      .then(async ([me, state]) => {
        if (cancelled) {
          return;
        }
        setAuthenticated(me.authenticated);
        let nextFollowed = state.followed;
        if (me.authenticated && !state.followed) {
          try {
            const pending = await fetchPendingFollow();
            if (
              !cancelled &&
              pending.seriesSlug &&
              pending.seriesSlug.toLowerCase() === seriesSlug.toLowerCase()
            ) {
              const confirmed = await followSeries(seriesSlug);
              nextFollowed = confirmed.followed;
            }
          } catch {
            // pending follow is best-effort; button still usable
          }
        }
        if (!cancelled) {
          setFollowed(nextFollowed);
          setLoaded(true);
        }
      })
      .catch((e: Error) => {
        if (!cancelled) {
          setError(e.message);
          setLoaded(true);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [seriesSlug]);

  async function handleToggle() {
    setError(null);
    if (authenticated === false) {
      window.location.href = readerLoginWithFollowIntent(
        seriesSlug,
        `/read/s/${seriesSlug}`,
      );
      return;
    }
    if (busy) {
      return;
    }
    setBusy(true);
    try {
      const next = followed
        ? await unfollowSeries(seriesSlug)
        : await followSeries(seriesSlug);
      setFollowed(next.followed);
      setAuthenticated(true);
    } catch (e) {
      if (e instanceof EngagementAuthError) {
        window.location.href = readerLoginWithFollowIntent(
          seriesSlug,
          `/read/s/${seriesSlug}`,
        );
        return;
      }
      setError(e instanceof Error ? e.message : 'Could not update follow');
    } finally {
      setBusy(false);
    }
  }

  if (!loaded) {
    return (
      <button type="button" className="btn btn-secondary follow-btn" disabled aria-busy="true">
        Follow
      </button>
    );
  }

  const label = followed ? 'Following' : 'Follow';

  return (
    <div className="follow-control">
      <button
        type="button"
        className={`btn follow-btn${followed ? ' follow-btn--on' : ' btn-primary'}`}
        onClick={() => void handleToggle()}
        disabled={busy}
        aria-pressed={followed}
        aria-busy={busy}
        aria-label={
          authenticated === false
            ? 'Sign in to follow this series'
            : followed
              ? 'Unfollow this series'
              : 'Follow this series'
        }
      >
        {busy ? 'Saving…' : label}
      </button>
      {error && (
        <p className="follow-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
