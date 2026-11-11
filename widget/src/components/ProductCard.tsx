import { useState } from 'react';
import { storeUrl } from '../api';
import { formatPrice, messages, reasonLabels, type Lang } from '../i18n';
import type { Recommendation } from '../types';

export interface ProductCardProps {
  product: Recommendation;
  lang: Lang;
  /** The store's address: for the logo and the link */
  store: string;
  onSelect?: ((product: Recommendation) => void) | undefined;
  /** "_blank" (default): a new tab, for other websites; "_self" for the store's own pages */
  linkTarget?: LinkTarget | undefined;
}

export type LinkTarget = '_blank' | '_self';

/** One recommended product. Pure: everything comes from the props. */
export function ProductCard({ product, lang, store, onSelect, linkTarget = '_blank' }: ProductCardProps) {
  const reasons = reasonLabels(product.reasons, lang);
  const price = formatPrice(product.price, lang);
  const href = `${storeUrl(store, '/shop')}?name=${encodeURIComponent(product.name)}`;

  return (
    <li className="asw-card">
      <a
        className="asw-card__link"
        href={href}
        target={linkTarget}
        rel={linkTarget === '_blank' ? 'noopener' : undefined}
        onClick={() => onSelect?.(product)}
      >
        <BrandLogo store={store} logo={product.manufacturerLogo} brand={product.manufacturer} />
        {product.manufacturer && <span className="asw-card__brand">{product.manufacturer}</span>}
        <span className="asw-card__name">{product.name}</span>
        {product.model && <span className="asw-card__model">{product.model}</span>}
        {product.explanation && <span className="asw-card__why">{product.explanation}</span>}
        {reasons.length > 0 && (
          <ul className="asw-card__reasons" aria-label={messages(lang).reasonsLabel}>
            {reasons.map((reason) => (
              <li key={reason} className="asw-chip">
                {reason}
              </li>
            ))}
          </ul>
        )}
        {price && <span className="asw-card__price">{price}</span>}
      </a>
    </li>
  );
}

interface BrandLogoProps {
  store: string;
  logo: string | null;
  brand: string | null;
}

/** The manufacturer's logo, or its initial when there is none or it fails to load. Decorative: the brand
 * name is in the card as text. */
function BrandLogo({ store, logo, brand }: BrandLogoProps) {
  const [failed, setFailed] = useState(false);

  if (logo && !failed) {
    return (
      <img
        className="asw-card__logo"
        src={storeUrl(store, logo)}
        alt=""
        loading="lazy"
        onError={() => {
          setFailed(true);
        }}
      />
    );
  }
  return (
    <span className="asw-card__logo asw-card__logo--initial" aria-hidden="true">
      {brand?.charAt(0).toUpperCase() ?? '?'}
    </span>
  );
}
