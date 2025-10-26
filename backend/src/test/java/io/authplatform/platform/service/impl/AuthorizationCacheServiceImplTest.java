package io.authplatform.platform.service.impl;

import io.authplatform.platform.api.dto.AuthorizationRequest;
import io.authplatform.platform.api.dto.AuthorizationResponse;
import io.authplatform.platform.service.AuthorizationCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link AuthorizationCacheServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationCacheService Tests")
class AuthorizationCacheServiceImplTest {

    @Mock
    private CacheManager caffeineCacheManager;

    @Mock
    private CacheManager redisCacheManager;

    @Mock
    private Cache l1Cache;

    @Mock
    private Cache l2Cache;

    @Mock
    private org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;

    private AuthorizationCacheService cacheService;

    private UUID testOrgId;
    private AuthorizationRequest testRequest;
    private AuthorizationResponse testResponse;

    @BeforeEach
    void setUp() {
        testOrgId = UUID.randomUUID();

        testRequest = AuthorizationRequest.builder()
                .organizationId(testOrgId)
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .build())
                .build();

        testResponse = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("Test response")
                .evaluationTimeMs(10L)
                .build();

        // Set up cache manager mocks (using lenient to avoid unnecessary stubbing exceptions)
        lenient().when(caffeineCacheManager.getCache("authorizationCacheL1")).thenReturn(l1Cache);
        lenient().when(redisCacheManager.getCache("authorizationCache")).thenReturn(l2Cache);

