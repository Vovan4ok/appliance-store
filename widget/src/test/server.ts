import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import type { Recommendation } from '../types';

/** The fake store every test talks to (MSW answers the real fetch calls, nothing is mocked in the code). */
export const STORE = 'https://store.test';
export const RECOMMENDATIONS_URL = `${STORE}/api/v1/recommendations`;

export function product(overrides: Partial<Recommendation> = {}): Recommendation {
  return {
    id: 18,
    name: 'Dishwasher',
    model: 'SMS4',
    category: 'BIG',
    manufacturer: 'Bosch',
    manufacturerLogo: null,
    price: 699.99,
    score: 4.5,
    reasons: ['BOUGHT_TOGETHER'],
    ...overrides,
  };
}

export const server = setupServer(
  http.get(RECOMMENDATIONS_URL, () => HttpResponse.json([product()])),
);
