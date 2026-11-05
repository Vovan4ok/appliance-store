package com.vovan4ok.appliance.store.metrics;

import com.vovan4ok.appliance.store.repository.ApplianceRepository;
import com.vovan4ok.appliance.store.repository.OrdersRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreGaugesTest {

    @Mock
    OrdersRepository ordersRepository;
    @Mock
    ApplianceRepository applianceRepository;

    @Test
    void gauges_readTheCurrentCountsOnEveryScrape() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new StoreGauges(ordersRepository, applianceRepository).bindTo(registry);
        when(ordersRepository.countByApprovedIsNull()).thenReturn(4L, 3L);
        when(applianceRepository.countOutOfStock()).thenReturn(2L);

        assertThat(registry.get(StoreMetrics.PENDING_ORDERS).gauge().value()).isEqualTo(4);
        // Not cached: the next scrape sees the new number
        assertThat(registry.get(StoreMetrics.PENDING_ORDERS).gauge().value()).isEqualTo(3);
        assertThat(registry.get(StoreMetrics.OUT_OF_STOCK).gauge().value()).isEqualTo(2);
    }
}
