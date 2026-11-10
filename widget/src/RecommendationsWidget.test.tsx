import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { RecommendationsWidget } from './RecommendationsWidget';
import { product, RECOMMENDATIONS_URL, server, STORE } from './test/server';

describe('RecommendationsWidget', () => {
  it('shows placeholders while loading, then the products', async () => {
    render(<RecommendationsWidget store={STORE} applianceId={8} limit={3} />);

    expect(screen.getByRole('status')).toHaveTextContent('Loading recommendations…');
    expect(screen.getByRole('region', { name: 'You may also like' })).toHaveAttribute('aria-busy', 'true');
    expect(document.querySelectorAll('.asw-skeleton')).toHaveLength(3);

    expect(await screen.findByText('Dishwasher')).toBeInTheDocument();
    expect(screen.queryByRole('status')).toBeNull();
    expect(screen.getByRole('region')).toHaveAttribute('aria-busy', 'false');
  });

  it('is titled "Best sellers" without a product, and takes a custom heading', async () => {
    const { rerender } = render(<RecommendationsWidget store={STORE} />);
    expect(await screen.findByRole('heading', { name: 'Best sellers' })).toBeInTheDocument();

    rerender(<RecommendationsWidget store={STORE} heading="Our picks" />);
    expect(screen.getByRole('heading', { name: 'Our picks' })).toBeInTheDocument();
  });

  it('renders nothing when there is nothing to recommend', async () => {
    let answered = false;
    server.use(
      http.get(RECOMMENDATIONS_URL, () => {
        answered = true;
        return HttpResponse.json([]);
      }),
    );
    const { container } = render(<RecommendationsWidget store={STORE} applianceId={8} />);

    await waitFor(() => {
      expect(answered).toBe(true);
      expect(container).toBeEmptyDOMElement();
    });
  });

  it('shows an error with a retry button that loads again', async () => {
    server.use(http.get(RECOMMENDATIONS_URL, () => new HttpResponse(null, { status: 500 }), { once: true }));
    render(<RecommendationsWidget store={STORE} applianceId={8} />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load recommendations.');

    await userEvent.click(screen.getByRole('button', { name: 'Try again' }));

    expect(await screen.findByText('Dishwasher')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('says how long to wait when rate limited, in the visitor’s language', async () => {
    server.use(
      http.get(RECOMMENDATIONS_URL, () => new HttpResponse(null, { status: 429, headers: { 'Retry-After': '30' } })),
    );
    render(<RecommendationsWidget store={STORE} applianceId={8} lang="uk" />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Забагато запитів. Спробуйте ще раз через 30 с.');
  });

  it('loads again for a new product, and a slow old answer never replaces the new one', async () => {
    server.use(
      http.get(RECOMMENDATIONS_URL, async ({ request }) => {
        const id = new URL(request.url).searchParams.get('applianceId');
        if (id === '1') {
          await delay(100);
          return HttpResponse.json([product({ id: 101, name: 'Old answer' })]);
        }
        return HttpResponse.json([product({ id: 102, name: 'New answer' })]);
      }),
    );
    const { rerender } = render(<RecommendationsWidget store={STORE} applianceId={1} />);

    rerender(<RecommendationsWidget store={STORE} applianceId={2} />);

    expect(await screen.findByText('New answer')).toBeInTheDocument();
    await delay(150);
    expect(screen.queryByText('Old answer')).toBeNull();
    expect(screen.getByText('New answer')).toBeInTheDocument();
  });
});

