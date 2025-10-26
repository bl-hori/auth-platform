package io.authplatform.platform.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.authplatform.platform.api.dto.AuthorizationRequest;
import io.authplatform.platform.api.dto.BatchAuthorizationRequest;
import io.authplatform.platform.domain.entity.*;
import io.authplatform.platform.domain.repository.*;
import io.authplatform.platform.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration tests for {@link AuthorizationController}.
 *
 * <p>These tests verify the complete authorization flow from HTTP request to database,
 * including authentication, validation, authorization logic, and response formatting.
 *
 * <p><strong>Test Coverage:</strong>
 * <ul>
 *   <li>Single authorization requests (POST /v1/authorize)</li>
 *   <li>Batch authorization requests (POST /v1/authorize/batch)</li>
 *   <li>Role-based access control (RBAC) evaluation</li>
 *   <li>Permission checking with role hierarchy</li>
 *   <li>Input validation and error handling</li>
 *   <li>API key authentication</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "auth-platform.security.api-keys.keys.test-integration-key=test-org",
        "auth-platform.rate-limit.enabled=false"  // Disable rate limiting for integration tests
})
@DisplayName("Authorization Controller Integration Tests")
class AuthorizationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    private Organization testOrg;
    private User testUser;
    private Role viewerRole;
    private Role editorRole;
    private Permission readPermission;
    private Permission writePermission;

    @BeforeEach
    public void setUp() {
        // Create test organization
        testOrg = Organization.builder()
                .name("test-org")
                .displayName("Test Organization")
                .status(Organization.OrganizationStatus.ACTIVE)
                .build();
        testOrg = organizationRepository.save(testOrg);

        // Create test user
        testUser = User.builder()
                .organization(testOrg)
                .email("testuser@example.com")
                .username("testuser")
                .externalId("user-123")
                .status(User.UserStatus.ACTIVE)
                .build();
        testUser = userRepository.save(testUser);

        // Create roles
        viewerRole = Role.builder()
                .organization(testOrg)
                .name("viewer")
                .description("Can view resources")
                .build();
        viewerRole = roleRepository.save(viewerRole);

        editorRole = Role.builder()
                .organization(testOrg)
                .name("editor")
                .description("Can edit resources")
                .parentRole(viewerRole)  // editor inherits from viewer
                .build();
        editorRole = roleRepository.save(editorRole);

        // Create permissions
        readPermission = Permission.builder()
                .organization(testOrg)
                .name("document:read")
                .resourceType("document")
                .action("read")
                .effect(Permission.PermissionEffect.ALLOW)
                .build();
        readPermission = permissionRepository.save(readPermission);

        writePermission = Permission.builder()
                .organization(testOrg)
                .name("document:write")
                .resourceType("document")
                .action("write")
                .effect(Permission.PermissionEffect.ALLOW)
                .build();
        writePermission = permissionRepository.save(writePermission);

        // Assign permissions to roles
        RolePermission viewerReadPermission = RolePermission.builder()
                .role(viewerRole)
                .permission(readPermission)
                .build();
        rolePermissionRepository.save(viewerReadPermission);

        RolePermission editorWritePermission = RolePermission.builder()
                .role(editorRole)
                .permission(writePermission)
                .build();
        rolePermissionRepository.save(editorWritePermission);

        // Assign viewer role to test user
        UserRole userRole = UserRole.builder()
                .user(testUser)
                .role(viewerRole)
                .build();
        userRoleRepository.save(userRole);
    }

    @Test
    @DisplayName("Should allow access when user has required permission")
    void shouldAllowAccessWhenUserHasPermission() throws Exception {
        // Given: Request for read access to document
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

        // When/Then: Should allow access
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", "test-integration-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("ALLOW"))
                .andExpect(jsonPath("$.reason", containsString("permission")))
                .andExpect(jsonPath("$.evaluationTimeMs").isNumber())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should deny access when user lacks required permission")
    void shouldDenyAccessWhenUserLacksPermission() throws Exception {
        // Given: Request for write access (user only has read permission)
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-123")
                        .type("user")
                        .build())
                .action("write")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-1")
                        .build())
                .build();

        // When/Then: Should deny access
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", "test-integration-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DENY"))
                .andExpect(jsonPath("$.reason", containsString("permission")));
    }

    @Test
    @DisplayName("Should deny access when user not found")
    void shouldDenyAccessWhenUserNotFound() throws Exception {
        // Given: Request for non-existent user
        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("non-existent-user")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-1")
                        .build())
                .build();

        // When/Then: Should deny access
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", "test-integration-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DENY"))
                .andExpect(jsonPath("$.reason", containsString("User not found")));
    }

    @Test
    @DisplayName("Should support role hierarchy")
    void shouldSupportRoleHierarchy() throws Exception {
        // Given: User with editor role (which inherits from viewer)
        User editorUser = User.builder()
                .organization(testOrg)
                .email("editor@example.com")
                .username("editoruser")
                .externalId("user-editor")
                .status(User.UserStatus.ACTIVE)
                .build();
        editorUser = userRepository.save(editorUser);

        UserRole editorUserRole = UserRole.builder()
                .user(editorUser)
                .role(editorRole)
                .build();
        userRoleRepository.save(editorUserRole);

        // Request for both read (inherited) and write access
        AuthorizationRequest readRequest = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-editor")
                        .type("user")
                        .build())
                .action("read")  // Should be allowed via inheritance
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-1")
                        .build())
                .build();

        AuthorizationRequest writeRequest = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-editor")
                        .type("user")
                        .build())
                .action("write")  // Should be allowed via direct permission
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-1")
                        .build())
                .build();

        // When/Then: Should allow both read (inherited) and write access
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", "test-integration-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(readRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("ALLOW"));

        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", "test-integration-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(writeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("ALLOW"));
    }

    @Test
    @DisplayName("Should handle batch authorization requests")
    void shouldHandleBatchAuthorizationRequests() throws Exception {
        // Given: Multiple authorization requests
        AuthorizationRequest readRequest = AuthorizationRequest.builder()
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

        AuthorizationRequest writeRequest = AuthorizationRequest.builder()
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
                .build();

        BatchAuthorizationRequest batchRequest = BatchAuthorizationRequest.builder()
                .requests(List.of(readRequest, writeRequest))
                .build();

        // When/Then: Should return batch response with mixed results
        mockMvc.perform(post("/v1/authorize/batch")
                        .header("X-API-Key", "test-integration-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responses").isArray())
                .andExpect(jsonPath("$.responses", hasSize(2)))
                .andExpect(jsonPath("$.responses[0].decision").value("ALLOW"))  // read allowed
                .andExpect(jsonPath("$.responses[1].decision").value("DENY"));  // write denied
    }

    @Test
    @DisplayName("Should validate request input")
    void shouldValidateRequestInput() throws Exception {
        // Given: Invalid request (missing required fields)
        String invalidRequest = "{\"organizationId\": null}";

        // When/Then: Should return 400 Bad Request
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", "test-integration-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should require API key authentication")
    void shouldRequireApiKeyAuthentication() throws Exception {
        // Given: Valid request but no API key
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

        // When/Then: Should return 401 Unauthorized
        mockMvc.perform(post("/v1/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle expired role assignments")
    void shouldHandleExpiredRoleAssignments() throws Exception {
        // Given: User with expired role assignment
        User expiredUser = User.builder()
                .organization(testOrg)
                .email("expired@example.com")
                .username("expireduser")
                .externalId("user-expired")
                .status(User.UserStatus.ACTIVE)
                .build();
        expiredUser = userRepository.save(expiredUser);

        UserRole expiredUserRole = UserRole.builder()
                .user(expiredUser)
                .role(viewerRole)
                .expiresAt(OffsetDateTime.now().minusDays(1))  // Expired yesterday
                .build();
        userRoleRepository.save(expiredUserRole);

        AuthorizationRequest request = AuthorizationRequest.builder()
                .organizationId(testOrg.getId())
                .principal(AuthorizationRequest.Principal.builder()
                        .id("user-expired")
                        .type("user")
                        .build())
                .action("read")
                .resource(AuthorizationRequest.Resource.builder()
                        .type("document")
                        .id("doc-1")
                        .build())
                .build();

        // When/Then: Should deny access (expired role)
        mockMvc.perform(post("/v1/authorize")
                        .header("X-API-Key", "test-integration-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DENY"))
                .andExpect(jsonPath("$.reason", containsString("no roles")));
    }
}
