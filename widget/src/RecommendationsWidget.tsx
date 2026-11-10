import { clampLimit, DEFAULT_LIMIT } from './api';
import type { LinkTarget } from './components/ProductCard';
import { RecommendationList } from './components/RecommendationList';
import { messages, resolveLang } from './i18n';
import type { Recommendation } from './types';
import { useRecommendations } from './useRecommendations';

export interface RecommendationsWidgetProps {
  /** The store's address, e.g. https://appliance-store-xeibhsf7uq-lm.a.run.app */
  store: string;
  /** The product being viewed; without it: the store's best sellers */
  applianceId?: number | undefined;
  /** How many products, 1 to 12 (default 4) */
  limit?: number | undefined;
  /** "en" or "uk" (default "en") */
  lang?: string | undefined;
  /** Replaces the default heading */
  heading?: string | undefined;
  /** Called when the visitor opens a product, e.g. for the host page's analytics */
  onProductSelect?: ((product: Recommendation) => void) | undefined;
  /** Where product links open: "_blank" (default, a new tab) or "_self" */
  linkTarget?: LinkTarget | undefined;
}

/** The whole widget for React apps: loads the data and shows it. */
export function RecommendationsWidget({
  store,
  applianceId,
  limit = DEFAULT_LIMIT,
  lang,
  heading,
  onProductSelect,
  linkTarget,
}: RecommendationsWidgetProps) {
  const language = resolveLang(lang);
  const size = clampLimit(limit);
  const { retry, ...state } = useRecommendations(store, { applianceId, limit: size });
  const t = messages(language);

  return (
    <RecommendationList
      state={state}
      heading={heading ?? (applianceId === undefined ? t.bestSellers : t.heading)}
      lang={language}
      store={store}
      placeholders={size}
      onRetry={retry}
      onSelect={onProductSelect}
      linkTarget={linkTarget}
    />
  );
}
