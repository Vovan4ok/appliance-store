package com.vovan4ok.appliance.store.repository;

import com.vovan4ok.appliance.store.config.JooqConfig;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jooq.test.autoconfigure.AutoConfigureJooq;
import org.springframework.context.annotation.Import;

/**
 * The report queries on H2, as in dev: tables created by Hibernate with the seed data, jOOQ next to JPA on the
 * same DataSource.
 */
@DataJpaTest
@AutoConfigureJooq
@Import({SalesReportRepository.class, JooqConfig.class})
class SalesReportRepositoryTest extends AbstractSalesReportRepositoryTest {
}
