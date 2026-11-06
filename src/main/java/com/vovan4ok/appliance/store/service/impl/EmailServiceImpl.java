package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final String redirectTo;

    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine, boolean enabled, String from) {
        this(mailSender, templateEngine, enabled, from, "");
    }

    // JavaMailSender is provided by Spring Boot's MailSenderAutoConfiguration
    // (conditional on spring.mail.host) — IntelliJ cannot see it statically.
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public EmailServiceImpl(JavaMailSender mailSender,
                            TemplateEngine templateEngine,
                            @Value("${app.mail.enabled:true}") boolean enabled,
                            @Value("${app.mail.from:no-reply@appliance-store.com}") String from,
                            @Value("${app.mail.redirect-to:}") String redirectTo) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.enabled = enabled;
        this.from = from;
        this.redirectTo = redirectTo == null ? "" : redirectTo.strip();
        if (!this.redirectTo.isEmpty()) {
            log.warn("Mail redirect is on: every email goes to {} instead of its real recipient", this.redirectTo);
        }
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
            // Demo safety: the sample accounts use real-looking addresses that belong to strangers, so a demo
            // deployment sends everything to one inbox and keeps the intended recipient in the subject
            boolean redirected = !redirectTo.isEmpty();
            helper.setTo(redirected ? redirectTo : to);
            helper.setSubject(redirected ? "[to " + to + "] " + subject : subject);
            helper.setText(body, true);

            mailSender.send(message);
            log.info("Sent '{}' email to {}{}", subject, to, redirected ? " (redirected to " + redirectTo + ")" : "");
        } catch (MessagingException | MailException e) {
            log.error("Failed to send '{}' email to {}: {}", subject, to, e.getMessage());
        }
    }
}