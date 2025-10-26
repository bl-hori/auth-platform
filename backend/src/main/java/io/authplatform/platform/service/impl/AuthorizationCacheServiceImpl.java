package io.authplatform.platform.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.authplatform.platform.api.dto.AuthorizationRequest;
import io.authplatform.platform.api.dto.AuthorizationResponse;
import io.authplatform.platform.service.AuthorizationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of {@link AuthorizationCacheService} with multi-layer caching.
 *
 * <p>This implementation uses:
 * <ul>
 *   <li><strong>L1 Cache:</strong> Caffeine (in-memory, fast, 10s TTL)</li>
 *   <li><strong>L2 Cache:</strong> Redis (distributed, 5min TTL)</li>
 * </ul>
 *
 * <p><strong>Cache Flow:</strong>
 * <pre>
 * GET:
 * 1. Check L1 (Caffeine) → if hit, return
 * 2. Check L2 (Redis) → if hit, promote to L1 and return
 * 3. Return empty (cache miss)
 *
 * PUT:
 * 1. Store in L1 (Caffeine)
 * 2. Store in L2 (Redis)
 *
 * INVALIDATE:
 * 1. Remove from L1 by key pattern
 * 2. Remove from L2 by key pattern
 * </pre>
 */
@Service
@Slf4j
public class AuthorizationCacheServiceImpl implements AuthorizationCacheService {

    private final CacheManager caffeineCacheManager;
    private final CacheManager redisCacheManager;
    private final RedisConnectionFactory redisConnectionFactory;

    // Statistics tracking
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong l1Hits = new AtomicLong(0);
    private final AtomicLong l2Hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    private static final String L1_CACHE_NAME = "authorizationCacheL1";
    private static final String L2_CACHE_NAME = "authorizationCache";

    public AuthorizationCacheServiceImpl(
            CacheManager caffeineCacheManager,
            CacheManager redisCacheManager,
            RedisConnectionFactory redisConnectionFactory) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.redisCacheManager = redisCacheManager;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Optional<AuthorizationResponse> get(AuthorizationRequest request) {
        totalRequests.incrementAndGet();
        String cacheKey = buildCacheKey(request);

        log.debug("Cache lookup: key={}", cacheKey);

        // L1 Cache (Caffeine)
        org.springframework.cache.Cache l1Cache = caffeineCacheManager.getCache(L1_CACHE_NAME);
        if (l1Cache != null) {
            AuthorizationResponse l1Result = l1Cache.get(cacheKey, AuthorizationResponse.class);
            if (l1Result != null) {
                l1Hits.incrementAndGet();
                log.debug("L1 cache hit: key={}", cacheKey);
                return Optional.of(l1Result);
            }
        }

        // L2 Cache (Redis)
        org.springframework.cache.Cache l2Cache = redisCacheManager.getCache(L2_CACHE_NAME);
        if (l2Cache != null) {
            AuthorizationResponse l2Result = l2Cache.get(cacheKey, AuthorizationResponse.class);
            if (l2Result != null) {
                l2Hits.incrementAndGet();
                log.debug("L2 cache hit: key={}, promoting to L1", cacheKey);

                // Promote to L1 for faster subsequent access
                if (l1Cache != null) {
                    l1Cache.put(cacheKey, l2Result);
                }

                return Optional.of(l2Result);
            }
        }

        // Cache miss
        misses.incrementAndGet();
        log.debug("Cache miss: key={}", cacheKey);
        return Optional.empty();
    }

    @Override
    public void put(AuthorizationRequest request, AuthorizationResponse response) {
        String cacheKey = buildCacheKey(request);

        log.debug("Caching authorization decision: key={}, decision={}", cacheKey, response.getDecision());

        // Store in L1 (Caffeine)
        org.springframework.cache.Cache l1Cache = caffeineCacheManager.getCache(L1_CACHE_NAME);
        if (l1Cache != null) {
            l1Cache.put(cacheKey, response);
        }

        // Store in L2 (Redis)
        org.springframework.cache.Cache l2Cache = redisCacheManager.getCache(L2_CACHE_NAME);
        if (l2Cache != null) {
            l2Cache.put(cacheKey, response);
        }
    }

    @Override
    public void invalidate(UUID organizationId, String principalId) {
        log.info("Invalidating cache for principal: org={}, principal={}", organizationId, principalId);

        String keyPrefix = buildKeyPrefix(organizationId, principalId);

        // Invalidate L1 cache entries matching the prefix
        org.springframework.cache.Cache l1Cache = caffeineCacheManager.getCache(L1_CACHE_NAME);
        if (l1Cache instanceof CaffeineCache caffeineCache) {
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            nativeCache.asMap().keySet().stream()
                    .filter(key -> key.toString().startsWith(keyPrefix))
                    .forEach(nativeCache::invalidate);
        }

        // Invalidate L2 cache entries matching the prefix using Redis SCAN
        invalidateRedisByPattern(keyPrefix + "*");
    }

