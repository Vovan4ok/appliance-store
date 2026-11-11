package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.Recommendation;
import com.vovan4ok.appliance.store.model.RecommendationResult;

import java.util.List;

public interface RecommendationService {

    int MAX_LIMIT = 12;

    /**
     * Products to recommend by the scoring rule alone, best first; never the viewed product itself and never an
     * out-of-stock one.
     *
     * @param applianceId the product being viewed, or null for the best sellers (e.g. on a home page)
     * @param limit       how many, clamped to 1..{@value #MAX_LIMIT}
     * @throws IllegalArgumentException when the viewed product does not exist
     */
    List<Recommendation> recommend(Long applianceId, int limit);

    /**
     * Like {@link #recommend(Long, int)}, re-ranked by the AI when it is enabled and available (with a short
     * explanation per product in {@code lang}); otherwise exactly the scoring order.
     *
     * @param lang "en" or "uk"
     */
    RecommendationResult recommend(Long applianceId, int limit, String lang);
}
