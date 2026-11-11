import { describe, expect, it } from 'vitest';
import { formatPrice, reasonLabels, resolveLang } from './i18n';

describe('i18n', () => {
  it.each([
    ['uk', 'uk'],
    ['uk-UA', 'uk'],
    ['UK', 'uk'],
    ['en-GB', 'en'],
    ['de', 'en'],
    ['', 'en'],
    [null, 'en'],
    [undefined, 'en'],
  ])('resolveLang(%s) → %s', (value, expected) => {
    expect(resolveLang(value)).toBe(expected);
  });

  it('translates the reason codes and skips codes it does not know', () => {
    expect(reasonLabels(['BOUGHT_TOGETHER', 'SOMETHING_NEW', 'SAME_BRAND'], 'en')).toEqual([
      'Often bought together',
      'Same brand',
    ]);
    expect(reasonLabels(['BEST_SELLER'], 'uk')).toEqual(['Хіт продажів']);
  });

  it('formats prices in hryvnias for each language', () => {
    expect(formatPrice(699.99, 'en')).toMatch(/UAH\s?699\.99/);
    // ICU versions differ: "699,99 ₴" (Node 26) or "699,99 грн"
    expect(formatPrice(699.99, 'uk')).toMatch(/699,99\s?(₴|грн)/);
    expect(formatPrice(null, 'en')).toBeNull();
  });
});
