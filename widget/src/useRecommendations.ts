import { useCallback, useEffect, useState } from 'react';
import { fetchRecommendations, type RecommendationQuery } from './api';
import type { RecommendationsState } from './types';

interface Settled {
  key: string;
  state: RecommendationsState;
}

/**
 * Loads recommendations and reloads them whenever the inputs change.
 *
 * - A request that is no longer needed (inputs changed, component removed) is aborted, so a slow old answer
 *   can never overwrite a newer one.
 * - The result is stored together with the inputs it belongs to; while they differ, the state is "loading".
 *   That derives the loading state instead of setting it inside the effect (one render less, no flash of
 *   old products for a new product).
 */
export function useRecommendations(
  base: string,
  { applianceId, limit }: RecommendationQuery,
): RecommendationsState & { retry: () => void } {
  const [attempt, setAttempt] = useState(0);
  const [settled, setSettled] = useState<Settled | null>(null);
  const key = JSON.stringify([base, applianceId, limit, attempt]);

  useEffect(() => {
    const controller = new AbortController();
    fetchRecommendations(base, { applianceId, limit }, controller.signal).then(
      (items) => {
        setSettled({ key, state: { status: 'success', items } });
      },
      (error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }
        setSettled({
          key,
          state: { status: 'error', error: error instanceof Error ? error : new Error(String(error)) },
        });
      },
    );
    return () => {
      controller.abort();
    };
  }, [base, applianceId, limit, key]);

  const retry = useCallback(() => {
    setAttempt((n) => n + 1);
  }, []);

  const state: RecommendationsState = settled?.key === key ? settled.state : { status: 'loading' };
  return { ...state, retry };
}
