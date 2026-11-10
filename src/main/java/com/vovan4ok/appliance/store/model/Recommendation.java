package com.vovan4ok.appliance.store.model;

import java.util.List;

/**
 * One recommended product with its score (higher is better) and the reasons that produced the score.
 */
public record Recommendation(Appliance appliance, double score, List<RecommendationReason> reasons) {
}
