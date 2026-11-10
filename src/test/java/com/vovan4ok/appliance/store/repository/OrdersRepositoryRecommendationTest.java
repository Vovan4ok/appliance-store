package com.vovan4ok.appliance.store.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The recommendation queries against the Flyway seed data (V2__seed_data.sql): approved orders 1-3 count,
 * pending orders (approved null) and carts (approved false) don't.
 */
@DataJpaTest
class OrdersRepositoryRecommendationTest {

    @Autowired
    OrdersRepository ordersRepository;

    @Autowired
    ApplianceRepository applianceRepository;

    @Test
    void boughtTogether_countsOnlyApprovedOrdersWithBothProducts() {
        // Order 1 (approved): 8, 18, 27
        assertThat(ordersRepository.countBoughtTogether(8L))
                .containsExactlyInAnyOrder(new ProductCount(18L, 1L), new ProductCount(27L, 1L));
    }

    @Test
    void boughtTogether_ignoresCarts() {
        // 77 is in approved order 2 (46, 64, 77) and in a cart with 80
        assertThat(ordersRepository.countBoughtTogether(77L))
                .containsExactlyInAnyOrder(new ProductCount(46L, 1L), new ProductCount(64L, 1L));
    }

    @Test
    void boughtTogether_ignoresPendingOrders() {
        // 19 is only in pending order 4
        assertThat(ordersRepository.countBoughtTogether(19L)).isEmpty();
    }

    @Test
    void unitsSold_sumsQuantities_mostSoldFirst() {
        // Order 3 has two units of 50; every other product of the approved orders sold once
        assertThat(ordersRepository.countUnitsSold())
                .first().isEqualTo(new ProductCount(50L, 2L));
        assertThat(ordersRepository.countUnitsSold())
                .extracting(ProductCount::applianceId)
                .containsExactly(50L, 8L, 18L, 27L, 35L, 40L, 46L, 58L, 64L, 77L);
    }

    @Test
    void inStock_excludesSoldOutProducts() {
        var appliance = applianceRepository.findById(8L).orElseThrow();
        appliance.setStock(0);
        applianceRepository.saveAndFlush(appliance);

        assertThat(applianceRepository.findByStockGreaterThan(0))
                .isNotEmpty()
                .noneMatch(a -> a.getId().equals(8L))
                .allMatch(a -> a.getStock() > 0);
    }
}
