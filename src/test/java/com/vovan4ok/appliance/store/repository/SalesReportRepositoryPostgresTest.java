package com.vovan4ok.appliance.store.repository;

import com.vovan4ok.appliance.store.config.JooqConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jooq.test.autoconfigure.JooqTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The same report queries on PostgreSQL, as in production: schema and seed data from the Flyway migrations, so the
 * SQL jOOQ renders for PostgreSQL (FILTER, window functions, numeric rounding) is checked against the real
 * database. Skipped when Docker is not available.
 */
@JooqTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({SalesReportRepository.class, JooqConfig.class})
@TestPropertySource(properties = "spring.flyway.enabled=true")
@Testcontainers(disabledWithoutDocker = true)
class SalesReportRepositoryPostgresTest extends AbstractSalesReportRepositoryTest {

    /** The major version Cloud SQL runs (infra/terraform/database.tf) and docker-compose uses. */
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
}
