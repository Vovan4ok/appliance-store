package com.vovan4ok.appliance.store.workflow;

import io.temporal.activity.ActivityInterface;

/**
 * The steps the workflow delegates to the app (database and email). Temporal retries them on failure, so each one
 * is safe to run twice: it re-reads the order and does nothing when the order has moved on.
 */
@ActivityInterface
public interface OrderApprovalActivities {

    /** Failure type for "not enough stock" (not retried; shown to the employee). */
    String INSUFFICIENT_STOCK = "InsufficientStock";
    /** Failure type for "the order is gone or no longer waiting" (not retried). */
    String NOT_PENDING = "OrderNotPending";

    /** Stock check + approval in one transaction (same rules as approving without Temporal). */
    void approveOrder(long orderId);

    /**
     * Emails all employees that the order is still waiting.
     *
     * @return false if the order is no longer pending (no email sent)
     */
    boolean remindEmployees(long orderId, long waitingSeconds, long secondsLeft);

    /**
     * Sends the order back to the client's cart and emails the client.
     *
     * @return false if the order is no longer pending (nothing changed)
     */
    boolean returnToCart(long orderId);
}