        cacheService = new AuthorizationCacheServiceImpl(caffeineCacheManager, redisCacheManager, redisConnectionFactory);
    }

    @Test
    @DisplayName("Should return cached response from L1 cache")
    void shouldReturnCachedResponseFromL1() {
        // Given: Response cached in L1
        when(l1Cache.get(anyString(), eq(AuthorizationResponse.class))).thenReturn(testResponse);

        // When: Get from cache
        Optional<AuthorizationResponse> result = cacheService.get(testRequest);

        // Then: Should return L1 cached response
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testResponse);

        // Should not check L2 cache
        verify(l2Cache, never()).get(anyString(), eq(AuthorizationResponse.class));
    }

    @Test
    @DisplayName("Should return cached response from L2 cache and promote to L1")
    void shouldReturnCachedResponseFromL2AndPromote() {
        // Given: L1 miss, L2 hit
        when(l1Cache.get(anyString(), eq(AuthorizationResponse.class))).thenReturn(null);
        when(l2Cache.get(anyString(), eq(AuthorizationResponse.class))).thenReturn(testResponse);

        // When: Get from cache
        Optional<AuthorizationResponse> result = cacheService.get(testRequest);

        // Then: Should return L2 cached response
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testResponse);

        // Should promote to L1
        verify(l1Cache).put(anyString(), eq(testResponse));
    }

    @Test
    @DisplayName("Should return empty on cache miss")
    void shouldReturnEmptyOnCacheMiss() {
        // Given: Both L1 and L2 miss
        when(l1Cache.get(anyString(), eq(AuthorizationResponse.class))).thenReturn(null);
        when(l2Cache.get(anyString(), eq(AuthorizationResponse.class))).thenReturn(null);

        // When: Get from cache
        Optional<AuthorizationResponse> result = cacheService.get(testRequest);

        // Then: Should return empty
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should put response in both L1 and L2 caches")
    void shouldPutResponseInBothCaches() {
        // When: Put in cache
        cacheService.put(testRequest, testResponse);

        // Then: Should store in both caches
        verify(l1Cache).put(anyString(), eq(testResponse));
        verify(l2Cache).put(anyString(), eq(testResponse));
    }

    @Test
    @DisplayName("Should invalidate cache for principal")
    void shouldInvalidateCacheForPrincipal() {
        // When: Invalidate cache for principal
        cacheService.invalidate(testOrgId, "user-123");

        // Then: Should attempt invalidation (Note: Implementation uses Caffeine native cache)
        // This test verifies the method executes without error
        verify(caffeineCacheManager).getCache("authorizationCacheL1");
        // Note: Redis SCAN-based invalidation will be called but we don't verify internal details
    }

    @Test
    @DisplayName("Should invalidate cache for organization")
    void shouldInvalidateCacheForOrganization() {
        // When: Invalidate cache for organization
        cacheService.invalidateOrganization(testOrgId);

        // Then: Should attempt invalidation (Note: Implementation uses Caffeine native cache)
        // This test verifies the method executes without error
        verify(caffeineCacheManager).getCache("authorizationCacheL1");
        // Note: Redis SCAN-based invalidation will be called but we don't verify internal details
    }

    @Test
    @DisplayName("Should clear all caches")
    void shouldClearAllCaches() {
        // When: Clear all caches
        cacheService.clearAll();

        // Then: Should clear both L1 and L2
        verify(l1Cache).clear();
        verify(l2Cache).clear();
    }

    @Test
    @DisplayName("Should track cache statistics")
    void shouldTrackCacheStatistics() {
        // Given: Multiple cache operations
        when(l1Cache.get(anyString(), eq(AuthorizationResponse.class)))
                .thenReturn(testResponse)  // First request: L1 hit
                .thenReturn(null)          // Second request: L1 miss
                .thenReturn(null);         // Third request: L1 miss

        when(l2Cache.get(anyString(), eq(AuthorizationResponse.class)))
                .thenReturn(testResponse)  // Second request: L2 hit
                .thenReturn(null);         // Third request: L2 miss

        // Make requests
        cacheService.get(testRequest); // L1 hit
        cacheService.get(testRequest); // L1 miss, L2 hit
        cacheService.get(testRequest); // L1 miss, L2 miss

        // When: Get statistics
        AuthorizationCacheService.CacheStatistics stats = cacheService.getStatistics();

        // Then: Should track correctly
        assertThat(stats.getTotalRequests()).isEqualTo(3);
        assertThat(stats.getTotalHits()).isEqualTo(2); // 1 L1 hit + 1 L2 hit
        assertThat(stats.getTotalMisses()).isEqualTo(1);
        assertThat(stats.getL1HitRate()).isEqualTo(1.0 / 3.0);
        assertThat(stats.getL2HitRate()).isEqualTo(1.0 / 3.0);
        assertThat(stats.getOverallHitRate()).isEqualTo(2.0 / 3.0);
    }

    @Test
    @DisplayName("Should handle null cache managers gracefully")
    void shouldHandleNullCacheManagersGracefully() {
        // Given: Cache managers return null
        when(caffeineCacheManager.getCache(anyString())).thenReturn(null);
        when(redisCacheManager.getCache(anyString())).thenReturn(null);

        // When: Perform operations
        Optional<AuthorizationResponse> result = cacheService.get(testRequest);
        cacheService.put(testRequest, testResponse);
        cacheService.clearAll();

        // Then: Should not throw exceptions
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should build correct cache key format")
    void shouldBuildCorrectCacheKeyFormat() {
        // Given: Request with known values
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrgId)
                .principal(AuthorizationRequest.Principal.builder()
                        .id("principal-id")
                        .type("user")
                        .build())
                .action("test-action")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("test-resource")
                        .id("resource-id")
                        .build())
                .build();

        // When: Get from cache
        cacheService.get(request);

        // Then: Should use correct key format: orgId:principalId:action:resourceType:resourceId
        String expectedKeyFormat = testOrgId + ":principal-id:test-action:test-resource:resource-id";
        verify(l1Cache).get(eq(expectedKeyFormat), eq(AuthorizationResponse.class));
    }

    @Test
    @DisplayName("Should track statistics with no requests")
    void shouldTrackStatisticsWithNoRequests() {
        // When: Get statistics without any requests
        AuthorizationCacheService.CacheStatistics stats = cacheService.getStatistics();

        // Then: Should return zeros
        assertThat(stats.getTotalRequests()).isEqualTo(0);
        assertThat(stats.getTotalHits()).isEqualTo(0);
        assertThat(stats.getTotalMisses()).isEqualTo(0);
        assertThat(stats.getL1HitRate()).isEqualTo(0.0);
        assertThat(stats.getL2HitRate()).isEqualTo(0.0);
        assertThat(stats.getOverallHitRate()).isEqualTo(0.0);
    }
}
