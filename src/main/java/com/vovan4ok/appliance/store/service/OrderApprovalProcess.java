package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.exception.InsufficientStockException;

/**
 * Submitting and approving orders. With {@code app.temporal.enabled=true} (prod) every submitted order runs as a
 * Temporal workflow with a reminder and an expiry timer; otherwise (dev/test) it calls {@link OrderService}
 * directly, so local development needs no Temporal server.
 */
public interface OrderApprovalProcess {

    /** The client sends the cart: the order becomes pending and waits for approval. */
    void submit(Long orderId);

    /**
     * An employee approves a pending order (stock is taken).
     *
     * @throws InsufficientStockException  when an appliance doesn't have enough stock
     * @throws IllegalArgumentException    when the order doesn't exist or isn't waiting for approval
     */
    void approve(Long orderId);

    /** An employee takes an approval back: stock is returned and the order waits for approval again. */
    void unapprove(Long orderId);
}
