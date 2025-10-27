package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Organization.OrganizationStatus;
import io.authplatform.platform.domain.entity.Role;
import io.authplatform.platform.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RoleRepository}.
 *
 * <p>These tests verify repository operations using a test database.
 * Tests ensure proper CRUD operations, soft delete functionality, hierarchy support,
 * multi-tenancy, and custom queries.
 */
@DataJpaTest
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Role Repository Tests")
class RoleRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        // Create test organization for multi-tenancy tests
        testOrg = Organization.builder()
                .name("test-org-" + UUID.randomUUID().toString().substring(0, 8))
                .displayName("Test Organization")
                .description("Organization for role tests")
                .status(OrganizationStatus.ACTIVE)
                .settings(Map.of())
                .build();
        testOrg = organizationRepository.save(testOrg);
    }

    @Test
    @DisplayName("Should save and retrieve role")
    void shouldSaveAndRetrieveRole() {
        // Given
        Role role = Role.builder()
                .organization(testOrg)
                .name("admin")
                .displayName("Administrator")
                .description("Full system access")
                .level(0)
                .isSystem(false)
                .metadata(Map.of("icon", "shield"))
                .build();

        // When
        Role saved = roleRepository.save(role);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getName()).isEqualTo("admin");
        assertThat(saved.getDisplayName()).isEqualTo("Administrator");
        assertThat(saved.getLevel()).isEqualTo(0);
        assertThat(saved.getIsSystem()).isFalse();
        assertThat(saved.getOrganization().getId()).isEqualTo(testOrg.getId());
    }

    @Test
    @DisplayName("Should find role by name in organization")
    void shouldFindByName() {
        // Given
        Role role = createRole("developer", "Developer", 0, false);
        roleRepository.save(role);

        // When
        Optional<Role> found = roleRepository.findByOrganizationIdAndNameAndDeletedAtIsNull(
                testOrg.getId(), "developer");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("developer");
    }

    @Test
    @DisplayName("Should support role hierarchy with parent-child relationships")
    void shouldSupportHierarchy() {
        // Given - Create role hierarchy: admin → developer → junior-developer
        Role admin = createRole("admin", "Administrator", 0, false);
        admin = roleRepository.save(admin);

        Role developer = createRole("developer", "Developer", 1, false);
        developer.setParentRole(admin);
        developer = roleRepository.save(developer);

        Role juniorDev = createRole("junior-dev", "Junior Developer", 2, false);
        juniorDev.setParentRole(developer);
        juniorDev = roleRepository.save(juniorDev);

        // When
        List<Role> childrenOfAdmin = roleRepository.findByParentRoleIdAndDeletedAtIsNull(admin.getId());
        List<Role> childrenOfDev = roleRepository.findByParentRoleIdAndDeletedAtIsNull(developer.getId());

        // Then
        assertThat(childrenOfAdmin).hasSize(1);
        assertThat(childrenOfAdmin.get(0).getName()).isEqualTo("developer");

        assertThat(childrenOfDev).hasSize(1);
        assertThat(childrenOfDev.get(0).getName()).isEqualTo("junior-dev");
    }

    @Test
    @DisplayName("Should find all root roles (no parent)")
    void shouldFindRootRoles() {
        // Given
        Role root1 = createRole("admin", "Administrator", 0, false);
        Role root2 = createRole("viewer", "Viewer", 0, false);
        Role child = createRole("developer", "Developer", 1, false);

        root1 = roleRepository.save(root1);
        root2 = roleRepository.save(root2);

        child.setParentRole(root1);
        roleRepository.save(child);

        // When
        List<Role> rootRoles = roleRepository.findRootRolesByOrganizationId(testOrg.getId());

        // Then
        assertThat(rootRoles).hasSize(2);
        assertThat(rootRoles).extracting(Role::getName)
                .containsExactlyInAnyOrder("admin", "viewer");
    }

    @Test
    @DisplayName("Should find system roles")
    void shouldFindSystemRoles() {
        // Given
        Role systemRole = createRole("superadmin", "Super Administrator", 0, true);
        Role normalRole = createRole("user", "User", 0, false);
        roleRepository.saveAll(List.of(systemRole, normalRole));

        // When
        List<Role> systemRoles = roleRepository.findSystemRolesByOrganizationId(testOrg.getId());
        List<Role> nonSystemRoles = roleRepository.findNonSystemRolesByOrganizationId(testOrg.getId());

        // Then
        assertThat(systemRoles).hasSize(1);
        assertThat(systemRoles.get(0).getName()).isEqualTo("superadmin");

        assertThat(nonSystemRoles).hasSize(1);
        assertThat(nonSystemRoles.get(0).getName()).isEqualTo("user");
    }

    @Test
    @DisplayName("Should find roles by hierarchy level")
    void shouldFindByLevel() {
        // Given
        Role level0 = createRole("admin", "Administrator", 0, false);
        Role level1a = createRole("developer", "Developer", 1, false);
        Role level1b = createRole("analyst", "Analyst", 1, false);
        Role level2 = createRole("junior-dev", "Junior Developer", 2, false);

        level0 = roleRepository.save(level0);
        level1a.setParentRole(level0);
        level1b.setParentRole(level0);
        level1a = roleRepository.save(level1a);
        level1b = roleRepository.save(level1b);
        level2.setParentRole(level1a);
        roleRepository.save(level2);

        // When
        List<Role> level0Roles = roleRepository.findByOrganizationIdAndLevel(testOrg.getId(), 0);
        List<Role> level1Roles = roleRepository.findByOrganizationIdAndLevel(testOrg.getId(), 1);
        List<Role> level2Roles = roleRepository.findByOrganizationIdAndLevel(testOrg.getId(), 2);

        // Then
        assertThat(level0Roles).hasSize(1);
        assertThat(level1Roles).hasSize(2);
        assertThat(level2Roles).hasSize(1);
    }

    @Test
    @DisplayName("Should handle soft delete")
    void shouldHandleSoftDelete() {
        // Given
        Role role = createRole("to-delete", "To Delete", 0, false);
        Role saved = roleRepository.save(role);

        // When
        saved.softDelete();
        roleRepository.save(saved);

        // Then
        Optional<Role> found = roleRepository.findByIdAndNotDeleted(saved.getId());
        assertThat(found).isEmpty();

        Optional<Role> foundWithDeleted = roleRepository.findById(saved.getId());
        assertThat(foundWithDeleted).isPresent();
        assertThat(foundWithDeleted.get().isDeleted()).isTrue();
        assertThat(foundWithDeleted.get().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should restore soft-deleted role")
    void shouldRestoreSoftDeletedRole() {
        // Given
        Role role = createRole("to-restore", "To Restore", 0, false);
        role.softDelete();
        Role saved = roleRepository.save(role);

        // When
        saved.restore();
        roleRepository.save(saved);

        // Then
        Optional<Role> found = roleRepository.findByIdAndNotDeleted(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().isDeleted()).isFalse();
        assertThat(found.get().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("Should check role name existence")
    void shouldCheckNameExistence() {
        // Given
        Role role = createRole("existing-role", "Existing Role", 0, false);
        roleRepository.save(role);

        // When/Then
        assertThat(roleRepository.existsByOrganizationIdAndNameAndDeletedAtIsNull(
                testOrg.getId(), "existing-role")).isTrue();
        assertThat(roleRepository.existsByOrganizationIdAndNameAndDeletedAtIsNull(
                testOrg.getId(), "non-existing-role")).isFalse();
    }

    @Test
    @DisplayName("Should count roles")
    void shouldCountRoles() {
        // Given
        long initialCount = roleRepository.countNotDeletedByOrganizationId(testOrg.getId());
        long initialSystemCount = roleRepository.countSystemRolesByOrganizationId(testOrg.getId());

        roleRepository.saveAll(List.of(
                createRole("role1", "Role 1", 0, false),
                createRole("role2", "Role 2", 0, false),
                createRole("system-role", "System Role", 0, true)
        ));

        // When
        long totalCount = roleRepository.countNotDeletedByOrganizationId(testOrg.getId());
        long systemCount = roleRepository.countSystemRolesByOrganizationId(testOrg.getId());

        // Then
        assertThat(totalCount - initialCount).isEqualTo(3);
        assertThat(systemCount - initialSystemCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find roles by name pattern")
    void shouldFindByNamePattern() {
        // Given
        roleRepository.saveAll(List.of(
                createRole("admin-read", "Admin Read", 0, false),
                createRole("admin-write", "Admin Write", 0, false),
                createRole("user-read", "User Read", 0, false)
        ));

        // When
        List<Role> adminRoles = roleRepository.findByNameContainingIgnoreCaseAndNotDeleted(
                testOrg.getId(), "%admin%");

        // Then
        assertThat(adminRoles).hasSize(2);
        assertThat(adminRoles).extracting(Role::getName)
                .containsExactlyInAnyOrder("admin-read", "admin-write");
    }

    @Test
    @DisplayName("Should verify isRootRole method")
    void shouldVerifyIsRootRole() {
        // Given
        Role root = createRole("root", "Root Role", 0, false);
        root = roleRepository.save(root);

        Role child = createRole("child", "Child Role", 1, false);
        child.setParentRole(root);
        child = roleRepository.save(child);

        // When/Then
        assertThat(root.isRootRole()).isTrue();
        assertThat(child.isRootRole()).isFalse();
    }

    @Test
    @DisplayName("Should handle JSONB metadata field")
    void shouldHandleJsonbMetadata() {
        // Given
        Map<String, Object> metadata = Map.of(
                "ui", Map.of(
                        "icon", "shield",
                        "color", "#3B82F6",
                        "badge", "ADMIN"
                ),
                "features", Map.of(
                        "canManageBilling", true,
                        "canAccessAuditLogs", true
                )
        );

        Role role = Role.builder()
                .organization(testOrg)
                .name("metadata-test")
                .displayName("Metadata Test Role")
                .level(0)
                .isSystem(false)
                .metadata(metadata)
                .build();

        // When
        Role saved = roleRepository.save(role);
        roleRepository.flush();

        // Then
        Optional<Role> found = roleRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getMetadata()).isNotNull();
        assertThat(found.get().getMetadata()).containsKeys("ui", "features");
    }

    @Test
    @DisplayName("Should enforce organization scoping")
    void shouldEnforceOrganizationScoping() {
        // Given
        Organization org2 = Organization.builder()
                .name("test-org2-" + UUID.randomUUID().toString().substring(0, 8))
                .displayName("Test Organization 2")
                .status(OrganizationStatus.ACTIVE)
                .build();
        org2 = organizationRepository.save(org2);

        Role roleOrg1 = createRole("admin", "Admin Org 1", 0, false);
        roleRepository.save(roleOrg1);

        Role roleOrg2 = Role.builder()
                .organization(org2)
                .name("admin")
                .displayName("Admin Org 2")
                .level(0)
                .isSystem(false)
                .build();
        roleRepository.save(roleOrg2);

        // When
        Optional<Role> foundInOrg1 = roleRepository.findByOrganizationIdAndNameAndDeletedAtIsNull(
                testOrg.getId(), "admin");
        List<Role> org1Roles = roleRepository.findAllNotDeletedByOrganizationId(testOrg.getId());

        // Then
        assertThat(foundInOrg1).isPresent();
        assertThat(foundInOrg1.get().getDisplayName()).isEqualTo("Admin Org 1");

        assertThat(org1Roles).hasSize(1);
        assertThat(org1Roles.get(0).getDisplayName()).isEqualTo("Admin Org 1");
    }

    @Test
    @DisplayName("Should get hierarchy path")
    void shouldGetHierarchyPath() {
        // Given
        Role admin = createRole("admin", "Administrator", 0, false);
        admin = roleRepository.save(admin);

        Role developer = createRole("developer", "Developer", 1, false);
        developer.setParentRole(admin);
        developer = roleRepository.save(developer);

        Role juniorDev = createRole("junior-dev", "Junior Developer", 2, false);
        juniorDev.setParentRole(developer);
        juniorDev = roleRepository.save(juniorDev);

        // When
        String path = juniorDev.getHierarchyPath();

        // Then
        assertThat(path).isEqualTo("admin/developer/junior-dev");
    }

    @Test
    @DisplayName("Should find all descendants recursively")
    void shouldFindAllDescendants() {
        // Given - Create hierarchy: admin → [developer, analyst] → [junior-dev, senior-analyst]
        Role admin = createRole("admin", "Administrator", 0, false);
        admin = roleRepository.save(admin);

        Role developer = createRole("developer", "Developer", 1, false);
        developer.setParentRole(admin);
        developer = roleRepository.save(developer);

        Role analyst = createRole("analyst", "Analyst", 1, false);
        analyst.setParentRole(admin);
        analyst = roleRepository.save(analyst);

        Role juniorDev = createRole("junior-dev", "Junior Developer", 2, false);
        juniorDev.setParentRole(developer);
        juniorDev = roleRepository.save(juniorDev);

        Role seniorAnalyst = createRole("senior-analyst", "Senior Analyst", 2, false);
        seniorAnalyst.setParentRole(analyst);
        seniorAnalyst = roleRepository.save(seniorAnalyst);

        // When
        List<Role> descendants = roleRepository.findAllDescendants(admin.getId());

        // Then
        assertThat(descendants).hasSize(4);
        assertThat(descendants).extracting(Role::getName)
                .containsExactlyInAnyOrder("developer", "analyst", "junior-dev", "senior-analyst");
    }

    @Test
    @DisplayName("Should find all ancestors recursively")
    void shouldFindAllAncestors() {
        // Given - Create hierarchy: admin → developer → junior-dev
        Role admin = createRole("admin", "Administrator", 0, false);
        admin = roleRepository.save(admin);

        Role developer = createRole("developer", "Developer", 1, false);
        developer.setParentRole(admin);
        developer = roleRepository.save(developer);

        Role juniorDev = createRole("junior-dev", "Junior Developer", 2, false);
        juniorDev.setParentRole(developer);
        juniorDev = roleRepository.save(juniorDev);

        // When
        List<Role> ancestors = roleRepository.findAllAncestors(juniorDev.getId());

        // Then
        assertThat(ancestors).hasSize(2);
        assertThat(ancestors).extracting(Role::getName)
                .containsExactlyInAnyOrder("admin", "developer");
    }

    /**
     * Helper method to create a test role.
     */
    private Role createRole(String name, String displayName, Integer level, Boolean isSystem) {
        return Role.builder()
                .organization(testOrg)
                .name(name)
                .displayName(displayName)
                .description("Test role: " + name)
                .level(level)
                .isSystem(isSystem)
                .metadata(Map.of())
                .build();
    }
}
