import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { product, STORE } from '../test/server';
import { ProductCard } from './ProductCard';

function renderCard(props: Partial<Parameters<typeof ProductCard>[0]> = {}) {
  return render(
    <ul>
      <ProductCard product={product()} lang="en" store={STORE} {...props} />
    </ul>,
  );
}

describe('ProductCard', () => {
  it('shows the product, its price and why it is recommended', () => {
    renderCard({ product: product({ reasons: ['BOUGHT_TOGETHER', 'SAME_BRAND', 'NEW_CODE'] }) });

    expect(screen.getByText('Dishwasher')).toBeInTheDocument();
    expect(screen.getByText('Bosch')).toBeInTheDocument();
    expect(screen.getByText('SMS4')).toBeInTheDocument();
    expect(screen.getByText(/699\.99/)).toBeInTheDocument();
    const reasons = screen.getByRole('list', { name: 'Why we recommend it' });
    expect(reasons).toHaveTextContent('Often bought together');
    expect(reasons).toHaveTextContent('Same brand');
    expect(reasons).not.toHaveTextContent('NEW_CODE');
  });

  it('shows the AI explanation when the store sends one', () => {
    renderCard({ product: product({ explanation: 'Dries what your washer cleans' }) });

    expect(screen.getByText('Dries what your washer cleans')).toHaveClass('asw-card__why');
  });

  it('shows no explanation line without one', () => {
    const { container } = renderCard({ product: product({ explanation: null }) });

    expect(container.querySelector('.asw-card__why')).toBeNull();
  });

  it('speaks Ukrainian', () => {
    renderCard({ lang: 'uk' });

    expect(screen.getByText('Часто купують разом')).toBeInTheDocument();
    // ICU versions differ: "699,99 ₴" (Node 26) or "699,99 грн"
    expect(screen.getByText(/699,99\s?(₴|грн)/)).toBeInTheDocument();
  });

  it('links to the product in the store, in a new tab', () => {
    renderCard({ product: product({ name: 'Coffee & Tea maker' }) });

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', `${STORE}/shop?name=Coffee%20%26%20Tea%20maker`);
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener');
  });

  it('opens in the same tab on the store’s own pages', () => {
    renderCard({ linkTarget: '_self' });

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('target', '_self');
    expect(link).not.toHaveAttribute('rel');
  });

  it('reports the click', async () => {
    const onSelect = vi.fn();
    renderCard({ onSelect });

    // jsdom doesn't navigate, so clicking the link is safe
    await userEvent.click(screen.getByRole('link'));

    expect(onSelect).toHaveBeenCalledWith(product());
  });

  it('loads the logo from the store, and falls back to the initial when it fails', () => {
    const { container } = renderCard({ product: product({ manufacturerLogo: '/uploads/bosch.png' }) });

    const logo = container.querySelector('img');
    expect(logo).toHaveAttribute('src', `${STORE}/uploads/bosch.png`);
    expect(logo).toHaveAttribute('alt', '');

    fireEvent.error(logo as HTMLImageElement);

    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('.asw-card__logo--initial')).toHaveTextContent('B');
  });

  it('leaves out what the product does not have', () => {
    renderCard({ product: product({ model: null, manufacturer: null, price: null, reasons: [] }) });

    expect(screen.queryByRole('list', { name: 'Why we recommend it' })).toBeNull();
    expect(screen.getByRole('link')).toHaveTextContent(/^\?Dishwasher$/);
  });
});
