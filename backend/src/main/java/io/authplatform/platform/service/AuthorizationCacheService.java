package io.authplatform.platform.service;

import io.authplatform.platform.api.dto.AuthorizationRequest;
import io.authplatform.platform.api.dto.AuthorizationResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for caching authorization decisions.
 *
 * <p>This service provides a multi-layer caching strategy:
 * <ul>
 *   <li><strong>L1 Cache (Caffeine):</strong> Fast in-memory cache (10s TTL, 10K entries)</li>
 *   <li><strong>L2 Cache (Redis):</strong> Distributed cache (5min TTL, shared across instances)</li>
 * </ul>
 *
 * <p><strong>Cache Key Strategy:</strong>
 * The cache key is composed of:
 * <ul>
 *   <li>Organization ID</li>
 *   <li>Principal ID</li>
 *   <li>Action</li>
 *   <li>Resource type</li>
 *   <li>Resource ID</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * // Check cache before evaluation
 * Optional<AuthorizationResponse> cached = cacheService.get(request);
 * if (cached.isPresent()) {
 *     return cached.get(); // Cache hit
 * }
 *
 * // Evaluate and cache result
 * AuthorizationResponse response = evaluateAuthorization(request);
 * cacheService.put(request, response);
 * return response;
 * }</pre>
 *
 * <p><strong>Cache Invalidation:</strong>
 * <ul>
 *   <li>When user roles change</li>
 *   <li>When role permissions change</li>
 *   <li>When policies are updated</li>
 *   <li>Manual invalidation via API</li>
 * </ul>
 */
public interface AuthorizationCacheService {

    /**
     * Get cached authorization response.
     *
     * <p>Checks L1 cache first, then L2 cache if L1 misses.
     * If found in L2, promotes to L1 for faster subsequent access.
     *
     * @param request the authorization request
     * @return cached response if found, empty otherwise
     */
    Optional<AuthorizationResponse> get(AuthorizationRequest request);

    /**
     * Cache an authorization response.
     *
     * <p>Stores in both L1 and L2 caches for redundancy and performance.
     *
     * @param request the authorization request
     * @param response the authorization response to cache
     */
    void put(AuthorizationRequest request, AuthorizationResponse response);

    /**
     * Invalidate cache for a specific principal.
     *
     * <p>Should be called when:
     * <ul>
     *   <li>User roles are added/removed</li>
     *   <li>User attributes change</li>
     *   <li>User is deleted/deactivated</li>
     * </ul>
     *
     * @param organizationId the organization ID
     * @param principalId the principal ID
     */
    void invalidate(UUID organizationId, String principalId);

    /**
     * Invalidate all cache entries for an organization.
     *
     * <p>Should be called when:
     * <ul>
     *   <li>Policies are updated</li>
     *   <li>Organization-wide role changes</li>
     *   <li>Security configuration changes</li>
     * </ul>
     *
     * @param organizationId the organization ID
     */
    void invalidateOrganization(UUID organizationId);

    /**
     * Clear all caches (L1 and L2).
     *
     * <p>Use with caution - this will clear all cached authorization decisions.
     * Primarily for testing or emergency situations.
     */
    void clearAll();

    /**
     * Get cache statistics.
     *
     * @return cache statistics including hit rate, size, etc.
     */
    CacheStatistics getStatistics();

    /**
     * Cache statistics for monitoring.
     */
    interface CacheStatistics {
        /**
         * L1 cache hit rate (0.0 to 1.0).
         */
        double getL1HitRate();

        /**
         * L2 cache hit rate (0.0 to 1.0).
         */
        double getL2HitRate();

        /**
         * Overall cache hit rate (0.0 to 1.0).
         */
        double getOverallHitRate();

        /**
         * Number of entries in L1 cache.
         */
        long getL1Size();

        /**
         * L1 cache eviction count.
         */
        long getL1EvictionCount();

        /**
         * Total cache requests.
         */
        long getTotalRequests();

        /**
         * Total cache hits.
         */
        long getTotalHits();

        /**
         * Total cache misses.
         */
        long getTotalMisses();
    }
}
