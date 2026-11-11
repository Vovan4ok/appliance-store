package com.vovan4ok.appliance.store.service.impl

import com.vovan4ok.appliance.store.model.Appliance
import com.vovan4ok.appliance.store.model.Category
import com.vovan4ok.appliance.store.model.Manufacturer
import spock.lang.Specification

import static com.vovan4ok.appliance.store.model.Category.BIG
import static com.vovan4ok.appliance.store.model.Category.SMALL
import static com.vovan4ok.appliance.store.model.RecommendationReason.BOUGHT_TOGETHER
import static com.vovan4ok.appliance.store.model.RecommendationReason.SAME_BRAND
import static com.vovan4ok.appliance.store.model.RecommendationReason.SAME_CATEGORY
import static com.vovan4ok.appliance.store.model.RecommendationReason.SIMILAR_PRICE

/**
 * The recommendation rule, one row per case:
 * score = 3.0 * bought together + 1.5 * same category + 1.0 * price closeness + 0.5 * same brand
 */
class RecommendationScorerSpec extends Specification {

    static final Manufacturer SAMSUNG = new Manufacturer(id: 1L, name: 'Samsung')
    static final Manufacturer BOSCH = new Manufacturer(id: 2L, name: 'Bosch')

    def "#scenario: score #score, reasons #reasons"() {
        given: 'a big Samsung appliance for 1000 is being viewed'
        def viewed = appliance(BIG, SAMSUNG, 1000.00)
        def candidate = appliance(category, brand, price)

        when:
        def result = RecommendationScorer.score(viewed, candidate, together, mostTogether)

        then:
        result.score() == score
        result.reasons() == reasons
        result.appliance().is(candidate)

        where:
        scenario                               | category | brand   | price   | together | mostTogether || score | reasons
        'everything in common, top pair'       | BIG      | SAMSUNG | 1000.00 | 4        | 4            || 6.0   | [BOUGHT_TOGETHER, SAME_CATEGORY, SIMILAR_PRICE, SAME_BRAND]
        'bought together half as often as top' | SMALL    | BOSCH   | 100.00  | 2        | 4            || 1.6   | [BOUGHT_TOGETHER]
        'same category, 20 % cheaper'          | BIG      | BOSCH   | 800.00  | 0        | 4            || 2.3   | [SAME_CATEGORY, SIMILAR_PRICE]
        'same category, 30 % cheaper'          | BIG      | BOSCH   | 700.00  | 0        | 4            || 2.2   | [SAME_CATEGORY]
        'same brand, a quarter of the price'   | SMALL    | SAMSUNG | 250.00  | 0        | 0            || 0.75  | [SAME_BRAND]
        'nothing in common, no price'          | SMALL    | BOSCH   | null    | 0        | 0            || 0.0   | []
    }

    def "bought together outweighs every product attribute together"() {
        given:
        def viewed = appliance(BIG, SAMSUNG, 1000.00)
        def boughtWithIt = appliance(SMALL, BOSCH, 50.00)
        def lookalike = appliance(BIG, SAMSUNG, 1000.00)

        expect: 'the top co-purchase (3.0 + ~0.05) beats same category, price and brand (1.5 + 1.0 + 0.5)'
        RecommendationScorer.score(viewed, boughtWithIt, 3, 3).score() >
                RecommendationScorer.score(viewed, lookalike, 0, 3).score()
    }

    def "price closeness of #a and #b is #closeness"() {
        expect:
        RecommendationScorer.priceCloseness(a, b) == closeness

        where:
        a       | b       || closeness
        100.00  | 100.00  || 1.0d
        100.00  | 75.00   || 0.75d
        75.00   | 100.00  || 0.75d
        1000.00 | 250.00  || 0.25d
        null    | 100.00  || 0.0d
        100.00  | 0.00    || 0.0d
        -5.00   | 100.00  || 0.0d
    }

    private static Appliance appliance(Category category, Manufacturer brand, BigDecimal price) {
        new Appliance(category: category, manufacturer: brand, price: price)
    }
}
