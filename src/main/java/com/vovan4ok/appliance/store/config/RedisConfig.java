package com.vovan4ok.appliance.store.config;

import io.lettuce.core.ClientOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientOptionsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisConfig {

    /**
     * By default Lettuce queues commands while disconnected until they time out, so a Redis outage
     * would add the full timeout to every request. Rejecting them makes the fallbacks (deny-list,
     * rate limit, cache) kick in immediately; Lettuce keeps reconnecting in the background.
     */
    @Bean
    public LettuceClientOptionsBuilderCustomizer failFastWhileDisconnected() {
        return builder -> builder.disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
    }
}
