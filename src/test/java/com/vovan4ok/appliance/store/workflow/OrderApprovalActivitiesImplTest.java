package com.vovan4ok.appliance.store.workflow;

import com.vovan4ok.appliance.store.exception.InsufficientStockException;
import com.vovan4ok.appliance.store.metrics.StoreMetrics;
import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.model.Employee;
import com.vovan4ok.appliance.store.model.Orders;
import com.vovan4ok.appliance.store.service.EmailService;
import com.vovan4ok.appliance.store.service.EmployeeService;
import com.vovan4ok.appliance.store.service.OrderService;
import io.temporal.failure.ApplicationFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderApprovalActivitiesImplTest {

    @Mock
    OrderService orderService;
    @Mock
    EmployeeService employeeService;
    @Mock
    EmailService emailService;
    @Mock
    StoreMetrics metrics;

    @InjectMocks
    OrderApprovalActivitiesImpl activities;

    private static Orders pendingOrder() {
        Client client = new Client();
        client.setName("Mercury");
        client.setEmail("mercury@gmail.com");
        Orders order = new Orders();
        order.setId(5L);
        order.setClient(client);
        order.setApproved(null);
        return order;
    }

    private static Employee employee(String email) {
        Employee employee = new Employee();
        employee.setEmail(email);
        return employee;
    }

    @Test
    void approveOrder_pending_approvesIt() {
        when(orderService.isPending(5L)).thenReturn(true);

        activities.approveOrder(5L);

        verify(orderService).approve(5L);
    }

    @Test
    void approveOrder_notEnoughStock_failsWithoutRetryKeepingTheDetails() {
        when(orderService.isPending(5L)).thenReturn(true);
        doThrow(new InsufficientStockException("Toaster", 1)).when(orderService).approve(5L);

        assertThatThrownBy(() -> activities.approveOrder(5L))
                .isInstanceOfSatisfying(ApplicationFailure.class, failure -> {
                    assertThat(failure.getType()).isEqualTo(OrderApprovalActivities.INSUFFICIENT_STOCK);
                    assertThat(failure.isNonRetryable()).isTrue();
                    assertThat(failure.getDetails().get(0, String.class)).isEqualTo("Toaster");
                    assertThat(failure.getDetails().get(1, Integer.class)).isEqualTo(1);
                });
    }

    @Test
    void approveOrder_notPending_failsWithoutTouchingTheOrder() {
        when(orderService.isPending(5L)).thenReturn(false);

        assertThatThrownBy(() -> activities.approveOrder(5L))
                .isInstanceOfSatisfying(ApplicationFailure.class, failure ->
                        assertThat(failure.getType()).isEqualTo(OrderApprovalActivities.NOT_PENDING));
        verify(orderService, never()).approve(anyLong());
    }

    @Test
    void remindEmployees_pending_emailsEveryEmployee() {
        when(orderService.findById(5L)).thenReturn(Optional.of(pendingOrder()));
        when(employeeService.findAll()).thenReturn(List.of(employee("a@store.com"), employee("b@store.com")));

        boolean reminded = activities.remindEmployees(5L, 86_400, 172_800);

        assertThat(reminded).isTrue();
        verify(emailService).sendHtml(eq("a@store.com"), anyString(), eq("email/order-approval-reminder"),
                argThat((Map<String, Object> vars) -> vars.get("waitingHours").equals(24L)
                        && vars.get("hoursLeft").equals(48L)));
        verify(emailService).sendHtml(eq("b@store.com"), anyString(), anyString(), anyMap());
        verify(metrics).approvalReminderSent();
    }

    @Test
    void remindEmployees_alreadyDecided_sendsNothing() {
        Orders approved = pendingOrder();
        approved.setApproved(true);
        when(orderService.findById(5L)).thenReturn(Optional.of(approved));

        assertThat(activities.remindEmployees(5L, 86_400, 172_800)).isFalse();
        verify(emailService, never()).sendHtml(any(), any(), any(), any());
        verify(metrics, never()).approvalReminderSent();
    }

    @Test
    void remindEmployees_orderDeleted_sendsNothing() {
        when(orderService.findById(5L)).thenReturn(Optional.empty());

        assertThat(activities.remindEmployees(5L, 86_400, 172_800)).isFalse();
        verify(emailService, never()).sendHtml(any(), any(), any(), any());
    }

    @Test
    void returnToCart_delegatesToTheOrderService() {
        when(orderService.returnToCart(5L)).thenReturn(true);

        assertThat(activities.returnToCart(5L)).isTrue();
    }
}
