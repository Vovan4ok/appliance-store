package com.vovan4ok.appliance.store.listener;

import com.vovan4ok.appliance.store.event.OrderApprovedEvent;
import com.vovan4ok.appliance.store.event.OrderEmailData;
import com.vovan4ok.appliance.store.event.OrderSubmittedEvent;
import com.vovan4ok.appliance.store.event.PasswordChangedEvent;
import com.vovan4ok.appliance.store.event.RegistrationCompletedEvent;
import com.vovan4ok.appliance.store.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationListenerTest {

    @Mock
    EmailService emailService;

    @InjectMocks
    EmailNotificationListener listener;

    private static OrderEmailData order() {
        return new OrderEmailData(7L, "Sam", "sam@mail.com", new BigDecimal("99.00"), List.of());
    }

    @Test
    void onRegistration_sendsWelcomeEmail() {
        listener.onRegistration(new RegistrationCompletedEvent("Sam", "sam@mail.com"));

        verify(emailService).sendHtml(eq("sam@mail.com"), anyString(), eq("email/welcome"), anyMap());
    }

    @Test
    void onOrderSubmitted_sendsOrderSubmittedEmail() {
        listener.onOrderSubmitted(new OrderSubmittedEvent(order()));

        verify(emailService).sendHtml(eq("sam@mail.com"), anyString(), eq("email/order-submitted"), anyMap());
    }

    @Test
    void onOrderApproved_sendsOrderApprovedEmail() {
        listener.onOrderApproved(new OrderApprovedEvent(order()));

        verify(emailService).sendHtml(eq("sam@mail.com"), anyString(), eq("email/order-approved"), anyMap());
    }

    @Test
    void onPasswordChanged_sendsPasswordChangedEmail() {
        listener.onPasswordChanged(new PasswordChangedEvent("Sam", "sam@mail.com"));

        verify(emailService).sendHtml(eq("sam@mail.com"), anyString(), eq("email/password-changed"), anyMap());
    }
}