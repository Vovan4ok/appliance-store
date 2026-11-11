package com.vovan4ok.appliance.store.config;

import org.jooq.conf.RenderQuotedNames;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jOOQ uses Spring Boot's DataSource, dialect detection (H2 or PostgreSQL) and Spring transactions; only the
 * rendering of names is changed here.
 */
@Configuration
public class JooqConfig {

    /**
     * Unquoted table and column names: each database folds them to its own case, like the SQL Hibernate and Flyway
     * write. Quoted "orders" would only match PostgreSQL's lower-case tables, not H2's upper-case ORDERS (dev/test).
     */
    @Bean
    DefaultConfigurationCustomizer unquotedNames() {
        return configuration -> configuration.settings()
                .withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED);
    }
}
