package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Organization.OrganizationStatus;
import io.authplatform.platform.domain.entity.Permission;
import io.authplatform.platform.domain.entity.Permission.PermissionEffect;
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
 * Integration tests for {@link PermissionRepository}.
 *
 * <p>These tests verify repository operations using a test database.
 * Tests ensure proper CRUD operations, permission queries, multi-tenancy,
 * and effect-based filtering.
 */
@DataJpaTest
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Permission Repository Tests")
class PermissionRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        // Create test organization for multi-tenancy tests
        testOrg = Organization.builder()
                .name("test-org-" + UUID.randomUUID().toString().substring(0, 8))
                .displayName("Test Organization")
                .description("Organization for permission tests")
                .status(OrganizationStatus.ACTIVE)
                .settings(Map.of())
                .build();
        testOrg = organizationRepository.save(testOrg);
    }

    @Test
    @DisplayName("Should save and retrieve permission")
    void shouldSaveAndRetrievePermission() {
        // Given
        Permission permission = Permission.builder()
                .organization(testOrg)
                .name("document:read")
                .displayName("Read Documents")
                .description("Allow reading documents")
                .resourceType("document")
                .action("read")
                .effect(PermissionEffect.ALLOW)
                .conditions(Map.of("owner", "${user.id}"))
                .build();

        // When
        Permission saved = permissionRepository.save(permission);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getName()).isEqualTo("document:read");
        assertThat(saved.getResourceType()).isEqualTo("document");
        assertThat(saved.getAction()).isEqualTo("read");
        assertThat(saved.getEffect()).isEqualTo(PermissionEffect.ALLOW);
        assertThat(saved.getOrganization().getId()).isEqualTo(testOrg.getId());
    }

    @Test
    @DisplayName("Should find permission by name")
    void shouldFindByName() {
        // Given
        Permission permission = createPermission("document:read", "document", "read", PermissionEffect.ALLOW);
        permissionRepository.save(permission);

        // When
        Optional<Permission> found = permissionRepository.findByOrganizationIdAndName(
                testOrg.getId(), "document:read");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("document:read");
    }

    @Test
    @DisplayName("Should find permission by resource type and action")
    void shouldFindByResourceTypeAndAction() {
        // Given
        Permission permission = createPermission("document:write", "document", "write", PermissionEffect.ALLOW);
        permissionRepository.save(permission);

        // When
        Optional<Permission> found = permissionRepository.findByOrganizationIdAndResourceTypeAndAction(
                testOrg.getId(), "document", "write");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("document:write");
    }

    @Test
    @DisplayName("Should find all permissions by resource type")
    void shouldFindByResourceType() {
        // Given
        permissionRepository.saveAll(List.of(
                createPermission("document:read", "document", "read", PermissionEffect.ALLOW),
                createPermission("document:write", "document", "write", PermissionEffect.ALLOW),
                createPermission("project:read", "project", "read", PermissionEffect.ALLOW)
        ));

        // When
        List<Permission> docPermissions = permissionRepository.findByOrganizationIdAndResourceType(
                testOrg.getId(), "document");

        // Then
        assertThat(docPermissions).hasSize(2);
        assertThat(docPermissions).extracting(Permission::getAction)
                .containsExactlyInAnyOrder("read", "write");
    }

    @Test
    @DisplayName("Should find all permissions by action")
    void shouldFindByAction() {
        // Given
        permissionRepository.saveAll(List.of(
                createPermission("document:read", "document", "read", PermissionEffect.ALLOW),
                createPermission("project:read", "project", "read", PermissionEffect.ALLOW),
                createPermission("document:write", "document", "write", PermissionEffect.ALLOW)
        ));

        // When
        List<Permission> readPermissions = permissionRepository.findByOrganizationIdAndAction(
                testOrg.getId(), "read");

        // Then
        assertThat(readPermissions).hasSize(2);
        assertThat(readPermissions).extracting(Permission::getResourceType)
                .containsExactlyInAnyOrder("document", "project");
    }

    @Test
    @DisplayName("Should find permissions by effect")
    void shouldFindByEffect() {
        // Given
        permissionRepository.saveAll(List.of(
                createPermission("document:read", "document", "read", PermissionEffect.ALLOW),
                createPermission("document:write", "document", "write", PermissionEffect.ALLOW),
                createPermission("document:delete", "document", "delete", PermissionEffect.DENY)
        ));

        // When
        List<Permission> allowPermissions = permissionRepository.findAllowPermissionsByOrganizationId(testOrg.getId());
        List<Permission> denyPermissions = permissionRepository.findDenyPermissionsByOrganizationId(testOrg.getId());

        // Then
        assertThat(allowPermissions).hasSize(2);
        assertThat(denyPermissions).hasSize(1);
        assertThat(denyPermissions.get(0).getAction()).isEqualTo("delete");
    }

    @Test
    @DisplayName("Should check permission existence by name")
    void shouldCheckExistenceByName() {
        // Given
        Permission permission = createPermission("document:read", "document", "read", PermissionEffect.ALLOW);
        permissionRepository.save(permission);

        // When/Then
        assertThat(permissionRepository.existsByOrganizationIdAndName(
                testOrg.getId(), "document:read")).isTrue();
        assertThat(permissionRepository.existsByOrganizationIdAndName(
                testOrg.getId(), "document:write")).isFalse();
    }

    @Test
    @DisplayName("Should check permission existence by resource type and action")
    void shouldCheckExistenceByResourceTypeAndAction() {
        // Given
        Permission permission = createPermission("document:read", "document", "read", PermissionEffect.ALLOW);
        permissionRepository.save(permission);

        // When/Then
        assertThat(permissionRepository.existsByOrganizationIdAndResourceTypeAndAction(
                testOrg.getId(), "document", "read")).isTrue();
        assertThat(permissionRepository.existsByOrganizationIdAndResourceTypeAndAction(
                testOrg.getId(), "document", "write")).isFalse();
    }

    @Test
    @DisplayName("Should count permissions")
    void shouldCountPermissions() {
        // Given
        long initialCount = permissionRepository.countByOrganizationId(testOrg.getId());
        long initialAllowCount = permissionRepository.countAllowPermissionsByOrganizationId(testOrg.getId());
        long initialDenyCount = permissionRepository.countDenyPermissionsByOrganizationId(testOrg.getId());

        permissionRepository.saveAll(List.of(
                createPermission("document:read", "document", "read", PermissionEffect.ALLOW),
                createPermission("document:write", "document", "write", PermissionEffect.ALLOW),
                createPermission("document:delete", "document", "delete", PermissionEffect.DENY)
        ));

        // When
        long totalCount = permissionRepository.countByOrganizationId(testOrg.getId());
        long allowCount = permissionRepository.countAllowPermissionsByOrganizationId(testOrg.getId());
        long denyCount = permissionRepository.countDenyPermissionsByOrganizationId(testOrg.getId());

        // Then
        assertThat(totalCount - initialCount).isEqualTo(3);
        assertThat(allowCount - initialAllowCount).isEqualTo(2);
        assertThat(denyCount - initialDenyCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find permissions by name pattern")
    void shouldFindByNamePattern() {
        // Given
        permissionRepository.saveAll(List.of(
                createPermission("document:read", "document", "read", PermissionEffect.ALLOW),
                createPermission("document:write", "document", "write", PermissionEffect.ALLOW),
                createPermission("project:read", "project", "read", PermissionEffect.ALLOW)
        ));

        // When
        List<Permission> docPermissions = permissionRepository.findByNameContainingIgnoreCase(
                testOrg.getId(), "%document%");

        // Then
        assertThat(docPermissions).hasSize(2);
        assertThat(docPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("document:read", "document:write");
    }

    @Test
    @DisplayName("Should find distinct resource types")
    void shouldFindDistinctResourceTypes() {
        // Given
        permissionRepository.saveAll(List.of(
                createPermission("document:read", "document", "read", PermissionEffect.ALLOW),
                createPermission("document:write", "document", "write", PermissionEffect.ALLOW),
                createPermission("project:read", "project", "read", PermissionEffect.ALLOW),
                createPermission("api:invoke", "api", "invoke", PermissionEffect.ALLOW)
        ));

        // When
        List<String> resourceTypes = permissionRepository.findDistinctResourceTypesByOrganizationId(testOrg.getId());

        // Then
        assertThat(resourceTypes).hasSize(3);
        assertThat(resourceTypes).containsExactlyInAnyOrder("api", "document", "project");
    }

    @Test
    @DisplayName("Should find distinct actions")
    void shouldFindDistinctActions() {
        // Given
        permissionRepository.saveAll(List.of(
                createPermission("document:read", "document", "read", PermissionEffect.ALLOW),
                createPermission("project:read", "project", "read", PermissionEffect.ALLOW),
                createPermission("document:write", "document", "write", PermissionEffect.ALLOW),
                createPermission("document:delete", "document", "delete", PermissionEffect.DENY)
        ));

        // When
        List<String> actions = permissionRepository.findDistinctActionsByOrganizationId(testOrg.getId());

        // Then
        assertThat(actions).hasSize(3);
        assertThat(actions).containsExactlyInAnyOrder("delete", "read", "write");
    }

    @Test
    @DisplayName("Should verify isAllow and isDeny methods")
    void shouldVerifyEffectMethods() {
        // Given
        Permission allowPerm = createPermission("document:read", "document", "read", PermissionEffect.ALLOW);
        Permission denyPerm = createPermission("document:delete", "document", "delete", PermissionEffect.DENY);

        // When/Then
        assertThat(allowPerm.isAllow()).isTrue();
        assertThat(allowPerm.isDeny()).isFalse();

        assertThat(denyPerm.isAllow()).isFalse();
        assertThat(denyPerm.isDeny()).isTrue();
    }

    @Test
    @DisplayName("Should verify hasConditions method")
    void shouldVerifyHasConditions() {
        // Given
        Permission withConditions = createPermission("document:read", "document", "read", PermissionEffect.ALLOW);
        withConditions.setConditions(Map.of("owner", "${user.id}"));

        Permission withoutConditions = createPermission("document:write", "document", "write", PermissionEffect.ALLOW);
        withoutConditions.setConditions(Map.of());

        // When/Then
        assertThat(withConditions.hasConditions()).isTrue();
        assertThat(withoutConditions.hasConditions()).isFalse();
    }

    @Test
    @DisplayName("Should get full identifier")
    void shouldGetFullIdentifier() {
        // Given
        Permission permission = createPermission("document:read", "document", "read", PermissionEffect.ALLOW);

        // When
        String identifier = permission.getFullIdentifier();

        // Then
        assertThat(identifier).isEqualTo("document:read");
    }

    @Test
    @DisplayName("Should handle JSONB conditions field")
    void shouldHandleJsonbConditions() {
        // Given
        Map<String, Object> conditions = Map.of(
                "resource", Map.of(
                        "owner", "${user.id}",
                        "status", "active"
                ),
                "user", Map.of(
                        "department", "Engineering",
                        "clearanceLevel", "confidential"
                )
        );

        Permission permission = Permission.builder()
                .organization(testOrg)
                .name("document:read")
                .displayName("Read Documents")
                .resourceType("document")
                .action("read")
                .effect(PermissionEffect.ALLOW)
                .conditions(conditions)
                .build();

        // When
        Permission saved = permissionRepository.save(permission);
        permissionRepository.flush();

        // Then
        Optional<Permission> found = permissionRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getConditions()).isNotNull();
        assertThat(found.get().getConditions()).containsKeys("resource", "user");
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

        Permission permOrg1 = createPermission("document:read", "document", "read", PermissionEffect.ALLOW);
        permissionRepository.save(permOrg1);

        Permission permOrg2 = Permission.builder()
                .organization(org2)
                .name("document:read")
                .displayName("Read Documents Org 2")
                .resourceType("document")
                .action("read")
                .effect(PermissionEffect.ALLOW)
                .build();
        permissionRepository.save(permOrg2);

        // When
        Optional<Permission> foundInOrg1 = permissionRepository.findByOrganizationIdAndName(
                testOrg.getId(), "document:read");
        List<Permission> org1Permissions = permissionRepository.findAllByOrganizationId(testOrg.getId());

        // Then
        assertThat(foundInOrg1).isPresent();
        assertThat(foundInOrg1.get().getDisplayName()).isEqualTo("DOCUMENT READ");

        assertThat(org1Permissions).hasSize(1);
        assertThat(org1Permissions.get(0).getDisplayName()).isEqualTo("DOCUMENT READ");
    }

    /**
     * Helper method to create a test permission.
     */
    private Permission createPermission(String name, String resourceType, String action, PermissionEffect effect) {
        return Permission.builder()
                .organization(testOrg)
                .name(name)
                .displayName(name.replace(":", " ").toUpperCase())
                .description("Test permission: " + name)
                .resourceType(resourceType)
                .action(action)
                .effect(effect)
                .conditions(Map.of())
                .build();
    }
}
