import { useId } from 'react';
import { ApiError } from '../api';
import { messages, type Lang } from '../i18n';
import type { Recommendation, RecommendationsState } from '../types';
import { ProductCard, type LinkTarget } from './ProductCard';

export interface RecommendationListProps {
  state: RecommendationsState;
  heading: string;
  lang: Lang;
  store: string;
  /** How many placeholder cards to show while loading */
  placeholders: number;
  onRetry: () => void;
  onSelect?: ((product: Recommendation) => void) | undefined;
  linkTarget?: LinkTarget | undefined;
}

/**
 * Shows any state of the recommendations: placeholders, the cards, or an error with a retry button.
 * Renders nothing when there is nothing to recommend, so the host page has no empty box.
 */
export function RecommendationList({
  state,
  heading,
  lang,
  store,
  placeholders,
  onRetry,
  onSelect,
  linkTarget,
}: RecommendationListProps) {
  const headingId = useId();
  const t = messages(lang);

  if (state.status === 'success' && state.items.length === 0) {
    return null;
  }

  return (
    <section className="asw" aria-labelledby={headingId} aria-busy={state.status === 'loading'}>
      <h2 id={headingId} className="asw__heading">
        {heading}
      </h2>

      {state.status === 'loading' && (
        <>
          <p className="asw-visually-hidden" role="status">
            {t.loading}
          </p>
          <ul className="asw__grid" aria-hidden="true">
            {Array.from({ length: placeholders }, (_, i) => (
              <li key={i} className="asw-skeleton" />
            ))}
          </ul>
        </>
      )}

      {state.status === 'error' && (
        <div className="asw__status" role="alert">
          <span>{errorMessage(state.error, lang)}</span>
          <button type="button" className="asw__retry" onClick={onRetry}>
            {t.retry}
          </button>
        </div>
      )}

      {state.status === 'success' && (
        <ul className="asw__grid">
          {state.items.map((product) => (
            <ProductCard
              key={product.id}
              product={product}
              lang={lang}
              store={store}
              onSelect={onSelect}
              linkTarget={linkTarget}
            />
          ))}
        </ul>
      )}
    </section>
  );
}

function errorMessage(error: Error, lang: Lang): string {
  const t = messages(lang);
  if (error instanceof ApiError && error.status === 429 && error.retryAfterSeconds !== undefined) {
    return t.rateLimited(error.retryAfterSeconds);
  }
  return t.error;
}
