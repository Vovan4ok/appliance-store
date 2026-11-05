package com.vovan4ok.appliance.store.metrics;

import com.vovan4ok.appliance.store.repository.ApplianceRepository;
import com.vovan4ok.appliance.store.repository.OrdersRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Current-state numbers, read from the database each time Prometheus scrapes (every 15 s; two COUNT queries).
 * Spring Boot binds every {@link MeterBinder} bean to the registry at startup.
 */
@Component
@RequiredArgsConstructor
public class StoreGauges implements MeterBinder {

    private final OrdersRepository ordersRepository;
    private final ApplianceRepository applianceRepository;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(StoreMetrics.PENDING_ORDERS, ordersRepository, OrdersRepository::countByApprovedIsNull)
                .description("Submitted orders waiting for an employee's approval")
                .register(registry);
        Gauge.builder(StoreMetrics.OUT_OF_STOCK, applianceRepository, ApplianceRepository::countOutOfStock)
                .description("Appliances with no stock left")
                .register(registry);
    }
}
