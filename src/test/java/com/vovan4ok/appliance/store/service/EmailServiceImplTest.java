package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    JavaMailSender mailSender;
    @Mock
    TemplateEngine templateEngine;

    @Test
    void sendHtml_enabled_rendersTemplateAndSends() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, templateEngine, true, "no-reply@store.com");
        when(templateEngine.process(eq("email/welcome"), any(IContext.class)))
                .thenReturn("<html>hi</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        service.sendHtml("user@mail.com", "Welcome", "email/welcome", Map.of("name", "Sam"));

        verify(templateEngine).process(eq("email/welcome"), any(IContext.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendHtml_disabled_doesNotSend() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, templateEngine, false, "no-reply@store.com");

        service.sendHtml("user@mail.com", "Welcome", "email/welcome", Map.of());

        verifyNoInteractions(mailSender, templateEngine);
    }

    @Test
    void sendHtml_blankRecipient_doesNotSend() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, templateEngine, true, "no-reply@store.com");

        service.sendHtml("   ", "Welcome", "email/welcome", Map.of());

        verifyNoInteractions(mailSender, templateEngine);
    }

    @Test
    void sendHtml_mailFailure_isSwallowed() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, templateEngine, true, "no-reply@store.com");
        when(templateEngine.process(eq("email/welcome"), any(IContext.class)))
                .thenReturn("<html>hi</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new org.springframework.mail.MailSendException("smtp down"))
                .when(mailSender).send(any(MimeMessage.class));

        service.sendHtml("user@mail.com", "Welcome", "email/welcome", Map.of());

        verify(mailSender).send(any(MimeMessage.class));
    }
}