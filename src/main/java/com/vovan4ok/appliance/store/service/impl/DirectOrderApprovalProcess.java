package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.service.OrderApprovalProcess;
import com.vovan4ok.appliance.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Without Temporal (dev/test): straight calls, no reminders and no expiry.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.temporal.enabled", havingValue = "false", matchIfMissing = true)
public class DirectOrderApprovalProcess implements OrderApprovalProcess {

    private final OrderService orderService;

    @Override
    public void submit(Long orderId) {
        orderService.submitCart(orderId);
    }

    @Override
    public void approve(Long orderId) {
        orderService.approve(orderId);
    }

    @Override
    public void unapprove(Long orderId) {
        orderService.unapprove(orderId);
    }
}
