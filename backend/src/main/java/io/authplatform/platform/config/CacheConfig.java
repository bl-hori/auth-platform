package io.authplatform.platform.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Cache configuration for multi-layer caching strategy.
 *
 * <p>This configuration sets up a two-layer cache:
 * <ul>
 *   <li><strong>L1 Cache (Caffeine):</strong> In-memory cache for fast access within a single application instance</li>
 *   <li><strong>L2 Cache (Redis):</strong> Distributed cache shared across multiple application instances</li>
 * </ul>
 *
 * <p><strong>Cache Strategy:</strong>
 * <ol>
 *   <li>Check L1 cache (Caffeine) - typically ~1-2ms</li>
 *   <li>If miss, check L2 cache (Redis) - typically ~3-5ms</li>
 *   <li>If miss, evaluate authorization and cache result in both L1 and L2</li>
 * </ol>
 *
 * <p><strong>Configuration:</strong>
 * <pre>{@code
 * # application.yml
 * spring:
 *   cache:
 *     type: caffeine
 *     caffeine:
 *       spec: maximumSize=10000,expireAfterWrite=10s
 *   data:
 *     redis:
 *       host: localhost
 *       port: 6379
 * }</pre>
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    /**
     * L1 Cache: Caffeine in-memory cache.
     *
     * <p>Fast in-memory cache for individual application instances.
     * Default configuration:
     * <ul>
     *   <li>Max size: 10,000 entries</li>
     *   <li>TTL: 10 seconds</li>
     *   <li>Stats recording enabled for monitoring</li>
     * </ul>
     *
     * @return Caffeine cache manager
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        log.info("Configuring L1 Cache (Caffeine)");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "authorizationCache",
                "authorizationCacheL1"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(10))
                .recordStats() // Enable statistics for monitoring
        );

        return cacheManager;
    }

    /**
     * L2 Cache: Redis distributed cache.
     *
     * <p>Shared cache across multiple application instances.
     * Default configuration:
     * <ul>
     *   <li>TTL: 5 minutes (300 seconds)</li>
     *   <li>JSON serialization with type information</li>
     *   <li>Null values not cached</li>
     * </ul>
     *
     * @param connectionFactory Redis connection factory
     * @return Redis cache manager
     */
    @Bean("redisCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring L2 Cache (Redis)");

        // Configure ObjectMapper for JSON serialization with type information
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }
}
