package io.authplatform.platform.service.impl;

import io.authplatform.platform.api.dto.AuthorizationRequest;
import io.authplatform.platform.api.dto.AuthorizationResponse;
import io.authplatform.platform.domain.entity.*;
import io.authplatform.platform.domain.repository.PermissionRepository;
import io.authplatform.platform.domain.repository.RolePermissionRepository;
import io.authplatform.platform.domain.repository.RoleRepository;
import io.authplatform.platform.domain.repository.UserRepository;
import io.authplatform.platform.domain.repository.UserRoleRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link RbacAuthorizationService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RbacAuthorizationService Tests")
class RbacAuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private io.authplatform.platform.config.OpaProperties opaProperties;

    @Mock
    private io.authplatform.platform.service.AuthorizationCacheService cacheService;

    private MeterRegistry meterRegistry;
    private RbacAuthorizationService authorizationService;

    private Organization testOrg;
    private User testUser;
    private Role viewerRole;
    private Permission readPermission;

    @BeforeEach
    void setUp() {
        // Create a real SimpleMeterRegistry for testing
        meterRegistry = new SimpleMeterRegistry();

        // Initialize service with real MeterRegistry
        authorizationService = new RbacAuthorizationService(
                userRepository,
                roleRepository,
                permissionRepository,
                userRoleRepository,
                rolePermissionRepository,
                opaProperties,
                cacheService,
                meterRegistry
        );

        // Disable OPA for tests (use RBAC only)
        lenient().when(opaProperties.isEnabled()).thenReturn(false);

        // Mock cache service to return empty (cache miss) by default
        lenient().when(cacheService.get(any())).thenReturn(Optional.empty());

        // Create test organization
        testOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("test-org")
                .displayName("Test Organization")
                .status(Organization.OrganizationStatus.ACTIVE)
                .build();

        // Create test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .email("user@example.com")
                .username("testuser")
                .externalId("user-123")
                .status(User.UserStatus.ACTIVE)
                .build();

        // Create viewer role
        viewerRole = Role.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .name("viewer")
                .displayName("Viewer")
                .level(0)
                .isSystem(false)
                .build();

        // Create read permission
        readPermission = Permission.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .name("document:read")
                .resourceType("document")
                .action("read")
                .effect(Permission.PermissionEffect.ALLOW)
                .build();
    }

    @Test
    @DisplayName("Should allow access when user has required permission")
    void shouldAllowAccessWhenUserHasPermission() {
        // Given: User with viewer role that has document:read permission
        RolePermission rolePermission = RolePermission.builder()
                .id(UUID.randomUUID())
                .role(viewerRole)
                .permission(readPermission)
                .build();

        UserRole userRole = UserRole.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .role(viewerRole)
                .build();

        when(userRepository.findByExternalIdAndDeletedAtIsNull("user-123"))
                .thenReturn(Optional.of(testUser));
        when(userRoleRepository.findNonExpiredByUserId(eq(testUser.getId()), any()))
                .thenReturn(List.of(userRole));
        when(rolePermissionRepository.findByRoleId(viewerRole.getId()))
                .thenReturn(List.of(rolePermission));

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
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

        // When: Authorize
        AuthorizationResponse response = authorizationService.authorize(request);

        // Then: Should allow
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.ALLOW);
        assertThat(response.getReason()).contains("viewer");
        assertThat(response.getReason()).contains("document:read");
        assertThat(response.getEvaluationTimeMs()).isNotNull();
        assertThat(response.getEvaluationTimeMs()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("Should deny access when user lacks required permission")
    void shouldDenyAccessWhenUserLacksPermission() {
        // Given: User with viewer role but no write permission
        UserRole userRole = UserRole.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .role(viewerRole)
                .build();

        when(userRepository.findByExternalIdAndDeletedAtIsNull("user-123"))
                .thenReturn(Optional.of(testUser));
        when(userRoleRepository.findNonExpiredByUserId(eq(testUser.getId()), any()))
                .thenReturn(List.of(userRole));
        when(rolePermissionRepository.findByRoleId(viewerRole.getId()))
                .thenReturn(List.of()); // No permissions

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("write")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .build())
                .build();

        // When: Authorize
        AuthorizationResponse response = authorizationService.authorize(request);

        // Then: Should deny
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.DENY);
        assertThat(response.getReason()).contains("lacks");
        assertThat(response.getReason()).contains("document:write");
    }

    @Test
    @DisplayName("Should deny access when user not found")
    void shouldDenyAccessWhenUserNotFound() {
        // Given: User does not exist
        when(userRepository.findByExternalIdAndDeletedAtIsNull("unknown-user"))
                .thenReturn(Optional.empty());

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("unknown-user")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .build())
                .build();

        // When: Authorize
        AuthorizationResponse response = authorizationService.authorize(request);

        // Then: Should deny
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.DENY);
        assertThat(response.getReason()).contains("User not found");
    }

    @Test
    @DisplayName("Should deny access when user has no roles")
    void shouldDenyAccessWhenUserHasNoRoles() {
        // Given: User with no roles
        when(userRepository.findByExternalIdAndDeletedAtIsNull("user-123"))
                .thenReturn(Optional.of(testUser));

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
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

        // When: Authorize
        AuthorizationResponse response = authorizationService.authorize(request);

        // Then: Should deny
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.DENY);
        assertThat(response.getReason()).contains("no roles assigned");
    }

    @Test
    @DisplayName("Should support role hierarchy")
    void shouldSupportRoleHierarchy() {
        // Given: Admin role with child viewer role
        Role adminRole = Role.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .name("admin")
                .displayName("Admin")
                .level(0)
                .isSystem(false)
                .build();

        viewerRole.setParentRole(adminRole);

        RolePermission rolePermission = RolePermission.builder()
                .id(UUID.randomUUID())
                .role(adminRole)
                .permission(readPermission)
                .build();

        UserRole userRole = UserRole.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .role(viewerRole) // User has viewer role
                .build();

        when(userRepository.findByExternalIdAndDeletedAtIsNull("user-123"))
                .thenReturn(Optional.of(testUser));
        when(userRoleRepository.findNonExpiredByUserId(eq(testUser.getId()), any()))
                .thenReturn(List.of(userRole));
        when(rolePermissionRepository.findByRoleId(viewerRole.getId()))
                .thenReturn(List.of()); // No permissions on viewer role
        when(rolePermissionRepository.findByRoleId(adminRole.getId()))
                .thenReturn(List.of(rolePermission)); // Permission on parent admin role

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
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

        // When: Authorize
        AuthorizationResponse response = authorizationService.authorize(request);

        // Then: Should allow due to inherited permission from admin role
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.ALLOW);
    }

    @Test
    @DisplayName("Should evaluate batch requests")
    void shouldEvaluateBatchRequests() {
        // Given: User with read permission
        RolePermission rolePermission = RolePermission.builder()
                .id(UUID.randomUUID())
                .role(viewerRole)
                .permission(readPermission)
                .build();

        UserRole userRole = UserRole.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .role(viewerRole)
                .build();

        when(userRepository.findByExternalIdAndDeletedAtIsNull("user-123"))
                .thenReturn(Optional.of(testUser));
        when(userRoleRepository.findNonExpiredByUserId(eq(testUser.getId()), any()))
                .thenReturn(List.of(userRole));
        when(rolePermissionRepository.findByRoleId(viewerRole.getId()))
                .thenReturn(List.of(rolePermission));

        List<AuthorizationRequest> requests = List.of(
                AuthorizationRequest.builder()
                        .organizationId(testOrg.getId())
                        .principal(AuthorizationRequest.Principal.builder()
                                .id("user-123")
                                .type("user")
                                .build())
                        .action("read")
                        .resource(AuthorizationRequest.Resource.builder()
                                .type("document")
                                .id("doc-1")
                                .build())
                        .build(),
                AuthorizationRequest.builder()
                        .organizationId(testOrg.getId())
                        .principal(AuthorizationRequest.Principal.builder()
                                .id("user-123")
                                .type("user")
                                .build())
                        .action("write")
                        .resource(AuthorizationRequest.Resource.builder()
                                .type("document")
                                .id("doc-2")
                                .build())
                        .build()
        );

        // When: Authorize batch
        List<AuthorizationResponse> responses = authorizationService.authorizeBatch(requests);

        // Then: Should return responses for all requests
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getDecision()).isEqualTo(AuthorizationResponse.Decision.ALLOW);
        assertThat(responses.get(1).getDecision()).isEqualTo(AuthorizationResponse.Decision.DENY);
    }

    @Test
    @DisplayName("Should reject null request")
    void shouldRejectNullRequest() {
        // When/Then: Should throw exception
        assertThatThrownBy(() -> authorizationService.authorize(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authorization request cannot be null");
    }

    @Test
    @DisplayName("Should reject request with null organization ID")
    void shouldRejectRequestWithNullOrganizationId() {
        // Given: Request with null organization ID
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(null)
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

        // When/Then: Should throw exception
        assertThatThrownBy(() -> authorizationService.authorize(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Organization ID cannot be null");
    }

    @Test
    @DisplayName("Should return error response on exception")
    void shouldReturnErrorResponseOnException() {
        // Given: Repository throws exception
        when(userRepository.findByExternalIdAndDeletedAtIsNull(anyString()))
                .thenThrow(new RuntimeException("Database error"));

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
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

        // When: Authorize
        AuthorizationResponse response = authorizationService.authorize(request);

        // Then: Should return error
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.ERROR);
        assertThat(response.getReason()).contains("error");
    }

    @Test
    @DisplayName("Should use cache when available")
    void shouldUseCacheWhenAvailable() {
        // Given: Cached response
        AuthorizationResponse cachedResponse = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("Cached decision")
                .evaluationTimeMs(0L)
                .build();

        when(cacheService.get(any())).thenReturn(Optional.of(cachedResponse));

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
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

        // When: Authorize
        AuthorizationResponse response = authorizationService.authorize(request);

        // Then: Should return cached response without hitting database
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.ALLOW);
        assertThat(response.getReason()).isEqualTo("Cached decision");
        verify(userRepository, never()).findByExternalIdAndDeletedAtIsNull(anyString());
        verify(cacheService).get(any());
    }

    @Test
    @DisplayName("Should cache authorization decisions")
    void shouldCacheAuthorizationDecisions() {
        // Given: User with permission
        RolePermission rolePermission = RolePermission.builder()
                .id(UUID.randomUUID())
                .role(viewerRole)
                .permission(readPermission)
                .build();

        UserRole userRole = UserRole.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .role(viewerRole)
                .build();

        when(userRepository.findByExternalIdAndDeletedAtIsNull("user-123"))
                .thenReturn(Optional.of(testUser));
        when(userRoleRepository.findNonExpiredByUserId(eq(testUser.getId()), any()))
                .thenReturn(List.of(userRole));
        when(rolePermissionRepository.findByRoleId(viewerRole.getId()))
                .thenReturn(List.of(rolePermission));

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
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

        // When: Authorize
        authorizationService.authorize(request);

        // Then: Should cache the result
        verify(cacheService).put(any(), any());
    }

    @Test
    @DisplayName("Should invalidate cache for principal")
    void shouldInvalidateCacheForPrincipal() {
        // Given: Organization and principal IDs
        UUID orgId = UUID.randomUUID();
        String principalId = "user-123";

        // When: Invalidate cache
        authorizationService.invalidateCache(orgId, principalId);

        // Then: Should call cache service
        verify(cacheService).invalidate(orgId, principalId);
    }

    @Test
    @DisplayName("Should invalidate cache for organization")
    void shouldInvalidateCacheForOrganization() {
        // Given: Organization ID
        UUID orgId = UUID.randomUUID();

        // When: Invalidate cache for organization
        authorizationService.invalidateCacheForOrganization(orgId);

        // Then: Should call cache service
        verify(cacheService).invalidateOrganization(orgId);
    }

    @Test
    @DisplayName("Should handle async authorization")
    void shouldHandleAsyncAuthorization() throws Exception {
        // Given: User with permission
        RolePermission rolePermission = RolePermission.builder()
                .id(UUID.randomUUID())
                .role(viewerRole)
                .permission(readPermission)
                .build();

        UserRole userRole = UserRole.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .role(viewerRole)
                .build();

        when(userRepository.findByExternalIdAndDeletedAtIsNull("user-123"))
                .thenReturn(Optional.of(testUser));
        when(userRoleRepository.findNonExpiredByUserId(eq(testUser.getId()), any()))
                .thenReturn(List.of(userRole));
        when(rolePermissionRepository.findByRoleId(viewerRole.getId()))
                .thenReturn(List.of(rolePermission));

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
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

        // When: Authorize asynchronously
        var future = authorizationService.authorizeAsync(request);
        AuthorizationResponse response = future.get();

        // Then: Should allow access
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.ALLOW);
        assertThat(response.getReason()).contains("permission via roles");
    }

    @Test
    @DisplayName("Should deny when user is inactive")
    void shouldDenyWhenUserIsInactive() {
        // Given: Inactive user
        User inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .email("inactive@example.com")
                .username("inactiveuser")
                .externalId("user-inactive")
                .status(User.UserStatus.INACTIVE)
                .build();

        when(userRepository.findByExternalIdAndDeletedAtIsNull("user-inactive"))
                .thenReturn(Optional.of(inactiveUser));

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-inactive")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-456")
                        .build())
                .build();

        // When: Authorize
        AuthorizationResponse response = authorizationService.authorize(request);

        // Then: Should deny access (inactive users typically have no roles)
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.DENY);
        assertThat(response.getReason()).contains("no roles");
    }

    @Test
    @DisplayName("Should track statistics")
    void shouldTrackStatistics() {
        // Given: Multiple authorization requests
        RolePermission rolePermission = RolePermission.builder()
                .id(UUID.randomUUID())
                .role(viewerRole)
                .permission(readPermission)
                .build();

        UserRole userRole = UserRole.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .role(viewerRole)
                .build();

        when(userRepository.findByExternalIdAndDeletedAtIsNull("user-123"))
                .thenReturn(Optional.of(testUser));
        when(userRoleRepository.findNonExpiredByUserId(eq(testUser.getId()), any()))
                .thenReturn(List.of(userRole));
        when(rolePermissionRepository.findByRoleId(viewerRole.getId()))
                .thenReturn(List.of(rolePermission));

        // Make several requests
        for (int i = 0; i < 5; i++) {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .organizationId(testOrg.getId())
                    .principal(AuthorizationRequest.Principal.builder()
                            .id("user-123")
                            .type("user")
                            .build())
                    .action(i % 2 == 0 ? "read" : "write")
                    .resource(AuthorizationRequest.Resource.builder()
                            .type("document")
                            .id("doc-" + i)
                            .build())
                    .build();
            authorizationService.authorize(request);
        }

        // When: Get statistics
        var stats = authorizationService.getStatistics();

        // Then: Should track requests
        assertThat(stats.getTotalRequests()).isEqualTo(5);
        assertThat(stats.getAllowedRequests()).isEqualTo(3); // read requests
        assertThat(stats.getDeniedRequests()).isEqualTo(2); // write requests
        assertThat(stats.getAverageEvaluationTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should record Prometheus metrics for authorization requests")
    void shouldRecordPrometheusMetrics() {
        // Given: User with permission
        RolePermission rolePermission = RolePermission.builder()
                .id(UUID.randomUUID())
                .role(viewerRole)
                .permission(readPermission)
                .build();

        UserRole userRole = UserRole.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .role(viewerRole)
                .build();

        when(userRepository.findByExternalIdAndDeletedAtIsNull("user-123"))
                .thenReturn(Optional.of(testUser));
        when(userRoleRepository.findNonExpiredByUserId(eq(testUser.getId()), any()))
                .thenReturn(List.of(userRole));
        when(rolePermissionRepository.findByRoleId(viewerRole.getId()))
                .thenReturn(List.of(rolePermission));

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-1")
                        .build())
                .build();

        // When: Make authorization request
        AuthorizationResponse response = authorizationService.authorize(request);

        // Then: Should record metrics
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.ALLOW);

        // Verify Prometheus metrics were recorded
        assertThat(meterRegistry.counter("authz.requests.total").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("authz.requests.allowed").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("authz.cache.misses").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("authz.decisions", "decision", "allow").count()).isEqualTo(1.0);
        assertThat(meterRegistry.timer("authz.evaluation.duration").count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should record cache hit metrics")
    void shouldRecordCacheHitMetrics() {
        // Given: Cached response
        AuthorizationResponse cachedResponse = AuthorizationResponse.builder()
                .decision(AuthorizationResponse.Decision.ALLOW)
                .reason("Cached decision")
                .evaluationTimeMs(0L)
                .build();

        when(cacheService.get(any())).thenReturn(Optional.of(cachedResponse));

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-1")
                        .build())
                .build();

        // When: Make authorization request (cache hit)
        authorizationService.authorize(request);

        // Then: Should record cache hit metric
        assertThat(meterRegistry.counter("authz.cache.hits").count()).isEqualTo(1.0);
        // Total requests counter should NOT be incremented for cache hits
        assertThat(meterRegistry.counter("authz.requests.total").count()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should record metrics for denied requests")
    void shouldRecordMetricsForDeniedRequests() {
        // Given: User without permission
        when(userRepository.findByExternalIdAndDeletedAtIsNull("user-456"))
                .thenReturn(Optional.of(testUser));
        when(userRoleRepository.findNonExpiredByUserId(eq(testUser.getId()), any()))
                .thenReturn(List.of());

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-456")
                        .type("user")
                        .build())
                .action("delete")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-2")
                        .build())
                .build();

        // When: Make authorization request
        AuthorizationResponse response = authorizationService.authorize(request);

        // Then: Should record denied metrics
        assertThat(response.getDecision()).isEqualTo(AuthorizationResponse.Decision.DENY);
        assertThat(meterRegistry.counter("authz.requests.total").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("authz.requests.denied").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("authz.decisions", "decision", "deny").count()).isEqualTo(1.0);
    }
}