    @Override
    public void invalidateOrganization(UUID organizationId) {
        log.info("Invalidating cache for organization: {}", organizationId);

        String keyPrefix = organizationId.toString() + ":";

        // Invalidate L1 cache entries for the organization
        org.springframework.cache.Cache l1Cache = caffeineCacheManager.getCache(L1_CACHE_NAME);
        if (l1Cache instanceof CaffeineCache caffeineCache) {
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            nativeCache.asMap().keySet().stream()
                    .filter(key -> key.toString().startsWith(keyPrefix))
                    .forEach(nativeCache::invalidate);
        }

        // Invalidate L2 cache using Redis SCAN
        invalidateRedisByPattern(keyPrefix + "*");
    }

    @Override
    public void clearAll() {
        log.warn("Clearing all authorization caches (L1 and L2)");

        org.springframework.cache.Cache l1Cache = caffeineCacheManager.getCache(L1_CACHE_NAME);
        if (l1Cache != null) {
            l1Cache.clear();
        }

        org.springframework.cache.Cache l2Cache = redisCacheManager.getCache(L2_CACHE_NAME);
        if (l2Cache != null) {
            l2Cache.clear();
        }
    }

    @Override
    public CacheStatistics getStatistics() {
        long total = totalRequests.get();
        long l1Hit = l1Hits.get();
        long l2Hit = l2Hits.get();
        long miss = misses.get();

        double l1HitRate = total > 0 ? (double) l1Hit / total : 0.0;
        double l2HitRate = total > 0 ? (double) l2Hit / total : 0.0;
        double overallHitRate = total > 0 ? (double) (l1Hit + l2Hit) / total : 0.0;

        // Get L1 (Caffeine) statistics
        long l1Size = 0;
        long l1Evictions = 0;
        org.springframework.cache.Cache l1Cache = caffeineCacheManager.getCache(L1_CACHE_NAME);
        if (l1Cache instanceof CaffeineCache caffeineCache) {
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            l1Size = nativeCache.estimatedSize();

            CacheStats stats = nativeCache.stats();
            l1Evictions = stats.evictionCount();
        }

        long finalL1Size = l1Size;
        long finalL1Evictions = l1Evictions;

        return new CacheStatistics() {
            @Override
            public double getL1HitRate() {
                return l1HitRate;
            }

            @Override
            public double getL2HitRate() {
                return l2HitRate;
            }

            @Override
            public double getOverallHitRate() {
                return overallHitRate;
            }

            @Override
            public long getL1Size() {
                return finalL1Size;
            }

            @Override
            public long getL1EvictionCount() {
                return finalL1Evictions;
            }

            @Override
            public long getTotalRequests() {
                return total;
            }

            @Override
            public long getTotalHits() {
                return l1Hit + l2Hit;
            }

            @Override
            public long getTotalMisses() {
                return miss;
            }
        };
    }

    /**
     * Build cache key from authorization request.
     *
     * <p>Format: {orgId}:{principalId}:{action}:{resourceType}:{resourceId}
     *
     * @param request the authorization request
     * @return cache key
     */
    private String buildCacheKey(AuthorizationRequest request) {
        return String.format("%s:%s:%s:%s:%s",
                request.getOrganizationId(),
                request.getPrincipal().getId(),
                request.getAction(),
                request.getResource().getType(),
                request.getResource().getId()
        );
    }

    /**
     * Build cache key prefix for invalidation.
     *
     * @param organizationId the organization ID
     * @param principalId the principal ID
     * @return cache key prefix
     */
    private String buildKeyPrefix(UUID organizationId, String principalId) {
        return String.format("%s:%s:", organizationId, principalId);
    }

    /**
     * Invalidate Redis cache entries matching a pattern using SCAN.
     *
     * <p>This implementation uses Redis SCAN command to efficiently find and delete
     * keys matching the given pattern without blocking the Redis server.
     *
     * @param pattern the key pattern to match (e.g., "orgId:principalId:*")
     */
    private void invalidateRedisByPattern(String pattern) {
        try {
            String cacheKeyPattern = L2_CACHE_NAME + "::" + pattern;

            try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                ScanOptions options = ScanOptions.scanOptions()
                        .match(cacheKeyPattern)
                        .count(100)
                        .build();

                Cursor<byte[]> cursor = connection.scan(options);
                int deletedCount = 0;

                while (cursor.hasNext()) {
                    byte[] key = cursor.next();
                    connection.del(key);
                    deletedCount++;
                }

                if (deletedCount > 0) {
                    log.debug("Deleted {} cache entries matching pattern: {}", deletedCount, pattern);
                }
            }
        } catch (Exception e) {
            log.error("Failed to invalidate Redis cache by pattern: {}", pattern, e);
        }
    }
}
