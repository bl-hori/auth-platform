package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Role;
import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.domain.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link UserRoleRepository}.
 *
 * <p>Tests user-role assignment operations including:
 * <ul>
 *   <li>Basic CRUD operations</li>
 *   <li>Resource scoping (global, type-scoped, instance-scoped)</li>
 *   <li>Expiration handling</li>
 *   <li>Grant tracking</li>
 *   <li>Organization scoping</li>
 *   <li>Helper methods</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("test")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class UserRoleRepositoryTest {

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization org1;
    private Organization org2;
    private User user1;
    private User user2;
    private User adminUser;
    private Role adminRole;
    private Role editorRole;
    private Role viewerRole;

    @BeforeEach
    void setUp() {
        // Create test organizations
        org1 = organizationRepository.save(Organization.builder()
                .name("test-org-1")
                .displayName("Test Org 1")
                .build());

        org2 = organizationRepository.save(Organization.builder()
                .name("test-org-2")
                .displayName("Test Org 2")
                .build());

        // Create test users
        user1 = userRepository.save(User.builder()
                .organization(org1)
                .email("user1@test.com")
                .username("user1")
                .displayName("User One")
                .build());

        user2 = userRepository.save(User.builder()
                .organization(org1)
                .email("user2@test.com")
                .username("user2")
                .displayName("User Two")
                .build());

        adminUser = userRepository.save(User.builder()
                .organization(org1)
                .email("admin@test.com")
                .username("admin")
                .displayName("Admin User")
                .build());

        // Create test roles
        adminRole = roleRepository.save(Role.builder()
                .organization(org1)
                .name("admin")
                .displayName("Administrator")
                .description("Full system access")
                .level(0)
                .isSystem(true)
                .build());

        editorRole = roleRepository.save(Role.builder()
                .organization(org1)
                .name("editor")
                .displayName("Editor")
                .description("Can edit content")
                .level(1)
                .build());

        viewerRole = roleRepository.save(Role.builder()
                .organization(org1)
                .name("viewer")
                .displayName("Viewer")
                .description("Can view content")
                .level(2)
                .build());
    }

    @Test
    void shouldSaveAndFindUserRole() {
        // Given
        UserRole userRole = UserRole.builder()
                .user(user1)
                .role(adminRole)
                .grantedBy(adminUser)
                .build();

        // When
        UserRole saved = userRoleRepository.save(userRole);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser()).isEqualTo(user1);
        assertThat(saved.getRole()).isEqualTo(adminRole);
        assertThat(saved.getGrantedBy()).isEqualTo(adminUser);
        assertThat(saved.getGrantedAt()).isNotNull();
    }

    @Test
    void shouldFindByUserId() {
        // Given
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(adminRole)
                .grantedBy(adminUser)
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user2)
                .role(viewerRole)
                .grantedBy(adminUser)
                .build());

        // When
        List<UserRole> user1Roles = userRoleRepository.findByUserId(user1.getId());

        // Then
        assertThat(user1Roles).hasSize(2);
        assertThat(user1Roles).extracting(ur -> ur.getRole().getName())
                .containsExactlyInAnyOrder("admin", "editor");
    }

    @Test
    void shouldFindByRoleId() {
        // Given
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user2)
                .role(editorRole)
                .grantedBy(adminUser)
                .build());

        // When
        List<UserRole> editorUsers = userRoleRepository.findByRoleId(editorRole.getId());

        // Then
        assertThat(editorUsers).hasSize(2);
        assertThat(editorUsers).extracting(ur -> ur.getUser().getUsername())
                .containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    void shouldFindByUserIdAndRoleId() {
        // Given
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(adminRole)
                .grantedBy(adminUser)
                .build());

        // When
        Optional<UserRole> found = userRoleRepository.findByUserIdAndRoleId(user1.getId(), adminRole.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUser()).isEqualTo(user1);
        assertThat(found.get().getRole()).isEqualTo(adminRole);
    }

    @Test
    void shouldFindGlobalRolesByUserId() {
        // Given
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(adminRole)
                .grantedBy(adminUser)
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .resourceType("document")
                .build());

        // When
        List<UserRole> globalRoles = userRoleRepository.findGlobalRolesByUserId(user1.getId());

        // Then
        assertThat(globalRoles).hasSize(1);
        assertThat(globalRoles.get(0).isGlobalScope()).isTrue();
        assertThat(globalRoles.get(0).getRole()).isEqualTo(adminRole);
    }

    @Test
    void shouldFindByUserIdAndResourceType() {
        // Given
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .resourceType("document")
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(viewerRole)
                .grantedBy(adminUser)
                .resourceType("document")
                .resourceId("doc-123")
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(viewerRole)
                .grantedBy(adminUser)
                .resourceType("project")
                .build());

        // When
        List<UserRole> documentRoles = userRoleRepository.findByUserIdAndResourceType(user1.getId(), "document");

        // Then
        assertThat(documentRoles).hasSize(2);
        assertThat(documentRoles).allMatch(ur -> "document".equals(ur.getResourceType()));
    }

    @Test
    void shouldFindByUserIdAndResourceTypeAndResourceId() {
        // Given
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .resourceType("document")
                .resourceId("doc-123")
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(viewerRole)
                .grantedBy(adminUser)
                .resourceType("document")
                .resourceId("doc-456")
                .build());

        // When
        List<UserRole> specificDocRoles = userRoleRepository.findByUserIdAndResourceTypeAndResourceId(
                user1.getId(), "document", "doc-123");

        // Then
        assertThat(specificDocRoles).hasSize(1);
        assertThat(specificDocRoles.get(0).getResourceId()).isEqualTo("doc-123");
        assertThat(specificDocRoles.get(0).isInstanceScoped()).isTrue();
    }

    @Test
    void shouldFindNonExpiredByUserId() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(adminRole)
                .grantedBy(adminUser)
                .build()); // No expiration
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .expiresAt(now.plusDays(7))
                .build()); // Expires in future
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(viewerRole)
                .grantedBy(adminUser)
                .expiresAt(now.minusDays(1))
                .build()); // Already expired

        // When
        List<UserRole> nonExpired = userRoleRepository.findNonExpiredByUserId(user1.getId(), now);

        // Then
        assertThat(nonExpired).hasSize(2);
        assertThat(nonExpired).extracting(ur -> ur.getRole().getName())
                .containsExactlyInAnyOrder("admin", "editor");
    }

    @Test
    void shouldFindExpiredByUserId() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(adminRole)
                .grantedBy(adminUser)
                .expiresAt(now.minusDays(7))
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .expiresAt(now.minusDays(1))
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(viewerRole)
                .grantedBy(adminUser)
                .expiresAt(now.plusDays(1))
                .build());

        // When
        List<UserRole> expired = userRoleRepository.findExpiredByUserId(user1.getId(), now);

        // Then
        assertThat(expired).hasSize(2);
        assertThat(expired).extracting(ur -> ur.getRole().getName())
                .containsExactlyInAnyOrder("admin", "editor");
    }

    @Test
    void shouldFindByGrantedById() {
        // Given
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user2)
                .role(viewerRole)
                .grantedBy(adminUser)
                .build());

        // When
        List<UserRole> grantedByAdmin = userRoleRepository.findByGrantedById(adminUser.getId());

        // Then
        assertThat(grantedByAdmin).hasSize(2);
        assertThat(grantedByAdmin).allMatch(ur -> ur.getGrantedBy().equals(adminUser));
    }

    @Test
    void shouldFindExpiringBefore() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime threshold = now.plusDays(7);

        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .expiresAt(now.plusDays(3))
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user2)
                .role(viewerRole)
                .grantedBy(adminUser)
                .expiresAt(now.plusDays(14))
                .build());

        // When
        List<UserRole> expiringSoon = userRoleRepository.findExpiringBefore(threshold);

        // Then
        assertThat(expiringSoon).hasSize(1);
        assertThat(expiringSoon.get(0).getUser()).isEqualTo(user1);
    }

    @Test
    void shouldCheckExistsByUserIdAndRoleId() {
        // Given
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(adminRole)
                .grantedBy(adminUser)
                .build());

        // When
        boolean exists = userRoleRepository.existsByUserIdAndRoleId(user1.getId(), adminRole.getId());
        boolean notExists = userRoleRepository.existsByUserIdAndRoleId(user2.getId(), adminRole.getId());

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldCheckExistsByUserIdAndRoleIdAndResourceTypeAndResourceId() {
        // Given
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .resourceType("document")
                .resourceId("doc-123")
                .build());

        // When
        boolean exists = userRoleRepository.existsByUserIdAndRoleIdAndResourceTypeAndResourceId(
                user1.getId(), editorRole.getId(), "document", "doc-123");
        boolean notExists = userRoleRepository.existsByUserIdAndRoleIdAndResourceTypeAndResourceId(
                user1.getId(), editorRole.getId(), "document", "doc-456");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldCountByUserId() {
        // Given
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(adminRole)
                .grantedBy(adminUser)
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .build());

        // When
        long count = userRoleRepository.countByUserId(user1.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldCountByRoleId() {
        // Given
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(viewerRole)
                .grantedBy(adminUser)
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user2)
                .role(viewerRole)
                .grantedBy(adminUser)
                .build());

        // When
        long count = userRoleRepository.countByRoleId(viewerRole.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldFindByOrganizationId() {
        // Given
        User user3 = userRepository.save(User.builder()
                .organization(org2)
                .email("user3@test.com")
                .username("user3")
                .displayName("User Three")
                .build());

        Role org2Role = roleRepository.save(Role.builder()
                .organization(org2)
                .name("org2-role")
                .displayName("Org2 Role")
                .build());

        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(adminRole)
                .grantedBy(adminUser)
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user3)
                .role(org2Role)
                .build());

        // When
        List<UserRole> org1UserRoles = userRoleRepository.findByOrganizationId(org1.getId());
        List<UserRole> org2UserRoles = userRoleRepository.findByOrganizationId(org2.getId());

        // Then
        assertThat(org1UserRoles).hasSize(1);
        assertThat(org2UserRoles).hasSize(1);
    }

    @Test
    void shouldFindDistinctResourceTypesByUserId() {
        // Given
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .resourceType("document")
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(viewerRole)
                .grantedBy(adminUser)
                .resourceType("document")
                .resourceId("doc-123")
                .build());
        userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(viewerRole)
                .grantedBy(adminUser)
                .resourceType("project")
                .build());

        // When
        List<String> resourceTypes = userRoleRepository.findDistinctResourceTypesByUserId(user1.getId());

        // Then
        assertThat(resourceTypes).containsExactly("document", "project");
    }

    @Test
    void shouldVerifyScopeHelperMethods() {
        // Given
        UserRole globalRole = userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(adminRole)
                .grantedBy(adminUser)
                .build());

        UserRole typeScopedRole = userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .resourceType("document")
                .build());

        UserRole instanceScopedRole = userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(viewerRole)
                .grantedBy(adminUser)
                .resourceType("document")
                .resourceId("doc-123")
                .build());

        // Then
        assertThat(globalRole.isGlobalScope()).isTrue();
        assertThat(globalRole.isTypeScopedOnly()).isFalse();
        assertThat(globalRole.isInstanceScoped()).isFalse();
        assertThat(globalRole.getScopeDescription()).isEqualTo("Global (all resources)");

        assertThat(typeScopedRole.isGlobalScope()).isFalse();
        assertThat(typeScopedRole.isTypeScopedOnly()).isTrue();
        assertThat(typeScopedRole.isInstanceScoped()).isFalse();
        assertThat(typeScopedRole.getScopeDescription()).isEqualTo("All document resources");

        assertThat(instanceScopedRole.isGlobalScope()).isFalse();
        assertThat(instanceScopedRole.isTypeScopedOnly()).isFalse();
        assertThat(instanceScopedRole.isInstanceScoped()).isTrue();
        assertThat(instanceScopedRole.getScopeDescription()).isEqualTo("document:doc-123");
    }

    @Test
    void shouldVerifyExpirationHelperMethod() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        UserRole expiredRole = userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(adminRole)
                .grantedBy(adminUser)
                .expiresAt(now.minusDays(1))
                .build());

        UserRole activeRole = userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(editorRole)
                .grantedBy(adminUser)
                .expiresAt(now.plusDays(7))
                .build());

        UserRole neverExpiresRole = userRoleRepository.save(UserRole.builder()
                .user(user1)
                .role(viewerRole)
                .grantedBy(adminUser)
                .build());

        // Then
        assertThat(expiredRole.isExpired()).isTrue();
        assertThat(activeRole.isExpired()).isFalse();
        assertThat(neverExpiresRole.isExpired()).isFalse();
    }
}
