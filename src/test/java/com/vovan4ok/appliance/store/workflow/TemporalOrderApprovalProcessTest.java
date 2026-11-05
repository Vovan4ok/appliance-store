package com.vovan4ok.appliance.store.workflow;

import com.vovan4ok.appliance.store.exception.InsufficientStockException;
import com.vovan4ok.appliance.store.model.Orders;
import com.vovan4ok.appliance.store.service.OrderService;
import io.temporal.client.WorkflowClient;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class TemporalOrderApprovalProcessTest {

    private TestWorkflowEnvironment env;
    private OrderApprovalActivities activities;
    private OrderService orderService;
    private TemporalOrderApprovalProcess process;

    @BeforeEach
    void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        Worker worker = env.newWorker(OrderApprovalWorkflow.TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(OrderApprovalWorkflowImpl.class);
        activities = mock(OrderApprovalActivities.class, withSettings().withoutAnnotations());
        worker.registerActivitiesImplementations(activities);
        env.start();

        orderService = mock(OrderService.class);
        process = new TemporalOrderApprovalProcess(orderService, env.getWorkflowClient(),
                Duration.ofHours(24), Duration.ofHours(72));
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private OrderApprovalWorkflow workflowOf(long orderId) {
        return env.getWorkflowClient().newWorkflowStub(OrderApprovalWorkflow.class,
                OrderApprovalWorkflow.workflowId(orderId));
    }

    private static Orders order(Boolean approved) {
        Orders order = new Orders();
        order.setId(1L);
        order.setApproved(approved);
        return order;
    }

    @Test
    void submit_savesTheOrderAndStartsItsWorkflow() {
        process.submit(1L);

        verify(orderService).submitCart(1L);
        assertThat(workflowOf(1L).state()).isEqualTo("PENDING");
    }

    @Test
    void approve_goesThroughTheWorkflow() {
        process.submit(1L);

        process.approve(1L);

        verify(activities).approveOrder(1L);
        verify(orderService, never()).approve(anyLong());
    }

    @Test
    void approve_notEnoughStock_isRethrownAsInsufficientStockException() {
        process.submit(1L);
        doThrow(ApplicationFailure.newNonRetryableFailure("Not enough stock",
                OrderApprovalActivities.INSUFFICIENT_STOCK, "Toaster", 1))
                .when(activities).approveOrder(1L);
        when(orderService.findById(1L)).thenReturn(Optional.of(order(null)));

        assertThatThrownBy(() -> process.approve(1L))
                .isInstanceOfSatisfying(InsufficientStockException.class, e -> {
                    assertThat(e.getApplianceName()).isEqualTo("Toaster");
                    assertThat(e.getAvailable()).isEqualTo(1);
                });
        // Still waiting for approval
        assertThat(workflowOf(1L).state()).isEqualTo("PENDING");
    }

    @Test
    void approve_orderWithoutWorkflow_isApprovedDirectly() {
        // Submitted before Temporal was enabled
        when(orderService.findById(1L)).thenReturn(Optional.of(order(null)));

        process.approve(1L);

        verify(orderService).approve(1L);
    }

    @Test
    void approve_twice_secondIsANoOp() {
        process.submit(1L);
        process.approve(1L);
        when(orderService.findById(1L)).thenReturn(Optional.of(order(true)));

        process.approve(1L);

        verify(activities).approveOrder(1L);
    }

    @Test
    void approve_orderThatExpiredBackToTheCart_isRejected() {
        when(orderService.findById(1L)).thenReturn(Optional.of(order(false)));

        assertThatThrownBy(() -> process.approve(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not waiting for approval");
        verify(orderService, never()).approve(anyLong());
    }

    @Test
    void unapprove_startsAFreshWorkflow() {
        when(orderService.isPending(1L)).thenReturn(true);

        process.unapprove(1L);

        verify(orderService).unapprove(1L);
        assertThat(workflowOf(1L).state()).isEqualTo("PENDING");
    }

    @Test
    void submit_whenTheWorkflowCannotStart_stillSubmitsTheOrder() {
        WorkflowClient unreachable = mock(WorkflowClient.class);
        when(unreachable.newWorkflowStub(org.mockito.ArgumentMatchers.eq(OrderApprovalWorkflow.class),
                org.mockito.ArgumentMatchers.any(io.temporal.client.WorkflowOptions.class)))
                .thenThrow(new IllegalStateException("Temporal down"));
        TemporalOrderApprovalProcess failOpen = new TemporalOrderApprovalProcess(orderService, unreachable,
                Duration.ofHours(24), Duration.ofHours(72));

        failOpen.submit(1L);

        verify(orderService).submitCart(1L);
    }

    @Test
    void invalidTimers_areRejectedAtStartup() {
        assertThatThrownBy(() -> new TemporalOrderApprovalProcess(orderService, env.getWorkflowClient(),
                Duration.ofHours(72), Duration.ofHours(24)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
