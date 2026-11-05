package com.vovan4ok.appliance.store.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.UpdateValidatorMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * One workflow per submitted order (workflow id {@code order-<id>}): waits for an employee's approval, reminds
 * the employees when nobody decides in time and finally sends the order back to the client's cart. The timers are
 * durable: they survive app restarts and deployments, because Temporal stores them, not the app.
 */
@WorkflowInterface
public interface OrderApprovalWorkflow {

    String TASK_QUEUE = "order-approval";

    static String workflowId(long orderId) {
        return "order-" + orderId;
    }

    /**
     * @return how it ended: APPROVED, EXPIRED (back in the cart) or CLOSED (approved, deleted or changed
     *         outside the workflow, so there was nothing left to do)
     */
    @WorkflowMethod
    String run(long orderId, long reminderAfterSeconds, long expireAfterSeconds);

    /**
     * Approves the order (stock check + approval in one database transaction). Synchronous for the caller: it
     * returns when the order is approved, or fails with the reason (e.g. not enough stock), so the employee sees
     * the result right away. The workflow keeps waiting after a failed attempt.
     */
    @UpdateMethod
    void approve();

    /** Rejects an approval before it starts once the workflow has decided. */
    @UpdateValidatorMethod(updateName = "approve")
    void validateApprove();

    /** PENDING, APPROVING, APPROVED, EXPIRED or CLOSED; shown in the Temporal UI and handy for debugging. */
    @QueryMethod
    String state();
}
