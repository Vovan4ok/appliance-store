package com.vovan4ok.appliance.store.model.dto.api;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Recommendation;
import com.vovan4ok.appliance.store.model.RecommendationReason;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public view of a recommended product: only what a product card on any website needs. The stock count,
 * descriptions and manufacturer details stay behind the authenticated API.
 */
@Getter
@Builder
public class RecommendationResponse {

    private Long id;
    private String name;
    private String model;
    private String category;
    private String manufacturer;
    /** Path of the manufacturer's logo on this server (e.g. /uploads/...), or null. */
    private String manufacturerLogo;
    private BigDecimal price;
    private double score;
    /** {@link RecommendationReason} codes; clients translate them. */
    private List<String> reasons;
    /** One short sentence from the AI re-ranking, in the requested language; null without it. */
    private String explanation;

    public static RecommendationResponse from(Recommendation recommendation) {
        Appliance a = recommendation.appliance();
        return RecommendationResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .model(a.getModel())
                .category(a.getCategory() != null ? a.getCategory().name() : null)
                .manufacturer(a.getManufacturer() != null ? a.getManufacturer().getName() : null)
                .manufacturerLogo(a.getManufacturer() != null ? a.getManufacturer().getLogoPath() : null)
                .price(a.getPrice())
                .score(recommendation.score())
                .reasons(recommendation.reasons().stream().map(Enum::name).toList())
                .explanation(recommendation.explanation())
                .build();
    }
}
