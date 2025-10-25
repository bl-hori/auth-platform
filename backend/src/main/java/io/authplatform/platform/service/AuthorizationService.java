package io.authplatform.platform.service;

import io.authplatform.platform.api.dto.AuthorizationRequest;
import io.authplatform.platform.api.dto.AuthorizationResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for authorization decisions.
 *
 * <p>This service evaluates authorization requests against policies, roles, and permissions
 * to determine whether access should be granted or denied.
 *
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Role-Based Access Control (RBAC)</li>
 *   <li>Attribute-Based Access Control (ABAC) via policies</li>
 *   <li>Policy evaluation with OPA integration</li>
 *   <li>Multi-layer caching (L1: Caffeine, L2: Redis)</li>
 *   <li>Batch authorization for performance</li>
 *   <li>Async evaluation for non-blocking operations</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * AuthorizationRequest request = AuthorizationRequest.builder()
 *     .organizationId(orgId)
 *     .principal(Principal.builder().id("user-123").type("user").build())
 *     .action("read")
 *     .resource(Resource.builder().type("document").id("doc-456").build())
 *     .build();
 *
 * AuthorizationResponse response = authorizationService.authorize(request);
 *
 * if (response.isAllowed()) {
 *     // Grant access
 * } else {
 *     // Deny access, log reason: response.getReason()
 * }
 * }</pre>
 *
 * @see AuthorizationRequest
 * @see AuthorizationResponse
 */
public interface AuthorizationService {

    /**
     * Evaluate a single authorization request synchronously.
     *
     * <p>This method determines whether the principal should be granted access
     * to perform the specified action on the resource.
     *
     * <p><strong>Evaluation Order:</strong>
     * <ol>
     *   <li>Check cache for recent decision</li>
     *   <li>Evaluate RBAC rules (roles and permissions)</li>
     *   <li>Evaluate ABAC policies (if configured)</li>
     *   <li>Return decision with reasoning</li>
     * </ol>
     *
     * <p><strong>Performance:</strong>
     * <ul>
     *   <li>Cached decisions: ~1-2ms (p95)</li>
     *   <li>RBAC evaluation: ~5-10ms (p95)</li>
     *   <li>Full policy evaluation: ~10-50ms (p95)</li>
     * </ul>
     *
     * @param request the authorization request
     * @return authorization response with decision and reasoning
     * @throws IllegalArgumentException if request is invalid
     */
    AuthorizationResponse authorize(AuthorizationRequest request);

    /**
     * Evaluate multiple authorization requests in batch.
     *
     * <p>Batch evaluation improves performance by:
     * <ul>
     *   <li>Sharing database queries across requests</li>
     *   <li>Bulk cache lookups</li>
     *   <li>Parallel policy evaluation</li>
     * </ul>
     *
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>UI rendering: Check multiple permissions at once</li>
     *   <li>Bulk operations: Validate access to multiple resources</li>
     *   <li>Report generation: Pre-check access to all required data</li>
     * </ul>
     *
     * @param requests list of authorization requests
     * @return list of authorization responses in the same order
     * @throws IllegalArgumentException if requests list is null or empty
     */
    List<AuthorizationResponse> authorizeBatch(List<AuthorizationRequest> requests);

    /**
     * Evaluate an authorization request asynchronously.
     *
     * <p>This method returns immediately with a CompletableFuture that will
     * complete with the authorization decision. Useful for non-blocking
     * operations and high-throughput scenarios.
     *
     * <p><strong>Benefits:</strong>
     * <ul>
     *   <li>Non-blocking thread execution</li>
     *   <li>Better resource utilization under load</li>
     *   <li>Composable with other async operations</li>
     * </ul>
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     * CompletableFuture<AuthorizationResponse> future =
     *     authorizationService.authorizeAsync(request);
     *
     * future.thenAccept(response -> {
     *     if (response.isAllowed()) {
     *         // Handle authorized access
     *     }
     * });
     * }</pre>
     *
     * @param request the authorization request
     * @return CompletableFuture that will complete with the authorization response
     */
    CompletableFuture<AuthorizationResponse> authorizeAsync(AuthorizationRequest request);

    /**
     * Invalidate cached authorization decisions for a principal.
     *
     * <p>Should be called when:
     * <ul>
     *   <li>User roles are changed</li>
     *   <li>User permissions are modified</li>
     *   <li>User attributes are updated (ABAC)</li>
     * </ul>
     *
     * @param organizationId the organization ID
     * @param principalId the principal identifier
     */
    void invalidateCache(java.util.UUID organizationId, String principalId);

    /**
     * Invalidate all cached authorization decisions for an organization.
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
    void invalidateCacheForOrganization(java.util.UUID organizationId);

    /**
     * Get authorization statistics for monitoring.
     *
     * <p>Returns metrics useful for:
     * <ul>
     *   <li>Performance monitoring</li>
     *   <li>Cache hit rate tracking</li>
     *   <li>Policy evaluation time analysis</li>
     * </ul>
     *
     * @return authorization statistics
     */
    AuthorizationStatistics getStatistics();

    /**
     * Authorization statistics for monitoring and observability.
     */
    interface AuthorizationStatistics {
        /**
         * Total number of authorization requests processed.
         */
        long getTotalRequests();

        /**
         * Number of authorization requests that were allowed.
         */
        long getAllowedRequests();

        /**
         * Number of authorization requests that were denied.
         */
        long getDeniedRequests();

        /**
         * Number of authorization requests that resulted in errors.
         */
        long getErrorRequests();

        /**
         * Cache hit rate (0.0 to 1.0).
         */
        double getCacheHitRate();

        /**
         * Average evaluation time in milliseconds.
         */
        double getAverageEvaluationTimeMs();

        /**
         * 95th percentile evaluation time in milliseconds.
         */
        double getP95EvaluationTimeMs();
    }
}
