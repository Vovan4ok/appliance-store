import type { Recommendation } from './types';

export const MAX_LIMIT = 12;
export const DEFAULT_LIMIT = 4;

/** An HTTP error from the store API; on 429 it says how long to wait. */
export class ApiError extends Error {
  readonly status: number;
  readonly retryAfterSeconds: number | undefined;

  constructor(status: number, retryAfterSeconds?: number) {
    super(`Store API answered ${String(status)}`);
    this.name = 'ApiError';
    this.status = status;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

export interface RecommendationQuery {
  /** The product being viewed; without it the store returns its best sellers */
  applianceId?: number | undefined;
  limit?: number | undefined;
  /** Language of the AI explanations: "en" (default) or "uk" */
  lang?: string | undefined;
}

/** Joins the store's address and a path from the API ("/uploads/x.png"); absolute URLs stay as they are. */
export function storeUrl(base: string, path: string): string {
  if (/^https?:\/\//i.test(path)) {
    return path;
  }
  return base.replace(/\/+$/, '') + (path.startsWith('/') ? path : `/${path}`);
}

export async function fetchRecommendations(
  base: string,
  { applianceId, limit = DEFAULT_LIMIT, lang = 'en' }: RecommendationQuery,
  signal?: AbortSignal,
): Promise<Recommendation[]> {
  const params = new URLSearchParams({ limit: String(clampLimit(limit)), lang });
  if (applianceId !== undefined) {
    params.set('applianceId', String(applianceId));
  }

  const response = await fetch(`${storeUrl(base, '/api/v1/recommendations')}?${params.toString()}`, {
    signal,
    // A "simple" CORS request (no custom headers): the browser needs no preflight
    headers: { Accept: 'application/json' },
    // The data is public; never send the visitor's cookies for the store, even from the store's own pages
    credentials: 'omit',
  });

  if (!response.ok) {
    const retryAfter = Number(response.headers.get('Retry-After'));
    throw new ApiError(response.status, retryAfter > 0 ? retryAfter : undefined);
  }
  const body: unknown = await response.json();
  if (!Array.isArray(body)) {
    throw new Error('Unexpected answer from the store API');
  }
  return body as Recommendation[];
}

export function clampLimit(limit: number): number {
  if (!Number.isFinite(limit)) {
    return DEFAULT_LIMIT;
  }
  return Math.min(MAX_LIMIT, Math.max(1, Math.trunc(limit)));
}
