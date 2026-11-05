package com.vovan4ok.appliance.store.listener;

import com.vovan4ok.appliance.store.event.OrderApprovedEvent;
import com.vovan4ok.appliance.store.event.OrderSubmittedEvent;
import com.vovan4ok.appliance.store.event.OrderUnapprovedEvent;
import com.vovan4ok.appliance.store.metrics.StoreMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Counts order events only after their transaction commits, so a rolled-back approval
 * (e.g. not enough stock) never shows up in the metrics.
 */
@Component
@RequiredArgsConstructor
public class OrderMetricsListener {

    private final StoreMetrics metrics;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderSubmitted(OrderSubmittedEvent event) {
        metrics.orderSubmitted();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderApproved(OrderApprovedEvent event) {
        metrics.orderApproved(event.order().getTotalAmount());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderUnapproved(OrderUnapprovedEvent event) {
        metrics.orderUnapproved();
    }
}
