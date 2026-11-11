import { isReasonCode, type ReasonCode } from './types';

export type Lang = 'en' | 'uk';

interface Messages {
  heading: string;
  bestSellers: string;
  loading: string;
  error: string;
  rateLimited: (seconds: number) => string;
  retry: string;
  reasonsLabel: string;
  reasons: Record<ReasonCode, string>;
}

// Same two languages as the store itself (messages.properties / messages_uk.properties)
const MESSAGES: Record<Lang, Messages> = {
  en: {
    heading: 'You may also like',
    bestSellers: 'Best sellers',
    loading: 'Loading recommendations…',
    error: 'Could not load recommendations.',
    rateLimited: (s) => `Too many requests. Try again in ${String(s)} s.`,
    retry: 'Try again',
    reasonsLabel: 'Why we recommend it',
    reasons: {
      BOUGHT_TOGETHER: 'Often bought together',
      SAME_CATEGORY: 'Same category',
      SIMILAR_PRICE: 'Similar price',
      SAME_BRAND: 'Same brand',
      BEST_SELLER: 'Best seller',
    },
  },
  uk: {
    heading: 'Вам також може сподобатися',
    bestSellers: 'Хіти продажів',
    loading: 'Завантаження рекомендацій…',
    error: 'Не вдалося завантажити рекомендації.',
    rateLimited: (s) => `Забагато запитів. Спробуйте ще раз через ${String(s)} с.`,
    retry: 'Спробувати ще раз',
    reasonsLabel: 'Чому ми це радимо',
    reasons: {
      BOUGHT_TOGETHER: 'Часто купують разом',
      SAME_CATEGORY: 'Та сама категорія',
      SIMILAR_PRICE: 'Схожа ціна',
      SAME_BRAND: 'Той самий бренд',
      BEST_SELLER: 'Хіт продажів',
    },
  },
};

const LOCALES: Record<Lang, string> = { en: 'en-US', uk: 'uk-UA' };

/** "uk", "uk-UA" → uk; anything else → en. */
export function resolveLang(value: string | null | undefined): Lang {
  return value?.toLowerCase().startsWith('uk') ? 'uk' : 'en';
}

export function messages(lang: Lang): Messages {
  return MESSAGES[lang];
}

/** Labels for the reason codes this widget knows; unknown (newer) codes are skipped. */
export function reasonLabels(codes: readonly string[], lang: Lang): string[] {
  return codes.filter(isReasonCode).map((code) => MESSAGES[lang].reasons[code]);
}

/** The store's prices are in hryvnias. */
export function formatPrice(price: number | null, lang: Lang): string | null {
  if (price === null) {
    return null;
  }
  return new Intl.NumberFormat(LOCALES[lang], { style: 'currency', currency: 'UAH' }).format(price);
}
