package com.vovan4ok.appliance.store.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Business metrics of the store, exported at /actuator/prometheus (names below use Micrometer's dot notation;
 * Prometheus shows them as e.g. {@code store_orders_total}). All names and tags live here, so the Grafana
 * dashboard has one place to match.
 * <p>
 * Tags only ever take a few fixed values (never user names, ids or free text), which keeps the number of
 * time series small.
 */
@Component
public class StoreMetrics {

    public static final String ORDERS = "store.orders";
    public static final String ORDER_AMOUNT = "store.orders.approved.amount";
    public static final String AI_DESCRIPTIONS = "store.ai.descriptions";
    public static final String AI_RATE_LIMITED = "store.ai.rate.limited";
    public static final String RECOMMENDATIONS_AI = "store.recommendations.ai";
    public static final String LOGINS = "store.auth.logins";
    public static final String LOGOUTS = "store.auth.logouts";
    public static final String PENDING_ORDERS = "store.orders.pending";
    public static final String OUT_OF_STOCK = "store.appliances.out.of.stock";

    /** Where a login or logout came from: the Thymeleaf UI (JWT cookie) or the REST API (Bearer token). */
    public enum Channel { WEB, API }

    /** How an AI description request ended. */
    public enum AiOutcome { GENERATED, CACHED, FALLBACK }

    /** How the AI re-ranking of recommendations ended: from the cache, a new ranking, or the scoring order. */
    public enum RankingOutcome { CACHED, RANKED, FALLBACK }

    private static final String NONE = "none";

    private final MeterRegistry registry;

    public StoreMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void orderSubmitted() {
        orderEvent("submitted");
    }

    public void orderApproved(BigDecimal amount) {
        orderEvent("approved");
        if (amount != null) {
            DistributionSummary.builder(ORDER_AMOUNT)
                    .description("Total amount of approved orders")
                    .register(registry)
                    .record(amount.doubleValue());
        }
    }

    public void orderUnapproved() {
        orderEvent("unapproved");
    }

    /** A pending order nobody approved in time went back to the client's cart. */
    public void orderExpired() {
        orderEvent("expired");
    }

    /** Employees were reminded that an order is still waiting for approval. */
    public void approvalReminderSent() {
        orderEvent("approval_reminder");
    }

    private void orderEvent(String event) {
        Counter.builder(ORDERS)
                .description("Order lifecycle events")
                .tag("event", event)
                .register(registry)
                .increment();
    }

    /** Starts timing an AI description request; finish it with {@link #aiDescription}. */
    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    /**
     * Records one AI description request.
     *
     * @param reason the fallback reason (AI_DISABLED, AI_ERROR...), or null when the AI text was returned
     */
    public void aiDescription(Timer.Sample sample, AiOutcome outcome, String reason) {
        sample.stop(Timer.builder(AI_DESCRIPTIONS)
                .description("AI description requests by outcome")
                .tag("outcome", outcome.name().toLowerCase())
                .tag("reason", reason != null ? reason : NONE)
                .publishPercentileHistogram()
                .register(registry));
    }

    /**
     * Records how the AI re-ranking of one recommendations request ended.
     *
     * @param reason why the scoring order was used (AI_ERROR, BUDGET, SLOW...), or null
     */
    public void recommendationRanking(RankingOutcome outcome, String reason) {
        Counter.builder(RECOMMENDATIONS_AI)
                .description("Recommendation requests by AI re-ranking outcome")
                .tag("outcome", outcome.name().toLowerCase())
                .tag("reason", reason != null ? reason : NONE)
                .register(registry)
                .increment();
    }

    public void aiRateLimited() {
        Counter.builder(AI_RATE_LIMITED)
                .description("AI description requests rejected by the per-employee rate limit")
                .register(registry)
                .increment();
    }

    public void login(Channel channel, boolean success) {
        Counter.builder(LOGINS)
                .description("Login attempts")
                .tag("channel", channel.name().toLowerCase())
                .tag("result", success ? "success" : "failure")
                .register(registry)
                .increment();
    }

    public void logout(Channel channel) {
        Counter.builder(LOGOUTS)
                .description("Logouts (each one revokes its JWT)")
                .tag("channel", channel.name().toLowerCase())
                .register(registry)
                .increment();
    }
}
