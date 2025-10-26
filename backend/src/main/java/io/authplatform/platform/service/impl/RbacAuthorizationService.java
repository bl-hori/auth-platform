package io.authplatform.platform.service.impl;

import io.authplatform.platform.api.dto.AuthorizationRequest;
import io.authplatform.platform.api.dto.AuthorizationResponse;
import io.authplatform.platform.config.OpaProperties;
import io.authplatform.platform.domain.entity.Permission;
import io.authplatform.platform.domain.entity.Role;
import io.authplatform.platform.domain.entity.RolePermission;
import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.domain.entity.UserRole;
import io.authplatform.platform.domain.repository.PermissionRepository;
import io.authplatform.platform.domain.repository.RolePermissionRepository;
import io.authplatform.platform.domain.repository.RoleRepository;
import io.authplatform.platform.domain.repository.UserRepository;
import io.authplatform.platform.domain.repository.UserRoleRepository;
import io.authplatform.platform.opa.client.OpaClient;
import io.authplatform.platform.opa.dto.OpaRequest;
import io.authplatform.platform.opa.dto.OpaResponse;
import io.authplatform.platform.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Role-Based Access Control (RBAC) implementation of {@link AuthorizationService}.
 *
 * <p>This implementation evaluates authorization requests based on:
 * <ul>
 *   <li>User roles and role hierarchy</li>
 *   <li>Role permissions</li>
 *   <li>Resource-scoped role assignments</li>
 * </ul>
 *
 * <p><strong>Evaluation Logic:</strong>
 * <ol>
 *   <li>Find user by principal ID</li>
 *   <li>Get all roles assigned to the user (including inherited roles)</li>
 *   <li>Get all permissions from those roles</li>
 *   <li>Check if any permission matches the requested action and resource type</li>
 * </ol>
 *
 * <p><strong>Future Enhancements:</strong>
 * <ul>
 *   <li>Add L1 cache (Caffeine) for frequently accessed decisions</li>
 *   <li>Add L2 cache (Redis) for distributed caching</li>
 *   <li>Integrate with OPA for policy-based evaluation</li>
 *   <li>Add circuit breaker for external dependencies</li>
 * </ul>
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class RbacAuthorizationService implements AuthorizationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final OpaProperties opaProperties;

    @Autowired(required = false)
    private OpaClient opaClient;

    public RbacAuthorizationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository,
            OpaProperties opaProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.opaProperties = opaProperties;
    }

    // In-memory statistics (will be replaced with Prometheus metrics in Task 3.10)
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong allowedRequests = new AtomicLong(0);
    private final AtomicLong deniedRequests = new AtomicLong(0);
    private final AtomicLong errorRequests = new AtomicLong(0);
    private final Map<String, Long> evaluationTimes = new ConcurrentHashMap<>();

    @Override
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        // Validate request first (this will throw IllegalArgumentException if invalid)
        validateRequest(request);

        long startTime = System.currentTimeMillis();
        totalRequests.incrementAndGet();

        try {
            log.debug("Evaluating authorization request: org={}, principal={}, action={}, resource={}",
                    request.getOrganizationId(),
                    request.getPrincipal().getId(),
                    request.getAction(),
                    request.getResource().getType() + ":" + request.getResource().getId());

            // Evaluate authorization
            AuthorizationResponse response = evaluateAuthorization(request, startTime);

            // Update statistics
            updateStatistics(response);

            // Record evaluation time
            long evaluationTime = response.getEvaluationTimeMs();
            evaluationTimes.put(UUID.randomUUID().toString(), evaluationTime);

            // Keep only recent evaluation times (last 1000)
            if (evaluationTimes.size() > 1000) {
                String oldestKey = evaluationTimes.keySet().iterator().next();
                evaluationTimes.remove(oldestKey);
            }

            log.debug("Authorization decision: {} ({}ms)", response.getDecision(), evaluationTime);

            return response;

        } catch (Exception e) {
            log.error("Error evaluating authorization request", e);
            errorRequests.incrementAndGet();

            long evaluationTime = System.currentTimeMillis() - startTime;
            return AuthorizationResponse.builder()
                    .decision(AuthorizationResponse.Decision.ERROR)
                    .reason("Authorization evaluation error: " + e.getMessage())
                    .timestamp(OffsetDateTime.now())
                    .evaluationTimeMs(evaluationTime)
                    .build();
        }
    }

    @Override
    public List<AuthorizationResponse> authorizeBatch(List<AuthorizationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Requests list cannot be null or empty");
        }

        log.debug("Evaluating batch authorization: {} requests", requests.size());

        // For now, evaluate sequentially
        // TODO: Optimize with parallel evaluation and bulk database queries
        return requests.stream()
                .map(this::authorize)
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<AuthorizationResponse> authorizeAsync(AuthorizationRequest request) {
        return CompletableFuture.supplyAsync(() -> authorize(request));
    }

    @Override
    public void invalidateCache(UUID organizationId, String principalId) {
        // TODO: Implement cache invalidation in Task 3.5-3.6
        log.debug("Cache invalidation requested for principal: org={}, principal={}",
                organizationId, principalId);
    }

    @Override
    public void invalidateCacheForOrganization(UUID organizationId) {
        // TODO: Implement cache invalidation in Task 3.5-3.6
        log.debug("Cache invalidation requested for organization: {}", organizationId);
    }

    @Override
    public AuthorizationStatistics getStatistics() {
        return new AuthorizationStatisticsImpl(
                totalRequests.get(),
                allowedRequests.get(),
                deniedRequests.get(),
                errorRequests.get(),
                0.0, // No cache yet
                calculateAverageEvaluationTime(),
                calculateP95EvaluationTime()
        );
    }

    /**
     * Evaluate authorization based on RBAC rules and optionally OPA policies.
     */
    private AuthorizationResponse evaluateAuthorization(AuthorizationRequest request, long startTime) {
        String principalId = request.getPrincipal().getId();
        UUID organizationId = request.getOrganizationId();
        String action = request.getAction();
        String resourceType = request.getResource().getType();

        // 1. Find user
        Optional<User> userOpt = userRepository.findByExternalIdAndDeletedAtIsNull(principalId);
        if (userOpt.isEmpty()) {
            long evaluationTime = System.currentTimeMillis() - startTime;
            return AuthorizationResponse.builder()
                    .decision(AuthorizationResponse.Decision.DENY)
                    .reason("User not found: " + principalId)
                    .timestamp(OffsetDateTime.now())
                    .evaluationTimeMs(evaluationTime)
                    .build();
        }

        User user = userOpt.get();

        // Verify user belongs to the correct organization
        if (!user.getOrganization().getId().equals(organizationId)) {
            long evaluationTime = System.currentTimeMillis() - startTime;
            return AuthorizationResponse.builder()
                    .decision(AuthorizationResponse.Decision.DENY)
                    .reason("User does not belong to organization")
                    .timestamp(OffsetDateTime.now())
                    .evaluationTimeMs(evaluationTime)
                    .build();
        }

        // OPA Policy Evaluation (if enabled)
        if (opaProperties.isEnabled() && opaClient != null) {
            try {
                AuthorizationResponse opaResponse = evaluateWithOpa(request, user, startTime);
                if (opaResponse != null) {
                    log.debug("OPA policy decision returned: {}", opaResponse.getDecision());
                    return opaResponse;
                }
            } catch (Exception e) {
                log.warn("OPA evaluation failed, falling back to RBAC: {}", e.getMessage());
                // Fall through to RBAC evaluation
            }
        }

        // 2. Get user's roles (including inherited roles from hierarchy)
        Set<Role> userRoles = getUserRolesWithHierarchy(user);

        if (userRoles.isEmpty()) {
            long evaluationTime = System.currentTimeMillis() - startTime;
            return AuthorizationResponse.builder()
                    .decision(AuthorizationResponse.Decision.DENY)
                    .reason("User has no roles assigned")
                    .timestamp(OffsetDateTime.now())
                    .evaluationTimeMs(evaluationTime)
                    .context(Map.of("matchedRoles", List.of()))
                    .build();
        }

        // 3. Get permissions from roles
        Set<Permission> permissions = getPermissionsFromRoles(userRoles);

        // 4. Check if any permission matches the request
        Optional<Permission> matchingPermission = permissions.stream()
                .filter(p -> p.getResourceType().equalsIgnoreCase(resourceType))
                .filter(p -> p.getAction().equalsIgnoreCase(action))
                .filter(p -> p.getEffect() == Permission.PermissionEffect.ALLOW)
                .findFirst();

        long evaluationTime = System.currentTimeMillis() - startTime;

        if (matchingPermission.isPresent()) {
            Permission permission = matchingPermission.get();
            List<String> matchedRoleNames = userRoles.stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());

            return AuthorizationResponse.builder()
                    .decision(AuthorizationResponse.Decision.ALLOW)
                    .reason(String.format("User has '%s:%s' permission via roles: %s",
                            resourceType, action, String.join(", ", matchedRoleNames)))
                    .timestamp(OffsetDateTime.now())
                    .evaluationTimeMs(evaluationTime)
                    .context(Map.of(
                            "matchedRoles", matchedRoleNames,
                            "matchedPermissions", List.of(permission.getName()),
                            "cacheHit", false
                    ))
                    .build();
        } else {
            return AuthorizationResponse.builder()
                    .decision(AuthorizationResponse.Decision.DENY)
                    .reason(String.format("User lacks '%s:%s' permission", resourceType, action))
                    .timestamp(OffsetDateTime.now())
                    .evaluationTimeMs(evaluationTime)
                    .context(Map.of(
                            "userRoles", userRoles.stream().map(Role::getName).collect(Collectors.toList()),
                            "availablePermissions", permissions.stream()
                                    .map(p -> p.getResourceType() + ":" + p.getAction())
                                    .collect(Collectors.toList())
                    ))
                    .build();
        }
    }

    /**
     * Get all roles for a user, including inherited roles from hierarchy.
     */
    private Set<Role> getUserRolesWithHierarchy(User user) {
        Set<Role> allRoles = new HashSet<>();

        // Get direct roles from user_roles table using repository
        List<UserRole> userRoles = userRoleRepository.findNonExpiredByUserId(
                user.getId(),
                OffsetDateTime.now()
        );

        userRoles.forEach(ur -> {
            Role role = ur.getRole();
            allRoles.add(role);

            // Add parent roles (role hierarchy)
            addParentRoles(role, allRoles);
        });

        return allRoles;
    }

    /**
     * Recursively add parent roles to support role hierarchy.
     */
    private void addParentRoles(Role role, Set<Role> allRoles) {
        Role parentRole = role.getParentRole();
        if (parentRole != null && !allRoles.contains(parentRole)) {
            allRoles.add(parentRole);
            addParentRoles(parentRole, allRoles);
        }
    }

    /**
     * Get all permissions from a set of roles.
     */
    private Set<Permission> getPermissionsFromRoles(Set<Role> roles) {
        Set<Permission> permissions = new HashSet<>();

        for (Role role : roles) {
            // Get role permissions from repository
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(role.getId());
            rolePermissions.forEach(rp -> {
                permissions.add(rp.getPermission());
            });
        }

        return permissions;
    }

    /**
     * Evaluate authorization using OPA (Open Policy Agent).
     */
    private AuthorizationResponse evaluateWithOpa(AuthorizationRequest request, User user, long startTime) {
        try {
            // Build OPA request
            Map<String, Object> principal = new HashMap<>();
            principal.put("id", request.getPrincipal().getId());
            principal.put("type", request.getPrincipal().getType());
            if (request.getPrincipal().getAttributes() != null) {
                principal.putAll(request.getPrincipal().getAttributes());
            }
            // Add user details
            principal.put("email", user.getEmail());
            principal.put("username", user.getUsername());
            principal.put("organization_id", user.getOrganization().getId().toString());

            Map<String, Object> resource = new HashMap<>();
            resource.put("type", request.getResource().getType());
            resource.put("id", request.getResource().getId());
            if (request.getResource().getAttributes() != null) {
                resource.putAll(request.getResource().getAttributes());
            }

            Map<String, Object> context = request.getContext() != null
                    ? new HashMap<>(request.getContext())
                    : new HashMap<>();
            context.put("timestamp", OffsetDateTime.now().toString());
            context.put("organization_id", request.getOrganizationId().toString());

            OpaRequest opaRequest = OpaRequest.builder()
                    .input(OpaRequest.OpaInput.builder()
                            .principal(principal)
                            .action(request.getAction())
                            .resource(resource)
                            .context(context)
                            .build())
                    .build();

            // Call OPA
            OpaResponse opaResponse = opaClient.evaluatePolicy(opaRequest);

            long evaluationTime = System.currentTimeMillis() - startTime;

            // Build authorization response from OPA result
            AuthorizationResponse.Decision decision = opaResponse.getResult().isAllow()
                    ? AuthorizationResponse.Decision.ALLOW
                    : AuthorizationResponse.Decision.DENY;

            String reason = opaResponse.getResult().getReasons() != null && !opaResponse.getResult().getReasons().isEmpty()
                    ? String.join("; ", opaResponse.getResult().getReasons())
                    : (decision == AuthorizationResponse.Decision.ALLOW ? "OPA policy allows access" : "OPA policy denies access");

            // Build applied policies
            List<AuthorizationResponse.AppliedPolicy> appliedPolicies = new ArrayList<>();
            if (opaResponse.getResult().getMatchedPolicies() != null) {
                for (String policyId : opaResponse.getResult().getMatchedPolicies()) {
                    appliedPolicies.add(AuthorizationResponse.AppliedPolicy.builder()
                            .policyId(policyId)
                            .policyName(policyId)
                            .effect(decision == AuthorizationResponse.Decision.ALLOW ? "allow" : "deny")
                            .build());
                }
            }

            Map<String, Object> responseContext = new HashMap<>();
            responseContext.put("evaluationMethod", "OPA");
            responseContext.put("cacheHit", false);
            if (opaResponse.getResult().getMetadata() != null) {
                responseContext.putAll(opaResponse.getResult().getMetadata());
            }

            return AuthorizationResponse.builder()
                    .decision(decision)
                    .reason(reason)
                    .timestamp(OffsetDateTime.now())
                    .evaluationTimeMs(evaluationTime)
                    .appliedPolicies(appliedPolicies)
                    .context(responseContext)
                    .build();

        } catch (OpaClient.OpaClientException e) {
            log.error("OPA evaluation failed: {}", e.getMessage());
            throw e; // Re-throw to trigger fallback to RBAC
        }
    }

    /**
     * Validate authorization request.
     */
    private void validateRequest(AuthorizationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Authorization request cannot be null");
        }
        if (request.getOrganizationId() == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (request.getPrincipal() == null) {
            throw new IllegalArgumentException("Principal cannot be null");
        }
        if (request.getAction() == null || request.getAction().isBlank()) {
            throw new IllegalArgumentException("Action cannot be null or blank");
        }
        if (request.getResource() == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }
    }

    /**
     * Update statistics based on response.
     */
    private void updateStatistics(AuthorizationResponse response) {
        switch (response.getDecision()) {
            case ALLOW:
                allowedRequests.incrementAndGet();
                break;
            case DENY:
                deniedRequests.incrementAndGet();
                break;
            case ERROR:
                errorRequests.incrementAndGet();
                break;
        }
    }

    /**
     * Calculate average evaluation time.
     */
    private double calculateAverageEvaluationTime() {
        if (evaluationTimes.isEmpty()) {
            return 0.0;
        }
        return evaluationTimes.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculate 95th percentile evaluation time.
     */
    private double calculateP95EvaluationTime() {
        if (evaluationTimes.isEmpty()) {
            return 0.0;
        }
        List<Long> sortedTimes = evaluationTimes.values().stream()
                .sorted()
                .collect(Collectors.toList());

        int p95Index = (int) Math.ceil(sortedTimes.size() * 0.95) - 1;
        if (p95Index < 0) {
            p95Index = 0;
        }
        return sortedTimes.get(p95Index).doubleValue();
    }

    /**
     * Implementation of AuthorizationStatistics.
     */
    private record AuthorizationStatisticsImpl(
            long totalRequests,
            long allowedRequests,
            long deniedRequests,
            long errorRequests,
            double cacheHitRate,
            double averageEvaluationTimeMs,
            double p95EvaluationTimeMs
    ) implements AuthorizationStatistics {
        @Override
        public long getTotalRequests() {
            return totalRequests;
        }

        @Override
        public long getAllowedRequests() {
            return allowedRequests;
        }

        @Override
        public long getDeniedRequests() {
            return deniedRequests;
        }

        @Override
        public long getErrorRequests() {
            return errorRequests;
        }

        @Override
        public double getCacheHitRate() {
            return cacheHitRate;
        }

        @Override
        public double getAverageEvaluationTimeMs() {
            return averageEvaluationTimeMs;
        }

        @Override
        public double getP95EvaluationTimeMs() {
            return p95EvaluationTimeMs;
        }
    }
}
