package com.vovan4ok.appliance.store.event;

/**
 * Published after a client submits their cart for review.
 */
public record OrderSubmittedEvent(OrderEmailData order) {
}