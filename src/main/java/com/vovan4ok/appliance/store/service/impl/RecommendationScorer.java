package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Recommendation;
import com.vovan4ok.appliance.store.model.RecommendationReason;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The recommendation rule, as a pure function (no database, no Spring), so it is easy to test and explain:
 * <pre>
 * score = 3.0 * bought together   (orders with both products / the most such orders of any candidate, 0..1)
 *       + 1.5 * same category
 *       + 1.0 * price closeness   (1 - price difference / higher price, 0..1)
 *       + 0.5 * same brand
 * </pre>
 * Bought together weighs most: it is real customer behaviour, the others only describe the products.
 */
public final class RecommendationScorer {

    static final double BOUGHT_TOGETHER_WEIGHT = 3.0;
    static final double SAME_CATEGORY_WEIGHT = 1.5;
    static final double PRICE_WEIGHT = 1.0;
    static final double SAME_BRAND_WEIGHT = 0.5;
    /** Price closeness from which SIMILAR_PRICE is given as a reason (the prices differ by at most 20 %). */
    static final double SIMILAR_PRICE_THRESHOLD = 0.8;

    private RecommendationScorer() {
    }

    /**
     * @param boughtTogether    approved orders containing both the viewed product and the candidate
     * @param maxBoughtTogether the highest such count of all candidates (0 when nothing was bought together)
     */
    public static Recommendation score(Appliance viewed, Appliance candidate,
                                       long boughtTogether, long maxBoughtTogether) {
        double score = 0;
        List<RecommendationReason> reasons = new ArrayList<>();

        if (boughtTogether > 0 && maxBoughtTogether > 0) {
            score += BOUGHT_TOGETHER_WEIGHT * boughtTogether / maxBoughtTogether;
            reasons.add(RecommendationReason.BOUGHT_TOGETHER);
        }
        if (viewed.getCategory() != null && viewed.getCategory() == candidate.getCategory()) {
            score += SAME_CATEGORY_WEIGHT;
            reasons.add(RecommendationReason.SAME_CATEGORY);
        }
        double closeness = priceCloseness(viewed.getPrice(), candidate.getPrice());
        score += PRICE_WEIGHT * closeness;
        if (closeness >= SIMILAR_PRICE_THRESHOLD) {
            reasons.add(RecommendationReason.SIMILAR_PRICE);
        }
        if (sameBrand(viewed, candidate)) {
            score += SAME_BRAND_WEIGHT;
            reasons.add(RecommendationReason.SAME_BRAND);
        }
        return new Recommendation(candidate, round(score), List.copyOf(reasons));
    }

    /** 1 for equal prices, towards 0 the further apart they are; 0 when a price is missing. */
    static double priceCloseness(BigDecimal a, BigDecimal b) {
        if (a == null || b == null || a.signum() <= 0 || b.signum() <= 0) {
            return 0;
        }
        BigDecimal higher = a.max(b);
        return 1 - a.subtract(b).abs().divide(higher, 4, RoundingMode.HALF_UP).doubleValue();
    }

    private static boolean sameBrand(Appliance a, Appliance b) {
        return a.getManufacturer() != null && b.getManufacturer() != null
                && a.getManufacturer().getId() != null
                && Objects.equals(a.getManufacturer().getId(), b.getManufacturer().getId());
    }

    private static double round(double score) {
        return Math.round(score * 100) / 100.0;
    }
}
