package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final boolean enabled;
    private final String from;

    // JavaMailSender is provided by Spring Boot's MailSenderAutoConfiguration
    // (conditional on spring.mail.host) — IntelliJ cannot see it statically.
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public EmailServiceImpl(JavaMailSender mailSender,
                            TemplateEngine templateEngine,
                            @Value("${app.mail.enabled:true}") boolean enabled,
                            @Value("${app.mail.from:no-reply@appliance-store.com}") String from) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.enabled = enabled;
        this.from = from;
    }

    @Override
    public void sendHtml(String to, String subject, String templateName, Map<String, Object> variables) {
        if (!enabled) {
            log.info("Mail disabled - skipping '{}' email to {}", subject, to);
            return;
        }
        if (to == null || to.isBlank()) {
            log.warn("Skipping '{}' email - recipient address is missing", subject);
            return;
        }
        try {
            Context context = new Context();
            context.setVariables(variables);
            String body = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            log.info("Sent '{}' email to {}", subject, to);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send '{}' email to {}: {}", subject, to, e.getMessage());
        }
    }
}