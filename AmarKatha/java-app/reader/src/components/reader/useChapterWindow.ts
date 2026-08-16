import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchChapter, trackChapterView } from '../../api/home';
import type { ChapterReader, ChapterSummary } from '../../types';

export type DirectionLoad = {
  chapterNumber: number;
  title: string;
  error?: string;
} | null;

type Options = {
  seriesSlug: string;
  playlist: ChapterSummary[];
  entryChapter: ChapterReader;
};

export function useChapterWindow({ seriesSlug, playlist, entryChapter }: Options) {
  const navigate = useNavigate();
  const entryIndex = Math.max(
    0,
    playlist.findIndex((c) => c.slug === entryChapter.chapterSlug),
  );

  const [loadedBySlug, setLoadedBySlug] = useState<Record<string, ChapterReader>>(() => ({
    [entryChapter.chapterSlug]: entryChapter,
  }));
  const [windowStart, setWindowStart] = useState(entryIndex);
  const [windowEnd, setWindowEnd] = useState(entryIndex);
  const [activeSlug, setActiveSlug] = useState(entryChapter.chapterSlug);
  const [loadingPrev, setLoadingPrev] = useState<DirectionLoad>(null);
  const [loadingNext, setLoadingNext] = useState<DirectionLoad>(null);
  const [jumping, setJumping] = useState(false);

  const loadingPrevRef = useRef(false);
  const loadingNextRef = useRef(false);
  const requestGen = useRef(0);
  const activeSlugRef = useRef(activeSlug);
  const trackedRef = useRef(new Set<string>([entryChapter.chapterSlug]));
  const pendingScrollAnchor = useRef<{ slug: string; top: number } | null>(null);
  const windowStartRef = useRef(windowStart);
  const windowEndRef = useRef(windowEnd);

  activeSlugRef.current = activeSlug;
  windowStartRef.current = windowStart;
  windowEndRef.current = windowEnd;

  const orderedChapters: ChapterReader[] = [];
  for (let i = windowStart; i <= windowEnd; i++) {
    const summary = playlist[i];
    const loaded = summary ? loadedBySlug[summary.slug] : undefined;
    if (loaded) {
      orderedChapters.push(loaded);
    }
  }

  const atStart = windowStart <= 0;
  const atEnd = windowEnd >= playlist.length - 1;

  const syncUrl = useCallback(
    (slug: string) => {
      const path = `/read/s/${seriesSlug}/c/${slug}`;
      if (window.location.pathname !== path) {
        navigate(path, { replace: true });
      }
    },
    [navigate, seriesSlug],
  );

  const markActive = useCallback(
    (slug: string) => {
      if (activeSlugRef.current === slug) {
        return;
      }
      setActiveSlug(slug);
      syncUrl(slug);
      if (!trackedRef.current.has(slug)) {
        trackedRef.current.add(slug);
        void trackChapterView(seriesSlug, slug);
      }
    },
    [seriesSlug, syncUrl],
  );

  useLayoutEffect(() => {
    const anchor = pendingScrollAnchor.current;
    if (!anchor) {
      return;
    }
    pendingScrollAnchor.current = null;
    const el = document.getElementById(`chapter-${anchor.slug}`);
    if (!el) {
      return;
    }
    const newTop = el.getBoundingClientRect().top;
    window.scrollBy(0, newTop - anchor.top);
  }, [windowStart, orderedChapters.length]);

  const loadPrev = useCallback(async () => {
    const start = windowStartRef.current;
    if (loadingPrevRef.current || start <= 0) {
      return;
    }
    const target = playlist[start - 1];
    const currentFirst = playlist[start];
    if (!target || !currentFirst) {
      return;
    }
    loadingPrevRef.current = true;
    const gen = ++requestGen.current;
    setLoadingPrev({
      chapterNumber: target.chapterNumber,
      title: target.title?.trim() || `Chapter ${target.chapterNumber}`,
    });

    try {
      const data = await fetchChapter(seriesSlug, target.slug);
      if (gen !== requestGen.current) {
        return;
      }
      const firstEl = document.getElementById(`chapter-${currentFirst.slug}`);
      if (firstEl) {
        pendingScrollAnchor.current = {
          slug: currentFirst.slug,
          top: firstEl.getBoundingClientRect().top,
        };
      }
      setLoadedBySlug((prev) => ({ ...prev, [data.chapterSlug]: data }));
      setWindowStart(start - 1);
      setLoadingPrev(null);
    } catch (e) {
      if (gen !== requestGen.current) {
        return;
      }
      setLoadingPrev({
        chapterNumber: target.chapterNumber,
        title: target.title?.trim() || `Chapter ${target.chapterNumber}`,
        error: e instanceof Error ? e.message : 'Could not open chapter',
      });
    } finally {
      loadingPrevRef.current = false;
    }
  }, [playlist, seriesSlug]);

  const loadNext = useCallback(async () => {
    const end = windowEndRef.current;
    if (loadingNextRef.current || end >= playlist.length - 1) {
      return;
    }
    const target = playlist[end + 1];
    if (!target) {
      return;
    }
    loadingNextRef.current = true;
    const gen = ++requestGen.current;
    setLoadingNext({
      chapterNumber: target.chapterNumber,
      title: target.title?.trim() || `Chapter ${target.chapterNumber}`,
    });

    try {
      const data = await fetchChapter(seriesSlug, target.slug);
      if (gen !== requestGen.current) {
        return;
      }
      setLoadedBySlug((prev) => ({ ...prev, [data.chapterSlug]: data }));
      setWindowEnd(end + 1);
      setLoadingNext(null);
    } catch (e) {
      if (gen !== requestGen.current) {
        return;
      }
      setLoadingNext({
        chapterNumber: target.chapterNumber,
        title: target.title?.trim() || `Chapter ${target.chapterNumber}`,
        error: e instanceof Error ? e.message : 'Could not open chapter',
      });
    } finally {
      loadingNextRef.current = false;
    }
  }, [playlist, seriesSlug]);

  const retryPrev = useCallback(() => {
    if (!loadingPrev?.error) {
      return;
    }
    loadingPrevRef.current = false;
    setLoadingPrev(null);
    void loadPrev();
  }, [loadPrev, loadingPrev?.error]);

  const retryNext = useCallback(() => {
    if (!loadingNext?.error) {
      return;
    }
    loadingNextRef.current = false;
    setLoadingNext(null);
    void loadNext();
  }, [loadNext, loadingNext?.error]);

  const jumpTo = useCallback(
    async (slug: string) => {
      const index = playlist.findIndex((c) => c.slug === slug);
      if (index < 0) {
        return;
      }

      const start = windowStartRef.current;
      const end = windowEndRef.current;
      if (loadedBySlug[slug] && index >= start && index <= end) {
        markActive(slug);
        document
          .getElementById(`chapter-${slug}`)
          ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        return;
      }

      setJumping(true);
      const gen = ++requestGen.current;
      try {
        let data = loadedBySlug[slug];
        if (!data) {
          data = await fetchChapter(seriesSlug, slug);
        }
        if (gen !== requestGen.current) {
          return;
        }
        setLoadedBySlug((prev) => ({ ...prev, [data!.chapterSlug]: data! }));
        setWindowStart(index);
        setWindowEnd(index);
        setLoadingPrev(null);
        setLoadingNext(null);
        loadingPrevRef.current = false;
        loadingNextRef.current = false;
        markActive(slug);
        requestAnimationFrame(() => {
          document.getElementById(`chapter-${slug}`)?.scrollIntoView({ block: 'start' });
        });
      } catch {
        // keep current feed; caller can retry from sheet
      } finally {
        if (gen === requestGen.current) {
          setJumping(false);
        }
      }
    },
    [loadedBySlug, markActive, playlist, seriesSlug],
  );

  useEffect(() => {
    const slugs = orderedChapters.map((c) => c.chapterSlug);
    const sections = slugs
      .map((slug) => document.getElementById(`chapter-${slug}`))
      .filter((el): el is HTMLElement => el != null);

    if (sections.length === 0) {
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio);
        const top = visible[0];
        if (!top) {
          return;
        }
        const slug = (top.target as HTMLElement).dataset.chapterSlug;
        if (slug) {
          markActive(slug);
        }
      },
      {
        root: null,
        rootMargin: '-20% 0px -55% 0px',
        threshold: [0, 0.1, 0.25, 0.5, 0.75, 1],
      },
    );

    sections.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
    // Intentionally depend on window bounds + loaded count, not the array identity
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [windowStart, windowEnd, orderedChapters.length, markActive]);

  return {
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
  };
}

export function formatChapterNumber(n: number): string {
  return Number.isInteger(n) ? String(n) : String(n);
}
