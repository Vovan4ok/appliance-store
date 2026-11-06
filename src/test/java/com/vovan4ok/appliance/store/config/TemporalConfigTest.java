package com.vovan4ok.appliance.store.config;

import com.vovan4ok.appliance.store.workflow.OrderApprovalActivities;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.WorkerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * One image, two roles: the web role (Cloud Run) talks to Temporal but runs no worker; the worker role (VM)
 * executes the workflows. Nothing connects here: the client connects lazily.
 */
class TemporalConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TemporalConfig.class)
            .withBean(OrderApprovalActivities.class, () -> mock(OrderApprovalActivities.class))
            .withPropertyValues("app.temporal.enabled=true",
                    "app.temporal.target=localhost:7233",
                    "app.temporal.namespace=default");

    @Test
    void byDefault_theAppAlsoRunsTheWorker() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(WorkflowClient.class);
            assertThat(context).hasSingleBean(WorkerFactory.class);
            assertThat(context).hasBean("temporalHealthIndicator");
        });
    }

    @Test
    void webRole_hasTheClientButNoWorker() {
        runner.withPropertyValues("app.temporal.worker.enabled=false").run(context -> {
            assertThat(context).hasSingleBean(WorkflowClient.class);
            assertThat(context).doesNotHaveBean(WorkerFactory.class);
            assertThat(context.getBean("temporalHealthIndicator", HealthIndicator.class)).isNotNull();
        });
    }

    @Test
    void temporalDisabled_createsNothing() {
        new ApplicationContextRunner()
                .withUserConfiguration(TemporalConfig.class)
                .withPropertyValues("app.temporal.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(WorkflowClient.class);
                    assertThat(context).doesNotHaveBean(WorkerFactory.class);
                });
    }
}
