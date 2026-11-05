package com.vovan4ok.appliance.store.workflow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Runs the real workflow code on Temporal's in-memory test server. Time is skipped, so the 24h reminder and the
 * 72h expiry take milliseconds.
 */
class OrderApprovalWorkflowTest {

    private static final long ORDER_ID = 7L;
    private static final long REMIND_AFTER = Duration.ofHours(24).toSeconds();
    private static final long EXPIRE_AFTER = Duration.ofHours(72).toSeconds();

    private TestWorkflowEnvironment env;
    private OrderApprovalActivities activities;
    private OrderApprovalWorkflow workflow;

    @BeforeEach
    void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        Worker worker = env.newWorker(OrderApprovalWorkflow.TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(OrderApprovalWorkflowImpl.class);
        activities = mock(OrderApprovalActivities.class, withSettings().withoutAnnotations());
        worker.registerActivitiesImplementations(activities);
        env.start();

        workflow = env.getWorkflowClient().newWorkflowStub(OrderApprovalWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(OrderApprovalWorkflow.workflowId(ORDER_ID))
                        .setTaskQueue(OrderApprovalWorkflow.TASK_QUEUE)
                        .build());
        WorkflowClient.start(workflow::run, ORDER_ID, REMIND_AFTER, EXPIRE_AFTER);
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private String result() {
        return WorkflowStub.fromTyped(workflow).getResult(String.class);
    }

    @Test
    void approvedBeforeTheReminder_endsApprovedWithoutEmails() {
        workflow.approve();

        assertThat(result()).isEqualTo("APPROVED");
        verify(activities).approveOrder(ORDER_ID);
        verify(activities, never()).remindEmployees(anyLong(), anyLong(), anyLong());
        verify(activities, never()).returnToCart(anyLong());
    }

    @Test
    void nobodyDecides_remindsAfter24hThenReturnsToCartAfter72h() {
        when(activities.remindEmployees(ORDER_ID, REMIND_AFTER, EXPIRE_AFTER - REMIND_AFTER)).thenReturn(true);
        when(activities.returnToCart(ORDER_ID)).thenReturn(true);

        env.sleep(Duration.ofHours(23));
        verify(activities, never()).remindEmployees(anyLong(), anyLong(), anyLong());

        env.sleep(Duration.ofHours(2));
        verify(activities).remindEmployees(ORDER_ID, REMIND_AFTER, EXPIRE_AFTER - REMIND_AFTER);
        verify(activities, never()).returnToCart(anyLong());
        assertThat(workflow.state()).isEqualTo("PENDING");

        assertThat(result()).isEqualTo("EXPIRED");
        verify(activities).returnToCart(ORDER_ID);
        verify(activities, never()).approveOrder(anyLong());
    }

    @Test
    void approvedAfterTheReminder_doesNotExpire() {
        when(activities.remindEmployees(anyLong(), anyLong(), anyLong())).thenReturn(true);

        env.sleep(Duration.ofHours(30));
        workflow.approve();

        assertThat(result()).isEqualTo("APPROVED");
        verify(activities, times(1)).remindEmployees(anyLong(), anyLong(), anyLong());
        verify(activities, never()).returnToCart(anyLong());
    }

    @Test
    void notEnoughStock_failsTheApprovalButKeepsWaiting() {
        doThrow(ApplicationFailure.newNonRetryableFailure("Not enough stock",
                OrderApprovalActivities.INSUFFICIENT_STOCK, "Toaster", 1))
                .doNothing()
                .when(activities).approveOrder(ORDER_ID);

        assertThatThrownBy(workflow::approve).isInstanceOf(WorkflowUpdateException.class);
        assertThat(workflow.state()).isEqualTo("PENDING");

        // Stock was added, the employee tries again
        workflow.approve();
        assertThat(result()).isEqualTo("APPROVED");
        verify(activities, times(2)).approveOrder(ORDER_ID);
    }

    @Test
    void orderChangedOutsideTheWorkflow_endsClosedWithoutTouchingIt() {
        // Approved or deleted in the meantime: the activity sees it is no longer pending
        when(activities.remindEmployees(anyLong(), anyLong(), anyLong())).thenReturn(false);

        assertThat(result()).isEqualTo("CLOSED");
        verify(activities, never()).returnToCart(anyLong());
    }

    @Test
    void approvalAfterExpiry_isRejected() {
        when(activities.remindEmployees(anyLong(), anyLong(), anyLong())).thenReturn(true);
        when(activities.returnToCart(ORDER_ID)).thenReturn(true);
        doNothing().when(activities).approveOrder(ORDER_ID);

        assertThat(result()).isEqualTo("EXPIRED");

        assertThatThrownBy(workflow::approve).isInstanceOf(RuntimeException.class);
        verify(activities, never()).approveOrder(anyLong());
    }
}
