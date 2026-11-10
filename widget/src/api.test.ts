import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { ApiError, clampLimit, fetchRecommendations, storeUrl } from './api';
import { product, RECOMMENDATIONS_URL, server, STORE } from './test/server';

describe('fetchRecommendations', () => {
  it('asks for the viewed product and the limit', async () => {
    let seen: Request | undefined;
    server.use(
      http.get(RECOMMENDATIONS_URL, ({ request }) => {
        seen = request;
        return HttpResponse.json([product()]);
      }),
    );

    const items = await fetchRecommendations(STORE, { applianceId: 8, limit: 3 });

    expect(items).toEqual([product()]);
    const url = new URL(seen?.url ?? '');
    expect(url.searchParams.get('applianceId')).toBe('8');
    expect(url.searchParams.get('limit')).toBe('3');
    expect(seen?.headers.get('Accept')).toBe('application/json');
  });

  it('never sends cookies', async () => {
    // Node's Request ignores `credentials`, so check what the widget passes to fetch
    const fetchSpy = vi.spyOn(globalThis, 'fetch');

    await fetchRecommendations(STORE, {});

    expect(fetchSpy.mock.calls[0]?.[1]).toMatchObject({ credentials: 'omit' });
  });

  it('leaves out applianceId for best sellers and uses the default limit', async () => {
    let url: URL | undefined;
    server.use(
      http.get(RECOMMENDATIONS_URL, ({ request }) => {
        url = new URL(request.url);
        return HttpResponse.json([]);
      }),
    );

    await fetchRecommendations(`${STORE}/`, {});

    expect(url?.searchParams.has('applianceId')).toBe(false);
    expect(url?.searchParams.get('limit')).toBe('4');
  });

  it('throws an ApiError with Retry-After on 429', async () => {
    server.use(
      http.get(RECOMMENDATIONS_URL, () =>
        HttpResponse.json({ status: 429 }, { status: 429, headers: { 'Retry-After': '42' } }),
      ),
    );

    const error = await fetchRecommendations(STORE, {}).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 429, retryAfterSeconds: 42 });
  });

  it('throws an ApiError without a wait time on other errors', async () => {
    server.use(http.get(RECOMMENDATIONS_URL, () => new HttpResponse(null, { status: 500 })));

    await expect(fetchRecommendations(STORE, {})).rejects.toMatchObject({
      status: 500,
      retryAfterSeconds: undefined,
    });
  });

  it('rejects an answer that is not a list', async () => {
    server.use(http.get(RECOMMENDATIONS_URL, () => HttpResponse.json({ items: [] })));

    await expect(fetchRecommendations(STORE, {})).rejects.toThrow('Unexpected answer');
  });
});

describe('clampLimit', () => {
  it.each([
    [0, 1],
    [-3, 1],
    [5, 5],
    [5.7, 5],
    [50, 12],
    [Number.NaN, 4],
  ])('%s → %s', (input, expected) => {
    expect(clampLimit(input)).toBe(expected);
  });
});

describe('storeUrl', () => {
  it('joins the store address and a path', () => {
    expect(storeUrl('https://store.test/', '/uploads/a.png')).toBe('https://store.test/uploads/a.png');
    expect(storeUrl('https://store.test', 'uploads/a.png')).toBe('https://store.test/uploads/a.png');
  });

  it('keeps absolute URLs', () => {
    expect(storeUrl('https://store.test', 'https://cdn.test/a.png')).toBe('https://cdn.test/a.png');
  });
});
