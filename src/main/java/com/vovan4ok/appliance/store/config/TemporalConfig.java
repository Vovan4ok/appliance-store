package com.vovan4ok.appliance.store.config;

import com.vovan4ok.appliance.store.workflow.OrderApprovalActivities;
import com.vovan4ok.appliance.store.workflow.OrderApprovalWorkflow;
import com.vovan4ok.appliance.store.workflow.OrderApprovalWorkflowImpl;
import io.grpc.health.v1.HealthCheckResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Temporal client and worker (prod: {@code app.temporal.enabled=true}). The worker runs inside the app: it polls
 * the {@value OrderApprovalWorkflow#TASK_QUEUE} task queue and executes the workflow and its activities.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.temporal.enabled", havingValue = "true")
public class TemporalConfig {

    @Bean(destroyMethod = "shutdown")
    public WorkflowServiceStubs workflowServiceStubs(@Value("${app.temporal.target}") String target) {
        // Connects lazily: the app starts even while the Temporal server is still coming up
        return WorkflowServiceStubs.newServiceStubs(WorkflowServiceStubsOptions.newBuilder()
                .setTarget(target)
                .build());
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs stubs,
                                         @Value("${app.temporal.namespace}") String namespace) {
        return WorkflowClient.newInstance(stubs, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
    }

    /**
     * The worker executes the workflows and activities. On by default; the web role on Cloud Run turns it off
     * ({@code app.temporal.worker.enabled=false}) and only starts and approves workflows, while the same image runs
     * as the worker on an always-on VM. A worker needs constant CPU to poll, which serverless web instances lack.
     */
    @Configuration
    // Both switches in one condition: component scanning also finds this nested class on its own
    @ConditionalOnExpression("${app.temporal.enabled:false} and ${app.temporal.worker.enabled:true}")
    static class WorkerConfig {

        @Bean(destroyMethod = "shutdown")
        public WorkerFactory workerFactory(WorkflowClient client, OrderApprovalActivities activities) {
            WorkerFactory factory = WorkerFactory.newInstance(client);
            Worker worker = factory.newWorker(OrderApprovalWorkflow.TASK_QUEUE);
            worker.registerWorkflowImplementationTypes(OrderApprovalWorkflowImpl.class);
            worker.registerActivitiesImplementations(activities);
            return factory;
        }

        /** Starts polling only when the app is fully up, so activities never run against a half-started context. */
        @EventListener(ApplicationReadyEvent.class)
        public void startWorker(ApplicationReadyEvent event) {
            WorkerFactory factory = event.getApplicationContext().getBean(WorkerFactory.class);
            try {
                factory.start();
                log.info("Temporal worker started on task queue '{}'", OrderApprovalWorkflow.TASK_QUEUE);
            } catch (RuntimeException e) {
                // Fail-open: the shop works without it (see TemporalOrderApprovalProcess); health shows temporal DOWN
                log.error("Temporal worker could not start: {}", e.getMessage());
            }
        }
    }

    /** Shows up as {@code temporal} in /actuator/health. */
    @Bean
    public HealthIndicator temporalHealthIndicator(WorkflowServiceStubs stubs) {
        return () -> {
            try {
                HealthCheckResponse.ServingStatus status = stubs.healthCheck().getStatus();
                return status == HealthCheckResponse.ServingStatus.SERVING
                        ? Health.up().build()
                        : Health.down().withDetail("status", status.name()).build();
            } catch (RuntimeException e) {
                return Health.down().withDetail("error", e.getClass().getSimpleName()).build();
            }
        };
    }
}
