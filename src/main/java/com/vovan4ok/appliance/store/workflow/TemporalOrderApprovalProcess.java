package com.vovan4ok.appliance.store.workflow;

import com.vovan4ok.appliance.store.exception.InsufficientStockException;
import com.vovan4ok.appliance.store.model.Orders;
import com.vovan4ok.appliance.store.service.OrderApprovalProcess;
import com.vovan4ok.appliance.store.service.OrderService;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowServiceException;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.failure.ApplicationFailure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Runs the approval of every submitted order as an {@link OrderApprovalWorkflow}.
 * <p>
 * Fail-open like the Redis features: when Temporal is unreachable the shop keeps working (orders are submitted
 * and approved directly), only the reminder and expiry of the affected orders are lost; the error is logged.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.temporal.enabled", havingValue = "true")
public class TemporalOrderApprovalProcess implements OrderApprovalProcess {

    private final OrderService orderService;
    private final WorkflowClient client;
    private final Duration reminderAfter;
    private final Duration expireAfter;

    public TemporalOrderApprovalProcess(OrderService orderService,
                                        WorkflowClient client,
                                        @Value("${app.orders.approval.reminder-after}") Duration reminderAfter,
                                        @Value("${app.orders.approval.expire-after}") Duration expireAfter) {
        if (reminderAfter.isZero() || reminderAfter.isNegative() || expireAfter.compareTo(reminderAfter) <= 0) {
            throw new IllegalArgumentException("app.orders.approval: need 0 < reminder-after < expire-after");
        }
        this.orderService = orderService;
        this.client = client;
        this.reminderAfter = reminderAfter;
        this.expireAfter = expireAfter;
    }

    @Override
    public void submit(Long orderId) {
        orderService.submitCart(orderId);
        startWorkflow(orderId);
    }

    @Override
    public void approve(Long orderId) {
        try {
            client.newWorkflowStub(OrderApprovalWorkflow.class, OrderApprovalWorkflow.workflowId(orderId)).approve();
        } catch (WorkflowUpdateException e) {
            RuntimeException error = translate(e, orderId);
            boolean alreadyApproved = orderService.findById(orderId)
                    .map(order -> Boolean.TRUE.equals(order.getApproved())).orElse(false);
            // Double click: approving an approved order is a no-op, like without Temporal
            if (!(error instanceof IllegalArgumentException && alreadyApproved)) {
                throw error;
            }
        } catch (WorkflowNotFoundException e) {
            // Submitted before Temporal was enabled, or its workflow already ended: approve the classic way
            approveDirectly(orderId);
        } catch (WorkflowServiceException e) {
            log.warn("Temporal unavailable, approving order id={} directly: {}", orderId, e.getMessage());
            approveDirectly(orderId);
        }
    }

    @Override
    public void unapprove(Long orderId) {
        orderService.unapprove(orderId);
        // Pending again: it gets a fresh approval workflow with fresh timers
        if (orderService.isPending(orderId)) {
            startWorkflow(orderId);
        }
    }

    private void startWorkflow(Long orderId) {
        try {
            OrderApprovalWorkflow workflow = client.newWorkflowStub(OrderApprovalWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(OrderApprovalWorkflow.workflowId(orderId))
                            .setTaskQueue(OrderApprovalWorkflow.TASK_QUEUE)
                            // An order can be submitted again after it expired or was unapproved
                            .setWorkflowIdReusePolicy(
                                    WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
                            .build());
            WorkflowClient.start(workflow::run, orderId, reminderAfter.toSeconds(), expireAfter.toSeconds());
            log.info("Started approval workflow for order id={}", orderId);
        } catch (WorkflowExecutionAlreadyStarted e) {
            log.debug("Approval workflow for order id={} is already running", orderId);
        } catch (RuntimeException e) {
            // The order itself is saved; it just won't get a reminder or expire
            log.error("Could not start the approval workflow for order id={}: {}", orderId, e.getMessage());
        }
    }

    private void approveDirectly(Long orderId) {
        Orders order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (Boolean.FALSE.equals(order.getApproved())) {
            // Expired back into the cart while the employee was looking at it
            throw new IllegalArgumentException("Order " + orderId + " is not waiting for approval");
        }
        orderService.approve(orderId);
    }

    private RuntimeException translate(WorkflowUpdateException e, Long orderId) {
        ApplicationFailure failure = applicationFailure(e);
        String type = failure != null ? failure.getType() : null;
        if (OrderApprovalActivities.INSUFFICIENT_STOCK.equals(type)) {
            return new InsufficientStockException(failure.getDetails().get(0, String.class),
                    failure.getDetails().get(1, Integer.class));
        }
        if (OrderApprovalActivities.NOT_PENDING.equals(type)
                || IllegalStateException.class.getName().equals(type)) {
            return new IllegalArgumentException("Order " + orderId + " is not waiting for approval");
        }
        return new IllegalStateException("Approval of order " + orderId + " failed", e);
    }

    private static ApplicationFailure applicationFailure(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ApplicationFailure failure) {
                return failure;
            }
        }
        return null;
    }
}
