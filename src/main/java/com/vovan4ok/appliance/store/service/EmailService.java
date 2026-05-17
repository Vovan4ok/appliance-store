package com.vovan4ok.appliance.store.service;

import java.util.Map;

/**
 * Sends HTML emails rendered from Thymeleaf templates.
 */
public interface EmailService {

    /**
     * Renders the given template and sends it as an HTML email.
     *
     * @param to           recipient address
     * @param subject      email subject line
     * @param templateName Thymeleaf template name, e.g. {@code email/welcome}
     * @param variables    model variables passed to the template
     */
    void sendHtml(String to, String subject, String templateName, Map<String, Object> variables);
}