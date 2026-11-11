package com.vovan4ok.appliance.store.service.impl;

import java.util.List;

/**
 * What the model answers: the candidates in its preferred order, each with a short reason. Untrusted: it is
 * checked against the real candidates before use ({@link AiRecommendationRanker#apply}).
 */
public record AiRanking(List<Pick> picks) {

    public record Pick(long id, String why) {
    }
}
