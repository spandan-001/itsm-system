import { useEffect, useRef } from 'react';

/** Calls `refresh` every `ms` while enabled and the tab is visible (polling, not push). */
export function useAutoRefresh(refresh, ms, enabled = true) {
  const latest = useRef(refresh);
  useEffect(() => {
    latest.current = refresh;
  }, [refresh]);
  useEffect(() => {
    if (!enabled) return undefined;
    const timer = setInterval(() => {
      if (document.visibilityState === 'visible') latest.current();
    }, ms);
    return () => clearInterval(timer);
  }, [ms, enabled]);
}
