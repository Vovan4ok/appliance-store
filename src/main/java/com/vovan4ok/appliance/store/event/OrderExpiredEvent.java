package com.vovan4ok.appliance.store.event;

/**
 * Published when a pending order was not approved in time and went back to the client's cart.
 *
 * @param order       snapshot of the expired order (for the client's email)
 * @param cartOrderId the order that now holds the items (the client's open cart, or the order itself)
 */
public record OrderExpiredEvent(OrderEmailData order, Long cartOrderId) {
}
