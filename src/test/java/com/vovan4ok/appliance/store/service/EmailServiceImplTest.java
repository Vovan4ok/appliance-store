package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import jakarta.mail.Message;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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

    private MimeMessage sendWelcome(EmailServiceImpl service) {
        when(templateEngine.process(eq("email/welcome"), any(IContext.class))).thenReturn("<html>hi</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        service.sendHtml("phobos@gmail.com", "Welcome", "email/welcome", Map.of("name", "Sam"));
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        return sent.getValue();
    }

    @Test
    void sendHtml_withoutRedirect_goesToTheRealRecipient() throws Exception {
        MimeMessage message = sendWelcome(
                new EmailServiceImpl(mailSender, templateEngine, true, "no-reply@store.com", ""));

        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("phobos@gmail.com");
        assertThat(message.getSubject()).isEqualTo("Welcome");
    }

    @Test
    void sendHtml_withRedirect_goesToTheDemoInboxAndKeepsTheRecipientInTheSubject() throws Exception {
        MimeMessage message = sendWelcome(
                new EmailServiceImpl(mailSender, templateEngine, true, "no-reply@store.com", " demo@inbox.com "));

        assertThat(message.getRecipients(Message.RecipientType.TO)).hasSize(1);
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("demo@inbox.com");
        assertThat(message.getSubject()).isEqualTo("[to phobos@gmail.com] Welcome");
    }
}
