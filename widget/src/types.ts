/**
 * One item of GET /api/v1/recommendations; mirrors RecommendationResponse.java.
 */
export interface Recommendation {
  id: number;
  name: string;
  model: string | null;
  category: string | null;
  manufacturer: string | null;
  /** Path on the store server (e.g. /uploads/logo.png), or null */
  manufacturerLogo: string | null;
  price: number | null;
  score: number;
  /** Reason codes; may contain codes newer than this widget, which are then not shown */
  reasons: string[];
}

export const REASON_CODES = [
  'BOUGHT_TOGETHER',
  'SAME_CATEGORY',
  'SIMILAR_PRICE',
  'SAME_BRAND',
  'BEST_SELLER',
] as const;

export type ReasonCode = (typeof REASON_CODES)[number];

export function isReasonCode(code: string): code is ReasonCode {
  return (REASON_CODES as readonly string[]).includes(code);
}

/** What the widget shows: still loading, the products, or why they could not be loaded. */
export type RecommendationsState =
  | { status: 'loading' }
  | { status: 'success'; items: Recommendation[] }
  | { status: 'error'; error: Error };
