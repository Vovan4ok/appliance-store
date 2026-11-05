package com.vovan4ok.appliance.store.event;

/**
 * Published when an employee takes back the approval of an order (its stock is returned).
 */
public record OrderUnapprovedEvent(Long orderId) {
}
