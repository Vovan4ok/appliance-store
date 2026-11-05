package com.vovan4ok.appliance.store.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

/**
 * Workflow code must be deterministic (Temporal replays it to rebuild its state after a restart): no clocks,
 * random numbers, I/O or Spring beans here. Time comes from {@link Workflow#currentTimeMillis()}, everything
 * else goes through the activities.
 */
public class OrderApprovalWorkflowImpl implements OrderApprovalWorkflow {

    private final OrderApprovalActivities activities = Workflow.newActivityStub(OrderApprovalActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofMinutes(1))
                            .setMaximumAttempts(10)
                            .setDoNotRetry(OrderApprovalActivities.INSUFFICIENT_STOCK,
                                    OrderApprovalActivities.NOT_PENDING)
                            .build())
                    .build());

    private long orderId;
    private String state = "PENDING";
    private boolean approving;

    @Override
    public String run(long orderId, long reminderAfterSeconds, long expireAfterSeconds) {
        this.orderId = orderId;
        long start = Workflow.currentTimeMillis();

        if (waitForApproval(start + reminderAfterSeconds * 1000)) {
            return finish("APPROVED");
        }
        if (!activities.remindEmployees(orderId, reminderAfterSeconds, expireAfterSeconds - reminderAfterSeconds)) {
            return finish("CLOSED");
        }
        if (waitForApproval(start + expireAfterSeconds * 1000)) {
            return finish("APPROVED");
        }
        return finish(activities.returnToCart(orderId) ? "EXPIRED" : "CLOSED");
    }

    /** Sleeps until approved or until the deadline; true if approved. */
    private boolean waitForApproval(long deadlineMillis) {
        Duration timeout = Duration.ofMillis(Math.max(0, deadlineMillis - Workflow.currentTimeMillis()));
        Workflow.await(timeout, () -> "APPROVED".equals(state));
        // The timer may fire while an approval is running: let it finish before deciding
        Workflow.await(() -> !approving);
        return "APPROVED".equals(state);
    }

    private String finish(String outcome) {
        state = outcome;
        // Never complete while an update handler is still running
        Workflow.await(Workflow::isEveryHandlerFinished);
        return outcome;
    }

    @Override
    public void validateApprove() {
        if (!"PENDING".equals(state) || approving) {
            throw new IllegalStateException("Order is not waiting for approval (state: " + state + ")");
        }
    }

    @Override
    public void approve() {
        approving = true;
        try {
            activities.approveOrder(orderId);
            state = "APPROVED";
        } finally {
            approving = false;
        }
    }

    @Override
    public String state() {
        return approving ? "APPROVING" : state;
    }
}
