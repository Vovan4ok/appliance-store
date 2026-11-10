import { createRoot, type Root } from 'react-dom/client';
import { RecommendationsWidget } from './RecommendationsWidget';
import styles from './styles.css?inline';
import type { Recommendation } from './types';

export const TAG_NAME = 'appliance-recommendations';
export const SELECT_EVENT = 'recommendation-select';

const ATTRIBUTES = ['store-url', 'appliance-id', 'limit', 'lang', 'heading'] as const;

// The store is where this script was loaded from, so <script src="https://store/widget/store-widget.js">
// needs no extra setting. document.currentScript only exists while the script first runs, so read it now.
const scriptOrigin = (() => {
  const script = document.currentScript;
  return script instanceof HTMLScriptElement && script.src ? new URL(script.src).origin : window.location.origin;
})();

/**
 * <appliance-recommendations appliance-id="8" limit="4" lang="uk"></appliance-recommendations>
 *
 * A web component, so it works on any page (plain HTML, WordPress, Angular...). React renders inside a
 * shadow root: the page's CSS can't break the widget and the widget's CSS can't leak into the page. The page
 * can still restyle it through the --asw-* CSS variables.
 *
 * Fires a "recommendation-select" event (detail: the product) when the visitor opens a product.
 */
export class ApplianceRecommendationsElement extends HTMLElement {
  static readonly observedAttributes = ATTRIBUTES;

  #root: Root | null = null;

  connectedCallback() {
    const shadow = this.shadowRoot ?? this.attachShadow({ mode: 'open' });
    const style = document.createElement('style');
    style.textContent = styles;
    const mount = document.createElement('div');
    shadow.replaceChildren(style, mount);
    this.#root = createRoot(mount);
    this.#render();
  }

  disconnectedCallback() {
    // Also aborts a running request (the hook's cleanup)
    this.#root?.unmount();
    this.#root = null;
  }

  attributeChangedCallback() {
    this.#render();
  }

  #render() {
    if (!this.#root) {
      return;
    }
    this.#root.render(
      <RecommendationsWidget
        store={this.getAttribute('store-url') ?? scriptOrigin}
        applianceId={parseId(this.getAttribute('appliance-id'))}
        limit={parseNumber(this.getAttribute('limit'))}
        lang={this.getAttribute('lang') ?? document.documentElement.lang}
        heading={this.getAttribute('heading') ?? undefined}
        onProductSelect={(product: Recommendation) =>
          this.dispatchEvent(new CustomEvent(SELECT_EVENT, { detail: product, bubbles: true }))
        }
      />,
    );
  }
}

/** A positive whole number, or undefined (then the widget shows the best sellers). */
function parseId(value: string | null): number | undefined {
  const id = parseNumber(value);
  return id !== undefined && Number.isInteger(id) && id > 0 ? id : undefined;
}

function parseNumber(value: string | null): number | undefined {
  if (value === null || value.trim() === '') {
    return undefined;
  }
  const number = Number(value);
  return Number.isFinite(number) ? number : undefined;
}

// Loading the script twice (e.g. two embeds on one page) must not throw
if (!customElements.get(TAG_NAME)) {
  customElements.define(TAG_NAME, ApplianceRecommendationsElement);
}

declare global {
  interface HTMLElementTagNameMap {
    [TAG_NAME]: ApplianceRecommendationsElement;
  }
}
