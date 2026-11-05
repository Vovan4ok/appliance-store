package com.vovan4ok.appliance.store.metrics;

import com.vovan4ok.appliance.store.metrics.StoreMetrics.AiOutcome;
import com.vovan4ok.appliance.store.metrics.StoreMetrics.Channel;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StoreMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final StoreMetrics metrics = new StoreMetrics(registry);

    private double counter(String name, String... tags) {
        return registry.get(name).tags(tags).counter().count();
    }

    @Test
    void orderEvents_areCountedPerEvent() {
        metrics.orderSubmitted();
        metrics.orderSubmitted();
        metrics.orderApproved(new BigDecimal("150.50"));
        metrics.orderUnapproved();

        assertThat(counter(StoreMetrics.ORDERS, "event", "submitted")).isEqualTo(2);
        assertThat(counter(StoreMetrics.ORDERS, "event", "approved")).isEqualTo(1);
        assertThat(counter(StoreMetrics.ORDERS, "event", "unapproved")).isEqualTo(1);
    }

    @Test
    void orderApproved_recordsTheOrderAmount() {
        metrics.orderApproved(new BigDecimal("100.00"));
        metrics.orderApproved(new BigDecimal("50.25"));

        DistributionSummary amount = registry.get(StoreMetrics.ORDER_AMOUNT).summary();
        assertThat(amount.count()).isEqualTo(2);
        assertThat(amount.totalAmount()).isEqualTo(150.25);
    }

    @Test
    void orderApproved_withoutAmount_countsTheOrderOnly() {
        metrics.orderApproved(null);

        assertThat(counter(StoreMetrics.ORDERS, "event", "approved")).isEqualTo(1);
        assertThat(registry.find(StoreMetrics.ORDER_AMOUNT).summary()).isNull();
    }

    @Test
    void aiDescription_isTimedWithOutcomeAndReason() {
        metrics.aiDescription(metrics.startTimer(), AiOutcome.GENERATED, null);
        metrics.aiDescription(metrics.startTimer(), AiOutcome.CACHED, null);
        metrics.aiDescription(metrics.startTimer(), AiOutcome.FALLBACK, "AI_ERROR");

        Timer generated = registry.get(StoreMetrics.AI_DESCRIPTIONS)
                .tags("outcome", "generated", "reason", "none").timer();
        assertThat(generated.count()).isEqualTo(1);
        assertThat(registry.get(StoreMetrics.AI_DESCRIPTIONS)
                .tags("outcome", "cached", "reason", "none").timer().count()).isEqualTo(1);
        assertThat(registry.get(StoreMetrics.AI_DESCRIPTIONS)
                .tags("outcome", "fallback", "reason", "AI_ERROR").timer().count()).isEqualTo(1);
    }

    @Test
    void aiRateLimited_isCounted() {
        metrics.aiRateLimited();
        metrics.aiRateLimited();

        assertThat(counter(StoreMetrics.AI_RATE_LIMITED)).isEqualTo(2);
    }

    @Test
    void loginsAndLogouts_areCountedPerChannelAndResult() {
        metrics.login(Channel.WEB, true);
        metrics.login(Channel.WEB, false);
        metrics.login(Channel.WEB, false);
        metrics.login(Channel.API, true);
        metrics.logout(Channel.API);

        assertThat(counter(StoreMetrics.LOGINS, "channel", "web", "result", "success")).isEqualTo(1);
        assertThat(counter(StoreMetrics.LOGINS, "channel", "web", "result", "failure")).isEqualTo(2);
        assertThat(counter(StoreMetrics.LOGINS, "channel", "api", "result", "success")).isEqualTo(1);
        assertThat(counter(StoreMetrics.LOGOUTS, "channel", "api")).isEqualTo(1);
    }
}
