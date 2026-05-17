package com.vovan4ok.appliance.store.event;

/**
 * Published after an employee approves a submitted order.
 */
public record OrderApprovedEvent(OrderEmailData order) {
}