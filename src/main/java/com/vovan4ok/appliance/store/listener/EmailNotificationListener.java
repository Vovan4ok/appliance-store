package com.vovan4ok.appliance.store.listener;

import com.vovan4ok.appliance.store.event.OrderApprovedEvent;
import com.vovan4ok.appliance.store.event.OrderEmailData;
import com.vovan4ok.appliance.store.event.OrderSubmittedEvent;
import com.vovan4ok.appliance.store.event.PasswordChangedEvent;
import com.vovan4ok.appliance.store.event.RegistrationCompletedEvent;
import com.vovan4ok.appliance.store.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns domain events into email notifications. Each handler runs asynchronously
 * and only after the publishing transaction has committed (or immediately, via
 * {@code fallbackExecution}, when no transaction is active).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final EmailService emailService;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRegistration(RegistrationCompletedEvent event) {
        log.debug("Handling RegistrationCompletedEvent for {}", event.email());
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", event.name());
        emailService.sendHtml(event.email(), "Welcome to Appliance Store",
                "email/welcome", variables);
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderSubmitted(OrderSubmittedEvent event) {
        OrderEmailData order = event.order();
        log.debug("Handling OrderSubmittedEvent for order #{}", order.getOrderId());
        emailService.sendHtml(order.getClientEmail(),
                "Your order #" + order.getOrderId() + " was received",
                "email/order-submitted", orderVariables(order));
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderApproved(OrderApprovedEvent event) {
        OrderEmailData order = event.order();
        log.debug("Handling OrderApprovedEvent for order #{}", order.getOrderId());
        emailService.sendHtml(order.getClientEmail(),
                "Your order #" + order.getOrderId() + " was approved",
                "email/order-approved", orderVariables(order));
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordChanged(PasswordChangedEvent event) {
        log.debug("Handling PasswordChangedEvent for {}", event.email());
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", event.name());
        emailService.sendHtml(event.email(), "Your Appliance Store password was changed",
                "email/password-changed", variables);
    }

    private Map<String, Object> orderVariables(OrderEmailData order) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("order", order);
        return variables;
    }
}