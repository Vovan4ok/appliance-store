package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.Recommendation;

import java.util.List;

public interface RecommendationService {

    int MAX_LIMIT = 12;

    /**
     * Products to recommend, best first; never the viewed product itself and never an out-of-stock one.
     *
     * @param applianceId the product being viewed, or null for the best sellers (e.g. on a home page)
     * @param limit       how many, clamped to 1..{@value #MAX_LIMIT}
     * @throws IllegalArgumentException when the viewed product does not exist
     */
    List<Recommendation> recommend(Long applianceId, int limit);
}
