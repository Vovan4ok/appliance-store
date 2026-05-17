package com.vovan4ok.appliance.store.service;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.vovan4ok.appliance.store.service.impl.EmailServiceImpl;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check that {@link EmailServiceImpl} renders a Thymeleaf template
 * and delivers it over SMTP, verified against an in-memory GreenMail server.
 */
class EmailServiceGreenMailTest {

    private GreenMail greenMail;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(ServerSetupTest.SMTP.getBindAddress());
        mailSender.setPort(ServerSetupTest.SMTP.getPort());

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        emailService = new EmailServiceImpl(mailSender, templateEngine, true, "no-reply@store.com");
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }

    @Test
    void sendHtml_deliversRenderedEmailToRecipient() throws Exception {
        emailService.sendHtml("client@mail.com", "Welcome to Appliance Store",
                "email/welcome", Map.of("name", "Volodymyr"));

        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("Welcome to Appliance Store");
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("client@mail.com");
        assertThat(received[0].getContent().toString())
                .contains("Volodymyr")
                .contains("Appliance Store");
    }
}