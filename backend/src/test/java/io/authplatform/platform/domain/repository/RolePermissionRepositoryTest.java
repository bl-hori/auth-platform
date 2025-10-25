package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Permission;
import io.authplatform.platform.domain.entity.Permission.PermissionEffect;
import io.authplatform.platform.domain.entity.Role;
import io.authplatform.platform.domain.entity.RolePermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RolePermissionRepository}.
 *
 * <p>Tests role-permission assignment operations including:
 * <ul>
 *   <li>Basic CRUD operations</li>
 *   <li>Finding permissions by role</li>
 *   <li>Finding roles by permission</li>
 *   <li>Organization scoping</li>
 *   <li>Resource type and action filtering</li>
 *   <li>Permission effect filtering</li>
 *   <li>System role permissions</li>
 *   <li>Helper methods</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("test")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class RolePermissionRepositoryTest {

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization org1;
    private Organization org2;
    private Role adminRole;
    private Role editorRole;
    private Role viewerRole;
    private Role systemRole;
    private Permission documentRead;
    private Permission documentWrite;
    private Permission documentDelete;
    private Permission projectRead;
    private Permission apiInvoke;

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

        // Create test roles
        adminRole = roleRepository.save(Role.builder()
                .organization(org1)
                .name("admin")
                .displayName("Administrator")
                .level(0)
                .isSystem(true)
                .build());

        editorRole = roleRepository.save(Role.builder()
                .organization(org1)
                .name("editor")
                .displayName("Editor")
                .level(1)
                .build());

        viewerRole = roleRepository.save(Role.builder()
                .organization(org1)
                .name("viewer")
                .displayName("Viewer")
                .level(2)
                .build());

        systemRole = roleRepository.save(Role.builder()
                .organization(org1)
                .name("system")
                .displayName("System Role")
                .level(0)
                .isSystem(true)
                .build());

        // Create test permissions
        documentRead = permissionRepository.save(Permission.builder()
                .organization(org1)
                .name("document:read")
                .displayName("DOCUMENT READ")
                .resourceType("document")
                .action("read")
                .effect(PermissionEffect.ALLOW)
                .build());

        documentWrite = permissionRepository.save(Permission.builder()
                .organization(org1)
                .name("document:write")
                .displayName("DOCUMENT WRITE")
                .resourceType("document")
                .action("write")
                .effect(PermissionEffect.ALLOW)
                .build());

        documentDelete = permissionRepository.save(Permission.builder()
                .organization(org1)
                .name("document:delete")
                .displayName("DOCUMENT DELETE")
                .resourceType("document")
                .action("delete")
                .effect(PermissionEffect.DENY)
                .build());

        projectRead = permissionRepository.save(Permission.builder()
                .organization(org1)
                .name("project:read")
                .displayName("PROJECT READ")
                .resourceType("project")
                .action("read")
                .effect(PermissionEffect.ALLOW)
                .build());

        apiInvoke = permissionRepository.save(Permission.builder()
                .organization(org1)
                .name("api:invoke")
                .displayName("API INVOKE")
                .resourceType("api")
                .action("invoke")
                .effect(PermissionEffect.ALLOW)
                .build());
    }

    @Test
    void shouldSaveAndFindRolePermission() {
        // Given
        RolePermission rolePermission = RolePermission.builder()
                .role(adminRole)
                .permission(documentRead)
                .build();

        // When
        RolePermission saved = rolePermissionRepository.save(rolePermission);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(adminRole);
        assertThat(saved.getPermission()).isEqualTo(documentRead);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindByRoleId() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentWrite)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(viewerRole)
                .permission(documentRead)
                .build());

        // When
        List<RolePermission> editorPermissions = rolePermissionRepository.findByRoleId(editorRole.getId());

        // Then
        assertThat(editorPermissions).hasSize(2);
        assertThat(editorPermissions).extracting(rp -> rp.getPermission().getName())
                .containsExactlyInAnyOrder("document:read", "document:write");
    }

    @Test
    void shouldFindByPermissionId() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(viewerRole)
                .permission(documentRead)
                .build());

        // When
        List<RolePermission> rolesWithRead = rolePermissionRepository.findByPermissionId(documentRead.getId());

        // Then
        assertThat(rolesWithRead).hasSize(3);
        assertThat(rolesWithRead).extracting(rp -> rp.getRole().getName())
                .containsExactlyInAnyOrder("admin", "editor", "viewer");
    }

    @Test
    void shouldFindByRoleIdAndPermissionId() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(documentRead)
                .build());

        // When
        Optional<RolePermission> found = rolePermissionRepository.findByRoleIdAndPermissionId(
                adminRole.getId(), documentRead.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(adminRole);
        assertThat(found.get().getPermission()).isEqualTo(documentRead);
    }

    @Test
    void shouldFindByOrganizationId() {
        // Given
        Role org2Role = roleRepository.save(Role.builder()
                .organization(org2)
                .name("org2-role")
                .displayName("Org2 Role")
                .build());

        Permission org2Permission = permissionRepository.save(Permission.builder()
                .organization(org2)
                .name("org2:permission")
                .displayName("ORG2 PERMISSION")
                .resourceType("resource")
                .action("action")
                .build());

        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(org2Role)
                .permission(org2Permission)
                .build());

        // When
        List<RolePermission> org1RolePermissions = rolePermissionRepository.findByOrganizationId(org1.getId());
        List<RolePermission> org2RolePermissions = rolePermissionRepository.findByOrganizationId(org2.getId());

        // Then
        assertThat(org1RolePermissions).hasSize(1);
        assertThat(org2RolePermissions).hasSize(1);
    }

    @Test
    void shouldFindByOrganizationIdAndResourceType() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentWrite)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(projectRead)
                .build());

        // When
        List<RolePermission> documentPermissions = rolePermissionRepository.findByOrganizationIdAndResourceType(
                org1.getId(), "document");

        // Then
        assertThat(documentPermissions).hasSize(2);
        assertThat(documentPermissions).allMatch(rp -> "document".equals(rp.getPermission().getResourceType()));
    }

    @Test
    void shouldFindByOrganizationIdAndAction() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(viewerRole)
                .permission(projectRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentWrite)
                .build());

        // When
        List<RolePermission> readPermissions = rolePermissionRepository.findByOrganizationIdAndAction(
                org1.getId(), "read");

        // Then
        assertThat(readPermissions).hasSize(2);
        assertThat(readPermissions).allMatch(rp -> "read".equals(rp.getPermission().getAction()));
    }

    @Test
    void shouldFindSystemRolePermissions() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(systemRole)
                .permission(apiInvoke)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentWrite)
                .build());

        // When
        List<RolePermission> systemPermissions = rolePermissionRepository.findSystemRolePermissions(org1.getId());

        // Then
        assertThat(systemPermissions).hasSize(2);
        assertThat(systemPermissions).extracting(rp -> rp.getRole().getName())
                .containsExactlyInAnyOrder("admin", "system");
    }

    @Test
    void shouldFindAllowPermissionsByRoleId() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentWrite)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentDelete)
                .build());

        // When
        List<RolePermission> allowPermissions = rolePermissionRepository.findAllowPermissionsByRoleId(editorRole.getId());

        // Then
        assertThat(allowPermissions).hasSize(2);
        assertThat(allowPermissions).allMatch(rp -> rp.getPermission().isAllow());
    }

    @Test
    void shouldFindDenyPermissionsByRoleId() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentDelete)
                .build());

        // When
        List<RolePermission> denyPermissions = rolePermissionRepository.findDenyPermissionsByRoleId(editorRole.getId());

        // Then
        assertThat(denyPermissions).hasSize(1);
        assertThat(denyPermissions.get(0).getPermission()).isEqualTo(documentDelete);
        assertThat(denyPermissions.get(0).getPermission().isDeny()).isTrue();
    }

    @Test
    void shouldFindByOrganizationIdAndRoleLevel() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole) // level 0
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole) // level 1
                .permission(documentWrite)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(viewerRole) // level 2
                .permission(projectRead)
                .build());

        // When
        List<RolePermission> level0Permissions = rolePermissionRepository.findByOrganizationIdAndRoleLevel(
                org1.getId(), 0);

        // Then
        assertThat(level0Permissions).hasSize(1);
        assertThat(level0Permissions.get(0).getRole().getLevel()).isEqualTo(0);
    }

    @Test
    void shouldCheckExistsByRoleIdAndPermissionId() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(documentRead)
                .build());

        // When
        boolean exists = rolePermissionRepository.existsByRoleIdAndPermissionId(
                adminRole.getId(), documentRead.getId());
        boolean notExists = rolePermissionRepository.existsByRoleIdAndPermissionId(
                viewerRole.getId(), documentRead.getId());

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldCountByRoleId() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentWrite)
                .build());

        // When
        long count = rolePermissionRepository.countByRoleId(editorRole.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldCountByPermissionId() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(viewerRole)
                .permission(documentRead)
                .build());

        // When
        long count = rolePermissionRepository.countByPermissionId(documentRead.getId());

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void shouldFindDistinctResourceTypesByRoleId() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(documentWrite)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(projectRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(apiInvoke)
                .build());

        // When
        List<String> resourceTypes = rolePermissionRepository.findDistinctResourceTypesByRoleId(adminRole.getId());

        // Then
        assertThat(resourceTypes).containsExactly("api", "document", "project");
    }

    @Test
    void shouldFindDistinctActionsByRoleId() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentWrite)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(projectRead)
                .build());

        // When
        List<String> actions = rolePermissionRepository.findDistinctActionsByRoleId(editorRole.getId());

        // Then
        assertThat(actions).containsExactly("read", "write");
    }

    @Test
    void shouldFindByRoleIdAndPermissionNamePattern() {
        // Given
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(documentRead)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(documentWrite)
                .build());
        rolePermissionRepository.save(RolePermission.builder()
                .role(adminRole)
                .permission(projectRead)
                .build());

        // When
        List<RolePermission> documentPermissions = rolePermissionRepository.findByRoleIdAndPermissionNamePattern(
                adminRole.getId(), "document%");

        // Then
        assertThat(documentPermissions).hasSize(2);
        assertThat(documentPermissions).allMatch(rp -> rp.getPermission().getName().startsWith("document"));
    }

    @Test
    void shouldVerifyHelperMethods() {
        // Given
        RolePermission rolePermission = rolePermissionRepository.save(RolePermission.builder()
                .role(editorRole)
                .permission(documentRead)
                .build());

        // Then
        assertThat(rolePermission.getPermissionIdentifier()).isEqualTo("document:read");
        assertThat(rolePermission.getRoleName()).isEqualTo("editor");
        assertThat(rolePermission.hasRole("editor")).isTrue();
        assertThat(rolePermission.hasRole("admin")).isFalse();
        assertThat(rolePermission.hasPermission("document:read")).isTrue();
        assertThat(rolePermission.hasPermission("document:write")).isFalse();
        assertThat(rolePermission.getAssignmentDescription())
                .contains("editor")
                .contains("document:read")
                .contains("document:read");
    }
}
