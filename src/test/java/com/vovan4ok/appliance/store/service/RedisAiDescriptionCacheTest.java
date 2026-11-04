package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.config.CacheConfig;
import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.PowerType;
import com.vovan4ok.appliance.store.model.dto.api.AiDescriptionResponse;
import com.vovan4ok.appliance.store.service.impl.AiDescriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The AI description cache backed by a real Redis (as in prod), with the model mocked.
 */
@DataRedisTest
@Testcontainers(disabledWithoutDocker = true)
class RedisAiDescriptionCacheTest {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @Autowired
    RedisConnectionFactory connectionFactory;

    @Autowired
    StringRedisTemplate template;

    ChatClient chatClient = mock(ChatClient.class);
    ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    ChatClient.CallResponseSpec callResponse = mock(ChatClient.CallResponseSpec.class);
    AiDescriptionServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        template.getConnectionFactory().getConnection().serverCommands().flushAll();

        Appliance toaster = new Appliance();
        toaster.setId(58L);
        toaster.setName("2-Slice Toaster");
        toaster.setCategory(Category.SMALL);
        toaster.setPowerType(PowerType.AC220);
        toaster.setCharacteristic("2 slots, 7 settings");
        ApplianceService applianceService = mock(ApplianceService.class);
        when(applianceService.findById(58L)).thenReturn(Optional.of(toaster));

        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(eq("ai.prompt.language"), any(), any(Locale.class))).thenReturn("English");

        ObjectProvider<ChatModel> chatModel = mock(ObjectProvider.class);
        when(chatModel.getIfAvailable()).thenReturn(mock(ChatModel.class));
        ObjectProvider<ChatClient.Builder> builderProvider = mock(ObjectProvider.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builderProvider.getObject()).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponse);
        when(callResponse.content()).thenReturn("Crispy toast every morning.");

        // Same settings as prod: spring.cache.type=redis, 7-day TTL
        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(7)))
                .build();
        cacheManager.afterPropertiesSet();

        service = new AiDescriptionServiceImpl(applianceService, messageSource, cacheManager,
                chatModel, builderProvider);
    }

    @Test
    void secondRequest_isServedFromRedis_withoutCallingTheModel() throws InterruptedException {
        AiDescriptionResponse first = service.generate(58L, Locale.ENGLISH);
        awaitCachedKeys();
        AiDescriptionResponse second = service.generate(58L, Locale.ENGLISH);

        assertThat(second.source()).isEqualTo(AiDescriptionResponse.Source.AI);
        assertThat(second.description()).isEqualTo(first.description());
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void cachedEntry_livesInRedisWithA7DayTtl() throws InterruptedException {
        service.generate(58L, Locale.ENGLISH);

        Set<String> keys = awaitCachedKeys();
        assertThat(keys).hasSize(1);
        assertThat(template.getExpire(keys.iterator().next())).isGreaterThan(Duration.ofDays(6).toSeconds());
    }

    // Spring Data Redis writes cache entries asynchronously, so the entry shows up shortly after the call
    private Set<String> awaitCachedKeys() throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            Set<String> keys = template.keys(CacheConfig.AI_DESCRIPTIONS + "::58:en:*");
            if (!keys.isEmpty()) {
                return keys;
            }
            Thread.sleep(50);
        }
        return Set.of();
    }
}
