package com.vovan4ok.appliance.store.workflow;

import com.vovan4ok.appliance.store.event.OrderEmailData;
import com.vovan4ok.appliance.store.exception.InsufficientStockException;
import com.vovan4ok.appliance.store.metrics.StoreMetrics;
import com.vovan4ok.appliance.store.model.Employee;
import com.vovan4ok.appliance.store.model.Orders;
import com.vovan4ok.appliance.store.service.EmailService;
import com.vovan4ok.appliance.store.service.EmployeeService;
import com.vovan4ok.appliance.store.service.OrderService;
import io.temporal.failure.ApplicationFailure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.temporal.enabled", havingValue = "true")
public class OrderApprovalActivitiesImpl implements OrderApprovalActivities {

    private final OrderService orderService;
    private final EmployeeService employeeService;
    private final EmailService emailService;
    private final StoreMetrics metrics;

    @Override
    public void approveOrder(long orderId) {
        if (!orderService.isPending(orderId)) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "Order " + orderId + " is not waiting for approval", NOT_PENDING);
        }
        try {
            orderService.approve(orderId);
        } catch (InsufficientStockException e) {
            // Details carry the exception's fields, so the caller can rebuild it for the UI
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), INSUFFICIENT_STOCK,
                    e.getApplianceName(), e.getAvailable());
        }
    }

    @Override
    public boolean remindEmployees(long orderId, long waitingSeconds, long secondsLeft) {
        Orders order = orderService.findById(orderId).orElse(null);
        if (order == null || order.getApproved() != null) {
            return false;
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("order", OrderEmailData.from(order));
        variables.put("waitingHours", waitingSeconds / 3600);
        variables.put("hoursLeft", secondsLeft / 3600);
        for (Employee employee : employeeService.findAll()) {
            emailService.sendHtml(employee.getEmail(), "Order #" + orderId + " is waiting for approval",
                    "email/order-approval-reminder", variables);
        }
        metrics.approvalReminderSent();
        log.info("Order id={} still pending: reminded employees", orderId);
        return true;
    }

    @Override
    public boolean returnToCart(long orderId) {
        boolean returned = orderService.returnToCart(orderId);
        if (returned) {
            log.info("Order id={} was not approved in time: returned to the client's cart", orderId);
        }
        return returned;
    }
}
