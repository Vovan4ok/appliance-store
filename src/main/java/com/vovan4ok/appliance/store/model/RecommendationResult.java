package com.vovan4ok.appliance.store.model;

import java.util.List;

/**
 * Recommendations, best first.
 *
 * @param aiPending true when the AI re-ranking is still being prepared: the scoring order is returned for now
 *                  and clients should not cache it for long
 */
public record RecommendationResult(List<Recommendation> items, boolean aiPending) {
}
