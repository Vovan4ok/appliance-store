package com.vovan4ok.appliance.store.listener;

import com.vovan4ok.appliance.store.event.OrderApprovedEvent;
import com.vovan4ok.appliance.store.event.OrderEmailData;
import com.vovan4ok.appliance.store.event.OrderSubmittedEvent;
import com.vovan4ok.appliance.store.event.OrderUnapprovedEvent;
import com.vovan4ok.appliance.store.metrics.StoreMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderMetricsListenerTest {

    @Mock
    StoreMetrics metrics;

    @InjectMocks
    OrderMetricsListener listener;

    private static OrderEmailData order(BigDecimal total) {
        return new OrderEmailData(1L, "Client", "client@test.com", total, List.of());
    }

    @Test
    void submittedOrder_isCounted() {
        listener.onOrderSubmitted(new OrderSubmittedEvent(order(BigDecimal.TEN)));

        verify(metrics).orderSubmitted();
    }

    @Test
    void approvedOrder_isCountedWithItsTotal() {
        listener.onOrderApproved(new OrderApprovedEvent(order(new BigDecimal("199.99"))));

        verify(metrics).orderApproved(new BigDecimal("199.99"));
    }

    @Test
    void unapprovedOrder_isCounted() {
        listener.onOrderUnapproved(new OrderUnapprovedEvent(1L));

        verify(metrics).orderUnapproved();
    }
}
