package com.vovan4ok.appliance.store.model;

/**
 * Why a product was recommended. The API returns these codes, not text, so every client (the widget, the shop)
 * shows them in its own language.
 */
public enum RecommendationReason {
    /** Was in the same approved orders as the product being viewed. */
    BOUGHT_TOGETHER,
    SAME_CATEGORY,
    /** Price within 20 % of the product being viewed. */
    SIMILAR_PRICE,
    SAME_BRAND,
    /** No product given: the most sold products overall. */
    BEST_SELLER
}
