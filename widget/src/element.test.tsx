import { waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { SELECT_EVENT, TAG_NAME } from './element';
import { product, RECOMMENDATIONS_URL, server, STORE } from './test/server';
import type { Recommendation } from './types';

/** Adds the element to the page like an HTML author would, and returns it with queries for its shadow root. */
function embed(attributes: Record<string, string>) {
  const element = document.createElement(TAG_NAME);
  for (const [name, value] of Object.entries(attributes)) {
    element.setAttribute(name, value);
  }
  document.body.append(element);
  const shadow = element.shadowRoot;
  if (!shadow) {
    throw new Error('no shadow root');
  }
  return { element, shadow, view: within(shadow as unknown as HTMLElement) };
}

function recordRequests() {
  const urls: URL[] = [];
  server.use(
    http.get(RECOMMENDATIONS_URL, ({ request }) => {
      const url = new URL(request.url);
      urls.push(url);
      return HttpResponse.json([product({ id: Number(url.searchParams.get('applianceId') ?? 0) + 100 })]);
    }),
  );
  return urls;
}

describe('<appliance-recommendations>', () => {
  it('is registered as a custom element', () => {
    expect(customElements.get(TAG_NAME)).toBeDefined();
  });

  it('renders the widget inside its shadow root, with its own styles', async () => {
    const { shadow, view } = embed({ 'store-url': STORE, 'appliance-id': '8', lang: 'uk' });

    expect(await view.findByText('Dishwasher')).toBeInTheDocument();
    expect(view.getByRole('heading')).toHaveTextContent('Вам також може сподобатися');
    expect(shadow.querySelector('style')?.textContent).toBeDefined();
    // Nothing leaks into the page itself
    expect(document.body.querySelector('.asw')).toBeNull();
  });

  it('passes the attributes to the API', async () => {
    const urls = recordRequests();
    embed({ 'store-url': STORE, 'appliance-id': '8', limit: '2' });

    await waitFor(() => {
      expect(urls).toHaveLength(1);
    });
    expect(urls[0]?.searchParams.get('applianceId')).toBe('8');
    expect(urls[0]?.searchParams.get('limit')).toBe('2');
  });

  it('shows best sellers when appliance-id is missing or not a valid id', async () => {
    const urls = recordRequests();
    embed({ 'store-url': STORE, 'appliance-id': 'abc' });

    await waitFor(() => {
      expect(urls).toHaveLength(1);
    });
    expect(urls[0]?.searchParams.has('applianceId')).toBe(false);
  });

  it('loads again when an attribute changes (e.g. a single-page app shows another product)', async () => {
    const urls = recordRequests();
    const { element } = embed({ 'store-url': STORE, 'appliance-id': '8' });
    await waitFor(() => {
      expect(urls).toHaveLength(1);
    });

    element.setAttribute('appliance-id', '9');

    await waitFor(() => {
      expect(urls.map((u) => u.searchParams.get('applianceId'))).toEqual(['8', '9']);
    });
  });

  it('fires recommendation-select with the product when one is opened', async () => {
    const { element, view } = embed({ 'store-url': STORE, 'appliance-id': '8' });
    const listener = vi.fn();
    document.addEventListener(SELECT_EVENT, listener);

    await userEvent.click(await view.findByRole('link'));

    document.removeEventListener(SELECT_EVENT, listener);
    expect(listener).toHaveBeenCalledTimes(1);
    const event = listener.mock.calls[0]?.[0] as CustomEvent<Recommendation>;
    expect(event.target).toBe(element);
    expect(event.detail).toEqual(product());
  });

  it('cleans up when removed, and renders again when added back', async () => {
    const { element, shadow, view } = embed({ 'store-url': STORE, 'appliance-id': '8' });
    await view.findByText('Dishwasher');

    element.remove();
    await waitFor(() => {
      expect(shadow.querySelector('.asw')).toBeNull();
    });

    document.body.append(element);
    expect(await view.findByText('Dishwasher')).toBeInTheDocument();
  });

  it('uses the page language when lang is not set', async () => {
    document.documentElement.lang = 'uk';
    try {
      const { view } = embed({ 'store-url': STORE });
      expect(await view.findByRole('heading', { name: 'Хіти продажів' })).toBeInTheDocument();
    } finally {
      document.documentElement.lang = '';
    }
  });
});
