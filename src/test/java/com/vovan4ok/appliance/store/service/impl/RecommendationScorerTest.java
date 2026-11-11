package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.model.Recommendation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.vovan4ok.appliance.store.model.RecommendationReason.BOUGHT_TOGETHER;
import static com.vovan4ok.appliance.store.model.RecommendationReason.SAME_BRAND;
import static com.vovan4ok.appliance.store.model.RecommendationReason.SAME_CATEGORY;
import static com.vovan4ok.appliance.store.model.RecommendationReason.SIMILAR_PRICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RecommendationScorerTest {

    private static final Manufacturer BOSCH = manufacturer(1L);
    private static final Manufacturer SAMSUNG = manufacturer(2L);

    private final Appliance viewed = appliance(1L, Category.BIG, "1000.00", BOSCH);

    @Test
    void allSignals_addUp_withTheirWeights() {
        Appliance candidate = appliance(2L, Category.BIG, "1000.00", BOSCH);

        Recommendation r = RecommendationScorer.score(viewed, candidate, 2, 2);

        // 3.0 bought together + 1.5 same category + 1.0 equal price + 0.5 same brand
        assertThat(r.score()).isEqualTo(6.0);
        assertThat(r.reasons()).containsExactly(BOUGHT_TOGETHER, SAME_CATEGORY, SIMILAR_PRICE, SAME_BRAND);
        assertThat(r.appliance()).isSameAs(candidate);
    }

    @Test
    void boughtTogether_isRelativeToTheMostBoughtTogetherCandidate() {
        Appliance candidate = appliance(2L, Category.SMALL, null, SAMSUNG);

        assertThat(RecommendationScorer.score(viewed, candidate, 1, 4).score()).isEqualTo(0.75);
        assertThat(RecommendationScorer.score(viewed, candidate, 4, 4).score()).isEqualTo(3.0);
    }

    @Test
    void nothingInCommon_scoresZero_withoutReasons() {
        Appliance candidate = appliance(2L, Category.SMALL, null, SAMSUNG);

        Recommendation r = RecommendationScorer.score(viewed, candidate, 0, 0);

        assertThat(r.score()).isZero();
        assertThat(r.reasons()).isEmpty();
    }

    @Test
    void boughtTogether_outweighsAllProductSimilarity() {
        Appliance twin = appliance(2L, Category.BIG, "1000.00", BOSCH);
        Appliance companion = appliance(3L, Category.SMALL, "1000.00", SAMSUNG);

        double twinScore = RecommendationScorer.score(viewed, twin, 0, 3).score();
        double companionScore = RecommendationScorer.score(viewed, companion, 3, 3).score();

        assertThat(companionScore).isGreaterThan(twinScore);
    }

    @Test
    void priceCloseness_fallsWithThePriceDifference() {
        assertThat(RecommendationScorer.priceCloseness(new BigDecimal("100"), new BigDecimal("100"))).isEqualTo(1.0);
        assertThat(RecommendationScorer.priceCloseness(new BigDecimal("100"), new BigDecimal("80")))
                .isCloseTo(0.8, within(0.0001));
        assertThat(RecommendationScorer.priceCloseness(new BigDecimal("50"), new BigDecimal("100")))
                .isCloseTo(0.5, within(0.0001));
        assertThat(RecommendationScorer.priceCloseness(null, new BigDecimal("100"))).isZero();
        assertThat(RecommendationScorer.priceCloseness(BigDecimal.ZERO, new BigDecimal("100"))).isZero();
    }

    @Test
    void similarPrice_isGivenFromTwentyPercentDifference() {
        Appliance within20 = appliance(2L, Category.SMALL, "800.00", SAMSUNG);
        Appliance beyond20 = appliance(3L, Category.SMALL, "799.00", SAMSUNG);

        assertThat(RecommendationScorer.score(viewed, within20, 0, 0).reasons()).containsExactly(SIMILAR_PRICE);
        assertThat(RecommendationScorer.score(viewed, beyond20, 0, 0).reasons()).isEmpty();
    }

    @Test
    void missingCategoryOrManufacturer_isNotAMatch() {
        Appliance noData = appliance(1L, null, null, null);
        Appliance candidate = appliance(2L, null, null, null);

        assertThat(RecommendationScorer.score(noData, candidate, 0, 0).reasons()).isEmpty();
    }

    @Test
    void score_isRoundedToTwoDecimals() {
        Appliance candidate = appliance(2L, Category.SMALL, null, SAMSUNG);

        assertThat(RecommendationScorer.score(viewed, candidate, 1, 3).score()).isEqualTo(1.0);
        assertThat(RecommendationScorer.score(viewed, candidate, 2, 3).score()).isEqualTo(2.0);
        assertThat(RecommendationScorer.score(viewed, candidate, 1, 7).score()).isEqualTo(0.43);
    }

    private static Appliance appliance(Long id, Category category, String price, Manufacturer manufacturer) {
        Appliance a = new Appliance();
        a.setId(id);
        a.setCategory(category);
        a.setPrice(price != null ? new BigDecimal(price) : null);
        a.setManufacturer(manufacturer);
        return a;
    }

    private static Manufacturer manufacturer(Long id) {
        Manufacturer m = new Manufacturer();
        m.setId(id);
        return m;
    }
}
