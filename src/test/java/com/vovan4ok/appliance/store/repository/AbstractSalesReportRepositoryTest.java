package com.vovan4ok.appliance.store.repository;

import com.vovan4ok.appliance.store.model.report.OrderPipeline;
import com.vovan4ok.appliance.store.model.report.RevenueShare;
import com.vovan4ok.appliance.store.model.report.StageTotals;
import com.vovan4ok.appliance.store.model.report.TopProduct;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static com.vovan4ok.appliance.store.model.Category.BIG;
import static com.vovan4ok.appliance.store.model.Category.SMALL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The report queries against the seed data, the same on every database (subclasses: H2 and PostgreSQL).
 * Expected values computed by hand from the seed orders:
 * <pre>
 * approved 1: 8 (1299.99 Samsung BIG), 18 (699.99 Samsung BIG), 27 (649.99 Bosch BIG)          = 2649.97
 * approved 2: 46 (1299.99 LG BIG), 64 (399.99 Philips SMALL), 77 (699.99 Dyson SMALL)            = 2399.97
 * approved 3: 35 (799.99 Bosch BIG), 40 (799.99 Samsung BIG), 50 (2 x 59.99 Philips SMALL),
 *             58 (34.99 Philips SMALL)                                                           = 1754.95
 * pending 4-6 = 3047.99 + 1679.96 + 1539.97, carts 7-8 = 1199.98 + 719.94 (incl. 3 x 89.99)
 * </pre>
 */
abstract class AbstractSalesReportRepositoryTest {

    /** Compare amounts by value: 6804.89 equals 6804.8900. */
    private static final RecursiveComparisonConfiguration BY_VALUE = RecursiveComparisonConfiguration.builder()
            .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
            .build();

    @Autowired
    SalesReportRepository repository;

    @Test
    void pipeline_countsEveryStageInOneQuery() {
        assertThat(repository.orderPipeline())
                .usingRecursiveComparison(BY_VALUE)
                .isEqualTo(new OrderPipeline(
                        new StageTotals(2, 8, money("1919.92")),
                        new StageTotals(3, 10, money("6267.92")),
                        new StageTotals(3, 11, money("6804.89"))));
    }

    @Test
    void revenueByCategory_onlyApprovedOrders_withShareOfTotal() {
        assertThat(repository.revenueByCategory())
                .usingRecursiveFieldByFieldElementComparator(BY_VALUE)
                .containsExactly(
                        new RevenueShare<>(BIG, 3, 6, money("5549.94"), money("81.6")),
                        new RevenueShare<>(SMALL, 2, 5, money("1254.95"), money("18.4")));
    }

    @Test
    void revenueByBrand_largestFirst_sharesAddUpTo100() {
        var brands = repository.revenueByBrand();

        assertThat(brands)
                .usingRecursiveFieldByFieldElementComparator(BY_VALUE)
                .containsExactly(
                        new RevenueShare<>("Samsung", 2, 3, money("2799.97"), money("41.1")),
                        new RevenueShare<>("Bosch", 2, 2, money("1449.98"), money("21.3")),
                        new RevenueShare<>("LG", 1, 1, money("1299.99"), money("19.1")),
                        new RevenueShare<>("Dyson", 1, 1, money("699.99"), money("10.3")),
                        new RevenueShare<>("Philips", 2, 4, money("554.96"), money("8.2")));
        assertThat(brands.stream().map(RevenueShare::sharePercent).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100.0");
        // Printed as is on the page: one decimal on every database (H2 once returned 41.1000...0)
        assertThat(brands).extracting(share -> share.sharePercent().scale()).containsOnly(1);
    }

    @Test
    void topProducts_rankedWithinEachCategory_tiesShareARank() {
        // BIG: every product sold once; 8 and 46 tie at 1299.99 (rank 1), 35 and 40 at 799.99 (rank 3)
        // SMALL: the kettle (50) sold twice, so it leads although the vacuum (77) earned more
        assertThat(repository.topProductsPerCategory(3))
                .usingRecursiveFieldByFieldElementComparator(BY_VALUE)
                .containsExactly(
                        new TopProduct(BIG, 1, 8, "Side-by-Side Refrigerator", 1, money("1299.99")),
                        new TopProduct(BIG, 1, 46, "OLED TV", 1, money("1299.99")),
                        new TopProduct(BIG, 3, 35, "Pyrolytic Oven", 1, money("799.99")),
                        new TopProduct(BIG, 3, 40, "Split Air Conditioner", 1, money("799.99")),
                        new TopProduct(SMALL, 1, 50, "Electric Kettle", 2, money("119.98")),
                        new TopProduct(SMALL, 2, 77, "Cordless Vacuum", 1, money("699.99")),
                        new TopProduct(SMALL, 3, 64, "Espresso Machine", 1, money("399.99")));
    }

    @Test
    void topProducts_limitIsApplied() {
        assertThat(repository.topProductsPerCategory(1))
                .extracting(TopProduct::applianceId)
                .containsExactly(8L, 46L, 50L);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
