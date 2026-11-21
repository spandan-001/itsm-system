import { useCallback, useEffect, useState } from 'react';

export function useApiData(request, path) {
  const [result, setResult] = useState(null);
  const [revision, setRevision] = useState(0);
  const reload = useCallback(() => setRevision(value => value + 1), []);
  useEffect(() => {
    if (!path) return;
    const controller = new AbortController();
    request(path, { signal: controller.signal })
      .then(data => setResult({ path, request, revision, data }))
      .catch(error => {
        if (!controller.signal.aborted) setResult({ path, request, revision, error });
      });
    return () => controller.abort();
  }, [request, path, revision]);

  const current = result?.path === path && result.request === request && result.revision === revision;
  return { data: current ? result.data : undefined, error: current ? result.error : undefined, reload };
}
