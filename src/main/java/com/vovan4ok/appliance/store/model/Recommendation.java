package com.vovan4ok.appliance.store.model;

import java.util.List;

/**
 * One recommended product with its score (higher is better) and the reasons that produced the score.
 *
 * @param explanation a short "why" written by the AI re-ranking, or null when the scoring order is used
 */
public record Recommendation(Appliance appliance, double score, List<RecommendationReason> reasons,
                             String explanation) {

    public Recommendation(Appliance appliance, double score, List<RecommendationReason> reasons) {
        this(appliance, score, reasons, null);
    }

    public Recommendation withExplanation(String text) {
        return new Recommendation(appliance, score, reasons, text);
    }
}
